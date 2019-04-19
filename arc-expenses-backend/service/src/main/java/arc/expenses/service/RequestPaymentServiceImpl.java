package arc.expenses.service;

import arc.expenses.acl.ArcPermission;
import arc.expenses.domain.RequestResponse;
import arc.expenses.domain.StageEvents;
import arc.expenses.domain.Stages;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
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
import org.springframework.security.acls.domain.AclImpl;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.AlreadyExistsException;
import org.springframework.security.core.Authentication;
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

@Service("requestPayment")
public class RequestPaymentServiceImpl extends GenericService<RequestPayment> {


    private static Logger logger = LogManager.getLogger(RequestPaymentServiceImpl.class);

    @Autowired
    private RequestApprovalServiceImpl requestApprovalService;

    @Autowired
    private RequestServiceImpl requestService;

    @Autowired
    private ProjectServiceImpl projectService;

    @Autowired
    private InstituteServiceImpl instituteService;

    @Autowired
    private OrganizationServiceImpl organizationService;

    @Autowired
    private AclService aclService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    @Lazy
    private StateMachineFactory<Stages, StageEvents> factory;

    public RequestPaymentServiceImpl() {
        super(RequestPayment.class);
    }

    @Override
    public String getResourceType() {
        return "payment";
    }

    private StateMachine<Stages, StageEvents> build(RequestPayment payment){

        StateMachine<Stages, StageEvents> sm = this.factory.getStateMachine(payment.getId());
        sm.stop();

        sm.getStateMachineAccessor()
                .doWithAllRegions(sma -> {

                    sma.addStateMachineInterceptor( new StateMachineInterceptorAdapter<Stages, StageEvents>(){

                        @Override
                        public void postStateChange(State state, Message message, Transition transition, StateMachine stateMachine) {
                            Optional.ofNullable(message).ifPresent(msg -> {
                                Optional.ofNullable((RequestPayment) msg.getHeaders().get("paymentObj"))
                                        .ifPresent(payment ->{
                                            payment.setCurrentStage(state.getId()+""); // <-- casting to String causes uncertain behavior. Keep it this way
                                            try {
                                                logger.info("Updating "+ payment.getId()+" payment's stage to " + state.getId());
                                                update(payment, payment.getId());
                                            } catch (ResourceNotFoundException e) {
                                                throw new ServiceException("Request with id " + payment.getId() + " not found");
                                            }
                                            msg.getHeaders().replace("paymentObj",payment);
                                        });
                            });
                        }
                    });

                    sma.resetStateMachine(new DefaultStateMachineContext<>(
                            Stages.valueOf((payment.getCurrentStage() == null ? Stages.Stage7.name() : payment.getCurrentStage())), null, null, null));

                    logger.info("Resetting machine of payment " + payment.getId() + " at state " + sm.getState().getId());
                });

        sm.start();
        return sm;
    }


    @PreAuthorize("hasPermission(#requestPayment,'EDIT')")
    public void approve(RequestPayment requestPayment, HttpServletRequest req) {

        logger.info("Approving payment with id " + requestPayment.getId());
        StateMachine<Stages, StageEvents> sm = this.build(requestPayment);
        Message<StageEvents> eventsMessage = MessageBuilder.withPayload(StageEvents.APPROVE)
                .setHeader("paymentObj", requestPayment)
                .setHeader("restRequest", req)
                .build();

        sm.sendEvent(eventsMessage);
        if(sm.hasStateMachineError())
            throw new ServiceException((String) sm.getExtendedState().getVariables().get("error"));

        sm.stop();

    }


    @PreAuthorize("hasPermission(#requestPayment,'EDIT')")
    public void reject(RequestPayment requestPayment, HttpServletRequest req) {
        logger.info("Rejecting payment with id " + requestPayment.getId());
        StateMachine<Stages, StageEvents> sm = this.build(requestPayment);
        Message<StageEvents> eventsMessage = MessageBuilder.withPayload(StageEvents.REJECT)
                .setHeader("paymentObj", requestPayment)
                .setHeader("restRequest", req)
                .build();

        sm.sendEvent(eventsMessage);
        if(sm.hasStateMachineError())
            throw new ServiceException((String) sm.getExtendedState().getVariables().get("error"));

        sm.stop();

    }


    @PreAuthorize("hasPermission(#requestPayment,'EDIT')")
    public void downgrade(RequestPayment requestPayment, HttpServletRequest req) {
        logger.info("Downgrading payment with id " + requestPayment.getId());
        StateMachine<Stages, StageEvents> sm = this.build(requestPayment);
        Message<StageEvents> eventsMessage = MessageBuilder.withPayload(StageEvents.DOWNGRADE)
                .setHeader("paymentObj", requestPayment)
                .setHeader("restRequest", req)
                .build();

        sm.sendEvent(eventsMessage);
        if(sm.hasStateMachineError())
            throw new ServiceException((String) sm.getExtendedState().getVariables().get("error"));

        sm.stop();

    }

    @PreAuthorize("hasPermission(#requestPayment,'CANCEL')")
    public String cancel(RequestPayment requestPayment, HttpServletRequest req) throws Exception {
        logger.info("Canceling payment with id " + requestPayment.getId());
        StateMachine<Stages, StageEvents> sm = this.build(requestPayment);
        Message<StageEvents> eventsMessage = MessageBuilder.withPayload(StageEvents.CANCEL)
                .setHeader("paymentObj", requestPayment)
                .setHeader("restRequest", req)
                .build();

        sm.sendEvent(eventsMessage);
        if(sm.hasStateMachineError())
            throw new ServiceException((String) sm.getExtendedState().getVariables().get("error"));

        sm.stop();
        Browsing<RequestPayment> payments = getPayments(requestPayment.getRequestId(), null);
        if(payments.getTotal()>0)
            return payments.getResults().get(0).getId();
        else
            return "";
    }

    @PreAuthorize("hasPermission(#requestPayment,'WRITE')")
    public void edit(RequestPayment requestPayment, HttpServletRequest req) {
        logger.info("Rejecting request payment with id " + requestPayment.getId());
        StateMachine<Stages, StageEvents> sm = this.build(requestPayment);
        Message<StageEvents> eventsMessage = MessageBuilder.withPayload(StageEvents.EDIT)
                .setHeader("paymentObj", requestPayment)
                .setHeader("restRequest", req)
                .build();
        sm.sendEvent(eventsMessage);
        if(sm.hasStateMachineError())
            throw new ServiceException((String) sm.getExtendedState().getVariables().get("error"));

        sm.stop();

    }


    public RequestResponse getRequestResponse(String id) throws Exception {
        RequestPayment requestPayment = get(id);
        if(requestPayment == null)
            throw new ServiceException("Request approval not found");

        RequestApproval requestApproval = requestApprovalService.getApproval(requestPayment.getRequestId());

        Request request = requestService.get(requestApproval.getRequestId());
        Project project = projectService.get(request.getProjectId());
        Institute institute = instituteService.get(project.getInstituteId());

        Map<String, Stage> stages = new HashMap<>();

        RequestResponse requestResponse = new RequestResponse();

        BaseInfo baseInfo = new BaseInfo();
        baseInfo.setId(requestPayment.getId());
        baseInfo.setCreationDate(requestPayment.getCreationDate());
        baseInfo.setRequestId(requestPayment.getRequestId());
        baseInfo.setStage(requestPayment.getStage());
        baseInfo.setStatus(requestPayment.getStatus());

        stages.put("1",requestApproval.getStage1());
        List<Class> stagesClasses = Arrays.stream(RequestPayment.class.getDeclaredFields()).filter(p-> Stage.class.isAssignableFrom(p.getType())).flatMap(p -> Stream.of(p.getType())).collect(Collectors.toList());
        for(Class stageClass : stagesClasses){
            if(RequestPayment.class.getMethod("get"+stageClass.getSimpleName()).invoke(requestPayment)!=null)
                stages.put(stageClass.getSimpleName().replace("Stage",""),(Stage) RequestPayment.class.getMethod("get"+stageClass.getSimpleName()).invoke(requestPayment));
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

        requestResponse.setCanEdit(requestApprovalService.hasPermission(requestPayment.getId(),32));
        requestResponse.setCanEditPrevious(requestApprovalService.hasPermission(requestPayment.getId(),2));

        return requestResponse;
    }

    public RequestPayment createPayment(Request request) throws Exception {
        Browsing<RequestPayment> payments = getPayments(request.getId(),null);
        RequestPayment requestPayment = new RequestPayment();
        requestPayment.setId(generateID(request.getId()));
        requestPayment.setRequestId(request.getId());
        requestPayment.setCreationDate(new Date().toInstant().toEpochMilli());
        requestPayment.setStage("7");
        requestPayment.setStatus(BaseInfo.Status.PENDING);
        requestPayment.setCurrentStage(Stages.Stage7.name());

        requestPayment = add(requestPayment,null);

        try{
            aclService.createAcl(new ObjectIdentityImpl(RequestPayment.class, requestPayment.getId()));
        }catch (AlreadyExistsException ex){
            logger.debug("Object identity already exists");
        }
        Project project = projectService.get(request.getProjectId());
        Institute institute = instituteService.get(project.getInstituteId());

        AclImpl acl = (AclImpl) aclService.readAclById(new ObjectIdentityImpl(RequestPayment.class, requestPayment.getId()));
        acl.insertAce(acl.getEntries().size(), ArcPermission.CANCEL, new PrincipalSid(request.getUser().getEmail()), true);
        acl.insertAce(acl.getEntries().size(), ArcPermission.EDIT, new PrincipalSid(request.getUser().getEmail()), true);

        if(request.getOnBehalfOf()!=null)
            acl.insertAce(acl.getEntries().size(), ArcPermission.CANCEL, new PrincipalSid(request.getOnBehalfOf().getEmail()), true);

        if(request.getType() == Request.Type.TRIP) {
            acl.insertAce(acl.getEntries().size(), ArcPermission.EDIT, new PrincipalSid(institute.getTravelManager().getEmail()), true);
            acl.insertAce(acl.getEntries().size(), ArcPermission.READ, new PrincipalSid(institute.getTravelManager().getEmail()), true);
            institute.getTravelManager().getDelegates().forEach(delegate -> {
                acl.insertAce(acl.getEntries().size(), ArcPermission.EDIT, new PrincipalSid(delegate.getEmail()), true);
                acl.insertAce(acl.getEntries().size(), ArcPermission.READ, new PrincipalSid(delegate.getEmail()), true);
            });
        }else{
            acl.insertAce(acl.getEntries().size(), ArcPermission.EDIT, new PrincipalSid(institute.getSuppliesOffice().getEmail()), true);
            acl.insertAce(acl.getEntries().size(), ArcPermission.READ, new PrincipalSid(institute.getSuppliesOffice().getEmail()), true);

            institute.getTravelManager().getDelegates().forEach(delegate -> {
                acl.insertAce(acl.getEntries().size(), ArcPermission.EDIT, new PrincipalSid(delegate.getEmail()), true);
                acl.insertAce(acl.getEntries().size(), ArcPermission.READ, new PrincipalSid(delegate.getEmail()), true);
            });
        }

        if(request.getType() != Request.Type.SERVICES_CONTRACT){
            acl.insertAce(acl.getEntries().size(), ArcPermission.WRITE, new PrincipalSid(institute.getDiaugeia().getEmail()), true);
            institute.getDiaugeia().getDelegates().forEach(delegate -> acl.insertAce(acl.getEntries().size(), ArcPermission.WRITE, new PrincipalSid(delegate.getEmail()), true));
        }else{
            if(payments.getTotal()==0){
                acl.insertAce(acl.getEntries().size(), ArcPermission.WRITE, new PrincipalSid(institute.getDiaugeia().getEmail()), true);
                institute.getDiaugeia().getDelegates().forEach(delegate -> acl.insertAce(acl.getEntries().size(), ArcPermission.WRITE, new PrincipalSid(delegate.getEmail()), true));
            }
        }


        acl.insertAce(acl.getEntries().size(), ArcPermission.READ, new GrantedAuthoritySid("ROLE_ADMIN"), true);
        acl.insertAce(acl.getEntries().size(), ArcPermission.WRITE, new GrantedAuthoritySid("ROLE_ADMIN"), true);

        for(String oldPoi : request.getPois()) {
            acl.insertAce(acl.getEntries().size(), ArcPermission.READ, new PrincipalSid(oldPoi), true);
        }

        acl.setOwner(new GrantedAuthoritySid(("ROLE_USER")));
        aclService.updateAcl(acl);

        return requestPayment;
    }

    public Browsing<RequestPayment> getPayments(String id, Authentication u) throws Exception {
        FacetFilter filter = new FacetFilter();
        filter.setResourceType(getResourceType());
        filter.addFilter("request_id",id);

        filter.setKeyword("");
        filter.setFrom(0);
        filter.setQuantity(1000);

        Map<String,Object> sort = new HashMap<>();
        Map<String,Object> order = new HashMap<>();

        String orderDirection = "desc";
        String orderField = "creation_date";

        order.put("order",orderDirection);
        sort.put(orderField, order);
        filter.setOrderBy(sort);

        return getAll(filter,u);
    }

    public String generateID(String requestId) {
        String maxID = getMaxID(requestId);
        if(maxID == null)
            return requestId+"-p1";
        else
            return requestId+"-p"+(Integer.valueOf(maxID.split("-p")[1])+1);
    }


    public String getMaxID(String requestId) {
        return new JdbcTemplate(dataSource).query("select payment_id from payment_view where request_id=? order by creation_date desc limit 1", ps -> ps.setString(1,requestId), resultSet -> {
            if(resultSet.next())
                return resultSet.getString("payment_id");
            else
                return null;
        });
    }

    @Override
    public void delete(RequestPayment requestPayment) throws ResourceNotFoundException {
        super.delete(requestPayment);
    }
}

