package arc.expenses.service;

import arc.expenses.domain.RequestResponse;
import arc.expenses.domain.StageEvents;
import arc.expenses.domain.Stages;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ServiceException;
import gr.athenarc.domain.*;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service("requestApproval")
public class RequestApprovalServiceImpl extends GenericService<RequestApproval> {

    private static Logger logger = LogManager.getLogger(RequestApprovalServiceImpl.class);

    @Autowired
    private RequestServiceImpl requestService;

    @Autowired
    private ProjectServiceImpl projectService;

    @Autowired
    private InstituteServiceImpl instituteService;

    @Autowired
    private TransitionService transitionService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    @Lazy
    private StateMachineFactory<Stages, StageEvents> factory;

    public RequestApprovalServiceImpl() {
        super(RequestApproval.class);
    }

    @Override
    public String getResourceType() {
        return "approval";
    }

    public RequestApproval getApproval(String id) throws Exception {
        return getByField("request_id",id);
    }

    public RequestApproval getApproval(List<String> stage, List<String> status, String id) throws Exception {
        return null;
    }

    private StateMachine<Stages, StageEvents> build(RequestApproval requestApproval){

        StateMachine<Stages, StageEvents> sm = this.factory.getStateMachine(requestApproval.getId());
        sm.stop();

        sm.getStateMachineAccessor()
                .doWithAllRegions(sma -> {

                    sma.addStateMachineInterceptor( new StateMachineInterceptorAdapter<Stages, StageEvents>(){

                        @Override
                        public void postStateChange(State state, Message message, Transition transition, StateMachine stateMachine) {
                            Optional.ofNullable(message).ifPresent(msg -> {
                                Optional.ofNullable((RequestApproval) msg.getHeaders().get("requestApprovalObj"))
                                        .ifPresent(requestApproval ->{
                                            requestApproval = get(requestApproval.getId());
                                            requestApproval.setLastModified(new LastModified((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal(), new Date().toInstant().toEpochMilli()));
                                            requestApproval.setCurrentStage(state.getId()+""); // <-- casting to String causes uncertain behavior. Keep it this way

                                            Request request = requestService.get(requestApproval.getRequestId());
                                            request.setLastModified(new LastModified((String) SecurityContextHolder.getContext().getAuthentication().getPrincipal(), new Date().toInstant().toEpochMilli()));
                                            try {
                                                logger.info("Updating "+ requestApproval.getId()+" request approval's stage to " + state.getId());
                                                requestService.update(request,request.getId());
                                                update(requestApproval, requestApproval.getId());
                                            } catch (ResourceNotFoundException e) {
                                                throw new ServiceException("Request approval with id " + requestApproval.getId() + " not found");
                                            }
                                            msg.getHeaders().replace("requestApprovalObj",requestApproval);
                                        });
                            });
                        }
                    });

                    sma.resetStateMachine(new DefaultStateMachineContext<>(
                            Stages.valueOf((requestApproval.getCurrentStage() == null ? Stages.Stage1.name() : requestApproval.getCurrentStage())), null, null, null));

                    logger.info("Resetting machine of request approval " + requestApproval.getId() + " at state " + sm.getState().getId());
                });

        sm.start();
        return sm;
    }

    @PreAuthorize("hasPermission(#requestApproval,'EDIT')")
    public void finalize(RequestApproval requestApproval){
        logger.info("Finalizing request approval with id " + requestApproval.getId());
        StateMachine<Stages, StageEvents> sm = this.build(requestApproval);
        Message<StageEvents> eventsMessage = MessageBuilder.withPayload(StageEvents.FINALIZE)
                .setHeader("requestApprovalObj", requestApproval)
                .build();

        sm.sendEvent(eventsMessage);
        if(sm.hasStateMachineError())
            throw new ServiceException((String) sm.getExtendedState().getVariables().get("error"));

        sm.stop();
    }


    @PreAuthorize("hasPermission(#requestApproval,'EDIT')")
    public void approve(RequestApproval requestApproval, HttpServletRequest req) {
        logger.info("Approving request approval with id " + requestApproval.getId());
        StateMachine<Stages, StageEvents> sm = this.build(requestApproval);
        Message<StageEvents> eventsMessage = MessageBuilder.withPayload(StageEvents.APPROVE)
                .setHeader("requestApprovalObj", requestApproval)
                .setHeader("restRequest", req)
                .build();

        sm.sendEvent(eventsMessage);
        if(sm.hasStateMachineError())
            throw new ServiceException((String) sm.getExtendedState().getVariables().get("error"));

        sm.stop();

    }


    @PreAuthorize("hasPermission(#requestApproval,'EDIT')")
    public void reject(RequestApproval requestApproval, HttpServletRequest req) {
        logger.info("Rejecting request approval with id " + requestApproval.getId());
        StateMachine<Stages, StageEvents> sm = this.build(requestApproval);
        Message<StageEvents> eventsMessage = MessageBuilder.withPayload(StageEvents.REJECT)
                .setHeader("requestApprovalObj", requestApproval)
                .setHeader("restRequest", req)
                .build();
        sm.sendEvent(eventsMessage);
        if(sm.hasStateMachineError())
            throw new ServiceException((String) sm.getExtendedState().getVariables().get("error"));

        sm.stop();

    }


    @PreAuthorize("hasPermission(#requestApproval,'EDIT')")
    public void downgrade(RequestApproval requestApproval, HttpServletRequest req) {
        logger.info("Downgrading request approval with id " + requestApproval.getId());
        StateMachine<Stages, StageEvents> sm = this.build(requestApproval);
        Message<StageEvents> eventsMessage = MessageBuilder.withPayload(StageEvents.DOWNGRADE)
                .setHeader("requestApprovalObj", requestApproval)
                .setHeader("restRequest", req)
                .build();

        sm.sendEvent(eventsMessage);
        if(sm.hasStateMachineError())
            throw new ServiceException((String) sm.getExtendedState().getVariables().get("error"));
        sm.stop();

    }

    @PreAuthorize("hasPermission(#requestApproval,'CANCEL')")
    public void cancel(RequestApproval requestApproval) throws Exception {
        logger.info("Canceling request approval with id " + requestApproval.getId());
        StateMachine<Stages, StageEvents> sm = this.build(requestApproval);
        Message<StageEvents> eventsMessage = MessageBuilder.withPayload(StageEvents.CANCEL)
                .setHeader("requestApprovalObj", requestApproval)
                .build();

        sm.sendEvent(eventsMessage);
        if(sm.hasStateMachineError())
            throw new ServiceException((String) sm.getExtendedState().getVariables().get("error"));
        sm.stop();
    }

    @PreAuthorize("hasPermission(#requestApproval,'WRITE')")
    public void edit(RequestApproval requestApproval, HttpServletRequest req) {
        logger.info("Editing request approval with id " + requestApproval.getId());
        StateMachine<Stages, StageEvents> sm = this.build(requestApproval);
        Message<StageEvents> eventsMessage = MessageBuilder.withPayload(StageEvents.EDIT)
                .setHeader("requestApprovalObj", requestApproval)
                .setHeader("restRequest", req)
                .build();

        sm.sendEvent(eventsMessage);
        if(sm.hasStateMachineError())
            throw new ServiceException((String) sm.getExtendedState().getVariables().get("error"));
        sm.stop();

    }

    @PreAuthorize("hasPermission(#requestApproval,'READ')")
    public RequestResponse getRequestResponse(RequestApproval requestApproval) throws Exception {
        Request request = requestService.get(requestApproval.getRequestId());
        Project project = projectService.get(request.getProjectId());
        Institute institute = instituteService.get(project.getInstituteId());

        Map<String, Stage> stages = new HashMap<>();

        RequestResponse requestResponse = new RequestResponse();

        BaseInfo baseInfo = new BaseInfo();
        baseInfo.setId(requestApproval.getId());
        baseInfo.setCreationDate(requestApproval.getCreationDate());
        baseInfo.setRequestId(requestApproval.getRequestId());
        baseInfo.setStage(requestApproval.getStage());
        baseInfo.setStatus(requestApproval.getStatus());

        List<Class> stagesClasses = Arrays.stream(RequestApproval.class.getDeclaredFields()).filter(p-> Stage.class.isAssignableFrom(p.getType())).flatMap(p -> Stream.of(p.getType())).collect(Collectors.toList());
        for(Class stageClass : stagesClasses){
            if(RequestApproval.class.getMethod("get"+stageClass.getSimpleName()).invoke(requestApproval)!=null)
                 stages.put(stageClass.getSimpleName().replace("Stage",""),(Stage) RequestApproval.class.getMethod("get"+stageClass.getSimpleName()).invoke(requestApproval));
        }
        requestResponse.setBaseInfo(baseInfo);
        requestResponse.setRequesterPosition(request.getRequesterPosition());
        requestResponse.setType(request.getType());
        requestResponse.setRequestStatus(request.getRequestStatus());
        requestResponse.setStages(stages);
        requestResponse.setProjectAcronym(project.getAcronym());
        requestResponse.setInstituteName(institute.getName());
        requestResponse.setRequesterFullName(request.getUser().getFirstname() + " " + request.getUser().getLastname());
        requestResponse.setRequesterEmail(request.getUser().getEmail());
        if(request.getOnBehalfOf()!=null) {
            requestResponse.setOnBehalfFullName(request.getOnBehalfOf().getFirstname() + " " + request.getOnBehalfOf().getLastname());
            requestResponse.setOnBehalfEmail(request.getOnBehalfOf().getEmail());
        }
        if(request.getTrip()!=null)
            requestResponse.setTripDestination(request.getTrip().getDestination());

        requestResponse.setCanEdit(hasPermission(requestApproval.getId(),32));
        requestResponse.setCanEditPrevious(hasPermission(requestApproval.getId(),2));

        return requestResponse;
    }


    public String generateID(String requestId) {
        return requestId+"-a1";
    }


    public boolean hasPermission(String approvalOrEditId,int mask){
        //if mask=32 we are looking for EDIT right
        //if mask==2 we are looking for WRITE right
        String roles = "";
        for(GrantedAuthority grantedAuthority : SecurityContextHolder.getContext().getAuthentication().getAuthorities()){
            roles = roles.concat(" or acl_sid.sid='"+grantedAuthority.getAuthority()+"'");
        }
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String aclEntriesQuery = "SELECT object_id_identity, canEdit FROM acl_object_identity INNER JOIN (select distinct acl_object_identity, CASE WHEN mask="+mask+" THEN true ELSE false END AS canEdit from acl_entry INNER JOIN acl_sid ON acl_sid.id=acl_entry.sid where acl_sid.sid='"+email+"' and acl_entry.mask="+mask+") as acl_entries ON acl_entries.acl_object_identity=acl_object_identity.id where acl_object_identity.object_id_identity='"+approvalOrEditId+"'";
        return new JdbcTemplate(dataSource).query(aclEntriesQuery , rs -> {

            if(rs.next())
                return rs.getBoolean("canEdit");
            else
                return false;
        });

    }

    @Override
    public RequestApproval getByField(String key, String value) throws Exception {
        return super.getByField(key, value);
    }
}
