package arc.expenses.config.stateMachine;

import arc.expenses.domain.StageEvents;
import arc.expenses.domain.Stages;
import arc.expenses.service.AclService;
import arc.expenses.service.RequestApprovalServiceImpl;
import arc.expenses.service.UserServiceImpl;
import eu.openminted.registry.core.service.ServiceException;
import eu.openminted.store.restclient.StoreRESTClient;
import gr.athenarc.domain.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListener;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableStateMachineFactory
public class StateMachineConfiguration extends EnumStateMachineConfigurerAdapter<Stages, StageEvents> {

    private static Logger logger = LogManager.getLogger(StateMachineConfiguration.class);

    @Autowired
    private RequestApprovalServiceImpl requestApprovalService;

    @Autowired
    AclService aclService;

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private StoreRESTClient storeRESTClient;


    @Override
    public void configure(StateMachineStateConfigurer<Stages, StageEvents> states) throws Exception {
        states.withStates()
                .initial(Stages.Stage1)
                .end(Stages.Stage13)
                .states(EnumSet.allOf(Stages.class));
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<Stages, StageEvents> config)
            throws Exception {
        config
                .withConfiguration()
                .autoStartup(true)
                .listener(loggingListener());
    }

    public StateMachineListener<Stages, StageEvents> loggingListener() {
        return new StateMachineListenerAdapter<Stages, StageEvents>() {
            @Override
            public void stateChanged(State<Stages, StageEvents> from, State<Stages, StageEvents> to) {
                logger.info("State changed to {}", to.getId());
            }

            @Override
            public void stateMachineError(StateMachine<Stages, StageEvents> stateMachine, Exception exception) {
                logger.info(exception.getMessage());
                stateMachine.getExtendedState().getVariables().put("error",exception.getMessage());
            }

            @Override
            public void eventNotAccepted(Message<StageEvents> event) {
                logger.error("Event not accepted: {}", event.getPayload());
            }
        };
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<Stages, StageEvents> transitions) throws Exception {

        List<Class> stagesClasses = Arrays.stream(RequestApproval.class.getDeclaredFields()).filter(p-> Stage.class.isAssignableFrom(p.getType())).flatMap(p -> Stream.of(p.getType())).collect(Collectors.toList());

        transitions = transitions.withExternal()
                .source(Stages.Stage1)
                .target(Stages.Stage2)
                .event(StageEvents.APPROVE)
                .guard(stateContext -> checkContains(stateContext.getMessage(), Stage2.class))
                .and()
                .withExternal()
                .source(Stages.Stage1)
                .target(Stages.CANCELLED)
                .event(StageEvents.CANCEL)
                .and()
                .withExternal()
                .source(Stages.Stage2)
                .target(Stages.Stage1)
                .event(StageEvents.DOWNGRADE)
                .action(context -> {
                    Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
                    MultipartHttpServletRequest req = context.getMessage().getHeaders().get("restRequest", MultipartHttpServletRequest.class);
                    String comment = Optional.ofNullable(req.getParameter("comment")).orElse("");
                    if(comment.isEmpty()) {
                        context.getStateMachine().setStateMachineError(new ServiceException("We need a comment!"));
                        throw new ServiceException("We need a comment!");
                    }
                    try {
                        modifyRequest(context, true, true, false,"1", BaseInfo.Status.UNDER_REVIEW);
                        aclService.updateAclEntries(
                                Collections.singletonList(new PrincipalSid(request.getProject().getScientificCoordinator().getEmail())),
                                Collections.singletonList(new PrincipalSid(request.getUser().getEmail())),
                                request.getId());
                    } catch (Exception e) {
                        logger.error("Error occurred on rejection of request " + request.getId());
                    }
                })
                .and();

        for(int i=1;i<stagesClasses.size()-1;i++){
            int finalI = i;
            transitions = transitions
            .withExternal()
                    .source(Stages.valueOf(stagesClasses.get(i).getSimpleName()))
                    .target(Stages.valueOf(stagesClasses.get(i+1).getSimpleName()))
                    .event(StageEvents.APPROVE)
                    .guard(stateContext -> checkContains(stateContext.getMessage(), stagesClasses.get(finalI)))
                    .action(context -> {
                        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
                        try {
                            modifyRequest(context, true, true, true, "3", BaseInfo.Status.PENDING);
                            aclService.updateAclEntries(
                                    Collections.singletonList(new PrincipalSid(request.getProject().getScientificCoordinator().getEmail())),
                                    request.getProject().getOperator().stream().flatMap(entry -> Stream.of(new PrincipalSid(entry.getEmail()))).collect(Collectors.toList()),
                                    request.getId());

                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request " + request.getId());
                        }
                    })
                    .and()
            .withExternal()
                    .source(Stages.valueOf(stagesClasses.get(i).getSimpleName()))
                    .target(Stages.CANCELLED)
                    .event(StageEvents.CANCEL)
                    .action(context -> {
                        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
                        try {
                            modifyRequest(context, true, true, true, "2", BaseInfo.Status.CANCELLED);
                            aclService.deleteAcl(new ObjectIdentityImpl(Request.class,request.getId()), true);
                        } catch (Exception e) {
                            logger.error("Error occurred on approval of request " + request.getId());
                        }
                    })
                    .and()
            .withExternal()
                    .source(Stages.valueOf(stagesClasses.get(i).getSimpleName()))
                    .target(Stages.REJECTED)
                    .event(StageEvents.REJECT)
                    .action(context -> {
                        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
                        try {
                            modifyRequest(context, true, true, true,"2", BaseInfo.Status.REJECTED);
                            aclService.deleteAcl(new ObjectIdentityImpl(Request.class,request.getId()), true);
                        } catch (Exception e) {
                            logger.error("Error occurred on rejection of request " + request.getId());
                        }
                    })
                    .and();
            if(!stagesClasses.get(i).equals(Stage7.class)){
                transitions =transitions.withExternal()
                        .source(Stages.valueOf(stagesClasses.get(i+1).getSimpleName()))
                        .target(Stages.valueOf(stagesClasses.get(i).getSimpleName()))
                        .event(StageEvents.DOWNGRADE)
                        .action(context -> {
                            Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
                            String comment = Optional.ofNullable(context.getMessage().getHeaders().get("restRequest", HttpServletRequest.class).getHeader("comment")).orElse("");
                            if(comment.isEmpty())
                                throw new ServiceException("We need a comment!");
                            try {
                                modifyRequest(context, true, true, false,"2", BaseInfo.Status.UNDER_REVIEW);
                                aclService.updateAclEntries(
                                        Collections.singletonList(new PrincipalSid(request.getProject().getScientificCoordinator().getEmail())),
                                        Collections.singletonList(new PrincipalSid(request.getUser().getEmail())),
                                        request.getId());
                            } catch (Exception e) {
                                logger.error("Error occurred on rejection of request " + request.getId());
                            }
                        })
                        .and();
            }
        }

        transitions.withExternal()
                .source(Stages.valueOf(stagesClasses.get(stagesClasses.size()-1).getSimpleName()))
                .target(Stages.valueOf(stagesClasses.get(stagesClasses.size()-2).getSimpleName()))
                .event(StageEvents.DOWNGRADE)
                .and()
                .withExternal()
                .source(Stages.valueOf(stagesClasses.get(stagesClasses.size()-1).getSimpleName()))
                .target(Stages.CANCELLED)
                .event(StageEvents.CANCEL)
                .and()
                .withExternal()
                .source(Stages.valueOf(stagesClasses.get(stagesClasses.size()-1).getSimpleName()))
                .target(Stages.REJECTED)
                .event(StageEvents.REJECT)
                .and();
    }


    private boolean checkContains(Message<StageEvents> message, Class stageClass){
        Request request = message.getHeaders().get("requestObj", Request.class);

        if(request == null) {
            return false;
        }

        HttpServletRequest req = message.getHeaders().get("restRequest", HttpServletRequest.class);
        if(req == null) {
            return false;
        }
        List<String> requiredFields = Arrays.stream(stageClass.getDeclaredFields()).filter(p -> p.isAnnotationPresent(NotNull.class)).flatMap(p -> Stream.of(p.getName())).collect(Collectors.toList());
        Map<String, String[]> parameters = req.getParameterMap();
        for(String field: requiredFields){
            if(!parameters.containsKey(field)) {
                return false;
            }
        }

        return true;
    }



    private void modifyRequest(
            StateContext<Stages, StageEvents> context,
            boolean checkFeasibility,
            boolean checkNecessity,
            boolean approved,
            String stage,
            BaseInfo.Status status) throws Exception {

        HttpServletRequest req = context.getMessage().getHeaders().get("restRequest", HttpServletRequest.class);
        MultipartHttpServletRequest multiPartRequest = context.getMessage().getHeaders().get("restRequest", MultipartHttpServletRequest.class);

        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
        String comment = Optional.ofNullable(req.getParameter("comment")).orElse("");

        ArrayList<Attachment> attachments = new ArrayList<>();
        for(MultipartFile file : multiPartRequest.getFiles("files")){
            storeRESTClient.storeFile(file.getBytes(), request.getArchiveId(), file.getOriginalFilename());
            attachments.add(new Attachment(file.getOriginalFilename(), FileUtils.extension(file.getOriginalFilename()),new Long(file.getSize()+""), request.getArchiveId()+"/stage1"));
        }

        RequestApproval requestApproval = requestApprovalService.getByField("request_id",request.getId());
        Stage2 stage2 = new Stage2(checkFeasibility,checkNecessity,approved);
        stage2.setAttachments(attachments);
        stage2.setComment(comment);
        try {
            User user = userService.getByField("user_email",(String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            stage2.setUser(user);
        } catch (Exception e) {
            throw new ServiceException("User not found");
        }
        stage2.setDate(LocalDate.now().toEpochDay()+"");
        requestApproval.setStage2(stage2);
        requestApproval.setStage(stage);
        requestApproval.setStatus(status);
        requestApprovalService.update(requestApproval,requestApproval.getId());
    }


}
