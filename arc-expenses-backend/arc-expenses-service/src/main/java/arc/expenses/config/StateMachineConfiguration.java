package arc.expenses.config;

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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
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
import java.io.IOException;
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
                .initial(Stages.Stage2)
                .end(Stages.Stage13)
                .states(EnumSet.allOf(Stages.class));
    }

    @Override
    public void configure(StateMachineConfigurationConfigurer<Stages, StageEvents> config)
            throws Exception {
//        config
//                .withConfiguration()
//                .autoStartup(true)
//                .and()
//                .withSecurity()
//                .enabled(true)
//                .event("hasRole('USER')");

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
            }

            @Override
            public void eventNotAccepted(Message<StageEvents> event) {
                logger.error("Event not accepted: {}", event.getPayload());
            }
        };
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<Stages, StageEvents> transitions) throws Exception {

        List<Class> stagesClasses = Arrays.stream(Request.class.getDeclaredFields()).filter(p-> Stage.class.isAssignableFrom(p.getType())).flatMap(p -> Stream.of(p.getType())).collect(Collectors.toList());

        transitions = transitions.withExternal()
                .source(Stages.valueOf(stagesClasses.get(0).getSimpleName()))
                .target(Stages.valueOf(stagesClasses.get(1).getSimpleName()))
                .event(StageEvents.APPROVE)
                .guard(stateContext -> checkContains(stateContext.getMessage(), stagesClasses.get(1)))
                .and()
                .withExternal()
                .source(Stages.valueOf(stagesClasses.get(0).getSimpleName()))
                .target(Stages.CANCELLED)
                .event(StageEvents.CANCEL)
                .and()
                .withExternal()
                .source(Stages.valueOf(stagesClasses.get(0).getSimpleName()))
                .target(Stages.REJECTED)
                .event(StageEvents.REJECT)
                .and();

        for(int i=1;i<stagesClasses.size()-1;i++){
            int finalI = i;
            transitions = transitions
            .withExternal()
                    .source(Stages.valueOf(stagesClasses.get(i).getSimpleName()))
                    .target(Stages.valueOf(stagesClasses.get(i+1).getSimpleName()))
                    .event(StageEvents.APPROVE)
                    .guard(stateContext -> checkContains(stateContext.getMessage(), stagesClasses.get(finalI)))
                    .action(approve2())
                    .and()
            .withExternal()
                    .source(Stages.valueOf(stagesClasses.get(i).getSimpleName()))
                    .target(Stages.CANCELLED)
                    .event(StageEvents.CANCEL)
                    .and()
            .withExternal()
                    .source(Stages.valueOf(stagesClasses.get(i).getSimpleName()))
                    .target(Stages.REJECTED)
                    .event(StageEvents.REJECT)
                    .and();
            if(!stagesClasses.get(i).equals(Stage7.class)){
                transitions =transitions.withExternal()
                        .source(Stages.valueOf(stagesClasses.get(i+1).getSimpleName()))
                        .target(Stages.valueOf(stagesClasses.get(i).getSimpleName()))
                        .event(StageEvents.DOWNGRADE)
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

        if(request == null)
            return false;

        HttpServletRequest req = message.getHeaders().get("restRequest", HttpServletRequest.class);
        if(req == null)
            return false;

        List<String> requiredFields = Arrays.stream(stageClass.getDeclaredFields()).filter(p -> p.isAnnotationPresent(NotNull.class)).flatMap(p -> Stream.of(p.getName())).collect(Collectors.toList());
        Map<String, String[]> parameters = req.getParameterMap();
        for(String field: requiredFields){
            if(!parameters.containsKey(field))
                return false;
        }

        return true;
    }

    @Bean
    public Action<Stages, StageEvents> approve2(){
        return context -> {
            HttpServletRequest req = context.getMessage().getHeaders().get("restRequest", HttpServletRequest.class);
            MultipartHttpServletRequest multiPartRequest = context.getMessage().getHeaders().get("restRequest", MultipartHttpServletRequest.class);

            Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
            String comment = Optional.ofNullable(req.getHeader("comment")).orElse("");

            ArrayList<Attachment> attachments = new ArrayList<>();
            for(MultipartFile file : multiPartRequest.getFiles("files")){
                try {
                    storeRESTClient.storeFile(file.getBytes(), request.getArchiveId(), file.getOriginalFilename());
                    attachments.add(new Attachment(file.getOriginalFilename(), FileUtils.extension(file.getOriginalFilename()),new Long(file.getSize()+""), request.getArchiveId()+"/stage1"));
                } catch (IOException e) {
                    logger.warn(e);
                }
            }

            try {
                RequestApproval requestApproval = requestApprovalService.getByField("request_id",request.getId());
                Stage2 stage2 = new Stage2(true,true,true);
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
                requestApproval.setStage("3");
                requestApproval.setStatus(BaseInfo.Status.PENDING);
                requestApprovalService.update(requestApproval,requestApproval.getId());
                aclService.updateAclEntries(
                        Collections.singletonList(new PrincipalSid(request.getProject().getScientificCoordinator().getEmail())),
                        request.getProject().getOperator().stream().flatMap(entry -> Stream.of(new PrincipalSid(entry.getEmail()))).collect(Collectors.toList()),
                        request.getId());

            } catch (Exception e) {
                logger.error("Exception occured moving to next state for request " + request.getId(),e);
            }
        };
    }

}
