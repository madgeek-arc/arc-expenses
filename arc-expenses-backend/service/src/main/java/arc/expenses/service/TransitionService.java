package arc.expenses.service;


import arc.expenses.domain.StageEvents;
import arc.expenses.domain.Stages;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.store.restclient.StoreRESTClient;
import gr.athenarc.domain.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.hibernate.service.spi.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.statemachine.StateContext;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service("transitionService")
public class TransitionService{

    private static Logger logger = LogManager.getLogger(TransitionService.class);

    @Autowired
    private RequestApprovalServiceImpl requestApprovalService;

    @Autowired
    private AclService aclService;

    @Autowired
    private ProjectServiceImpl projectService;

    @Autowired
    private InstituteServiceImpl instituteService;

    @Autowired
    private OrganizationServiceImpl organizationService;

    @Autowired
    private RequestServiceImpl requestService;

    @Autowired
    private MailService mailService;

    @Autowired
    private StoreRESTClient storeRESTClient;

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private RequestPaymentServiceImpl requestPaymentService;


    public boolean checkContains(StateContext<Stages, StageEvents> context, Class stageClass){

        if(context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class) == null && context.getMessage().getHeaders().get("paymentObj", RequestPayment.class)==null) {
            context.getStateMachine().setStateMachineError(new ServiceException("Both request approval and payment objects are empty"));
            return false;
        }

        HttpServletRequest req = context.getMessage().getHeaders().get("restRequest", HttpServletRequest.class);
        if(req == null) {
            context.getStateMachine().setStateMachineError(new ServiceException("Http request is empty"));
            return false;
        }
        List<String> requiredFields = Arrays.stream(stageClass.getDeclaredFields()).filter(p -> p.isAnnotationPresent(NotNull.class)).flatMap(p -> Stream.of(p.getName())).collect(Collectors.toList());
        Map<String, String[]> parameters = req.getParameterMap();
        for(String field: requiredFields){
            if(!parameters.containsKey(field)) {
                context.getStateMachine().setStateMachineError(new ServiceException(field + " is required"));
                return false;
            }
        }

        return true;
    }

    public void cancelRequestApproval(
            StateContext<Stages, StageEvents> context,
            String stage) throws Exception {

        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);

        Request request = requestService.get(requestApproval.getRequestId());
        request.setRequestStatus(Request.RequestStatus.CANCELLED);

        requestApproval.setStage(stage+"");
        requestApproval.setStatus(BaseInfo.Status.CANCELLED);
        requestApprovalService.update(requestApproval,requestApproval.getId());

        mailService.sendMail("Canceled",request.getPois());

        requestService.update(request, request.getId());
        aclService.deleteAcl(new ObjectIdentityImpl(Request.class,request.getId()), true);
    }

    public void cancelRequestPayment(
            StateContext<Stages, StageEvents> context,
            String stage) throws Exception {

        RequestPayment requestPayment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
        HttpServletRequest req = context.getMessage().getHeaders().get("restRequest", HttpServletRequest.class);
        boolean wholeRequest = Boolean.parseBoolean(Optional.ofNullable(req.getParameter("cancel_request")).orElse("false"));

        Request request = requestService.get(requestPayment.getRequestId());
        if(wholeRequest)
            request.setRequestStatus(Request.RequestStatus.CANCELLED);

        requestPayment.setStage(stage+"");
        requestPayment.setStatus(BaseInfo.Status.CANCELLED);
        requestPaymentService.update(requestPayment,requestPayment.getId());

        mailService.sendMail("Canceled",request.getPois());

        requestService.update(request, request.getId());
        aclService.deleteAcl(new ObjectIdentityImpl(Request.class,request.getId()), true);
    }

    public void modifyRequestApproval(
            StateContext<Stages, StageEvents> context,
            Stage stage,
            String stageString,
            BaseInfo.Status status) throws Exception {

        HttpServletRequest req = context.getMessage().getHeaders().get("restRequest", HttpServletRequest.class);
        MultipartHttpServletRequest multiPartRequest = (MultipartHttpServletRequest) req;

        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
        Request request = requestService.get(requestApproval.getRequestId());

        String comment = Optional.ofNullable(req.getParameter("comment")).orElse("");

        ArrayList<Attachment> attachments = new ArrayList<>();
        for(MultipartFile file : multiPartRequest.getFiles("attachments")){
            storeRESTClient.storeFile(file.getBytes(), request.getArchiveId()+"/stage"+stageString, file.getOriginalFilename());
            attachments.add(new Attachment(file.getOriginalFilename(), FileUtils.extension(file.getOriginalFilename()),new Long(file.getSize()+""), request.getArchiveId()+"/stage"+stageString));
        }

        stage.setAttachments(attachments);
        stage.setComment(comment);
        try {
            User user = userService.getByField("user_email",(String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            stage.setUser(user);
        } catch (Exception e) {
            context.getStateMachine().setStateMachineError(new ServiceException("User not found"));
            throw new ServiceException("User not found");
        }

        if(stage instanceof Stage1)
            requestApproval.setStage1((Stage1) stage);
        else if(stage instanceof Stage2)
            requestApproval.setStage2((Stage2) stage);
        else if(stage instanceof Stage3)
            requestApproval.setStage3((Stage3) stage);
        else if(stage instanceof Stage4)
            requestApproval.setStage4((Stage4) stage);
        else if(stage instanceof Stage5a)
            requestApproval.setStage5a((Stage5a) stage);
        else if(stage instanceof Stage5b)
            requestApproval.setStage5b((Stage5b) stage);
        else if(stage instanceof Stage6)
            requestApproval.setStage6((Stage6) stage);

        requestApproval.setStage(stageString);
        requestApproval.setStatus(status);
        if(status== BaseInfo.Status.ACCEPTED) {
            requestApproval.setCurrentStage(Stages.FINISHED.name());
            if(request.getType() == Request.Type.CONTRACT){
                request.setRequestStatus(Request.RequestStatus.ACCEPTED);
            }else{
                requestPaymentService.createPayment(request);
            }
            requestService.update(request,request.getId());
        }
        requestApprovalService.update(requestApproval,requestApproval.getId());
        if(status == BaseInfo.Status.REJECTED){
//            aclService.deleteAcl(new ObjectIdentityImpl(Request.class,request.getId()), true);
            mailService.sendMail("Rejected", request.getPois());
        }
    }

    public void modifyRequestPayment(
            StateContext<Stages, StageEvents> context,
            Stage stage,
            String stageString,
            BaseInfo.Status status) throws Exception {

        HttpServletRequest req = context.getMessage().getHeaders().get("restRequest", HttpServletRequest.class);
        MultipartHttpServletRequest multiPartRequest = (MultipartHttpServletRequest) req;

        RequestPayment requestPayment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
        Request request = requestService.get(requestPayment.getRequestId());
        String comment = Optional.ofNullable(req.getParameter("comment")).orElse("");

        ArrayList<Attachment> attachments = new ArrayList<>();
        for(MultipartFile file : multiPartRequest.getFiles("attachments")){
            storeRESTClient.storeFile(file.getBytes(), request.getArchiveId()+"/stage"+stageString, file.getOriginalFilename());
            attachments.add(new Attachment(file.getOriginalFilename(), FileUtils.extension(file.getOriginalFilename()),new Long(file.getSize()+""), request.getArchiveId()+"/stage"+stageString));
        }

        stage.setAttachments(attachments);
        stage.setComment(comment);
        try {
            User user = userService.getByField("user_email",(String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            stage.setUser(user);
        } catch (Exception e) {
            context.getStateMachine().setStateMachineError(new ServiceException("User not found"));
            throw new ServiceException("User not found");
        }

        if(stage instanceof Stage7)
            requestPayment.setStage7((Stage7) stage);
        else if(stage instanceof Stage8)
            requestPayment.setStage8((Stage8) stage);
        else if(stage instanceof Stage9)
            requestPayment.setStage9((Stage9) stage);
        else if(stage instanceof Stage10)
            requestPayment.setStage10((Stage10) stage);
        else if(stage instanceof Stage11)
            requestPayment.setStage11((Stage11) stage);
        else if(stage instanceof Stage12)
            requestPayment.setStage12((Stage12) stage);
        else if(stage instanceof Stage13)
            requestPayment.setStage13((Stage13) stage);

        requestPayment.setStage(stageString);
        requestPayment.setStatus(status);
        requestPaymentService.update(requestPayment,requestPayment.getId());

        if(status == BaseInfo.Status.REJECTED){
//            aclService.deleteAcl(new ObjectIdentityImpl(Request.class,request.getId()), true);
            mailService.sendMail("Rejected", request.getPois());
        }
    }

    public void approveApproval(StateContext<Stages, StageEvents> context, String fromStage, String toStage, Stage stage) throws Exception {
        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
        Request request = requestService.get(requestApproval.getRequestId());
        modifyRequestApproval(context, stage, toStage, BaseInfo.Status.PENDING);
        updatingPermissions(fromStage,toStage,request, "Approve", RequestApproval.class,requestApproval.getId());

        if(toStage.equals("5a") || toStage.equals("5b")){
            Project project = projectService.get(request.getProjectId());
            Institute institute = instituteService.get(project.getInstituteId());
            Organization organization = organizationService.get(institute.getOrganizationId());

            request.setDiataktis(institute.getDiataktis());

            if((project.getScientificCoordinatorAsDiataktis()!=null && project.getScientificCoordinatorAsDiataktis()) && request.getFinalAmount()<=2500  && requestService.exceedsProjectBudget(project.getScientificCoordinator(),project.getId(), request.getFinalAmount()))
                request.setDiataktis(project.getScientificCoordinator());

            if(request.getUser().getEmail().equals(request.getDiataktis().getEmail())){
                if(request.getUser().getEmail().equals(organization.getDirector().getEmail()))
                    request.setDiataktis(organization.getViceDirector());
                else
                    request.setDiataktis(organization.getDirector());
            }

            requestService.update(request,request.getId());
        }

    }

    public void approvePayment(StateContext<Stages, StageEvents> context, String fromStage, String toStage, Stage stage) throws Exception {
        RequestPayment requestPayment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
        modifyRequestPayment(context, stage, toStage, BaseInfo.Status.PENDING);

        Request request = requestService.get(requestPayment.getRequestId());


        if(toStage.equals("13") && fromStage.equals("13")){ // that's the signal for a finished payment
            Browsing<RequestPayment> payments = requestPaymentService.getPayments(request.getId(),null);
            if(payments.getTotal()>=request.getPaymentCycles()){ //if we have reached the max of payment cycles then request should be automatically move to FINISHED state
                requestApprovalService.finalize(requestApprovalService.getApproval(request.getId()));
            }else{ //if we haven't yet, create another payment request
                requestPaymentService.createPayment(request);
                updatingPermissions("6","7", request,"Approve",RequestPayment.class,requestPayment.getId());
                return;
            }
        }


        updatingPermissions(fromStage,toStage, request, "Approve",RequestPayment.class,requestPayment.getId());
        updatingPermissions(fromStage,toStage,request,"Approve",RequestApproval.class,requestApprovalService.getApproval(request.getId()).getId());

    }

    public void rejectApproval(StateContext<Stages, StageEvents> context, Stage stage, String rejectedAt) throws Exception {
        modifyRequestApproval(context, stage,rejectedAt, BaseInfo.Status.REJECTED);
    }

    public void rejectPayment(StateContext<Stages, StageEvents> context,Stage stage, String rejectedAt) throws Exception {
        modifyRequestPayment(context, stage,rejectedAt, BaseInfo.Status.REJECTED);
    }


    public void downgradeApproval(StateContext<Stages, StageEvents> context, String fromStage, String toStage, Stage stage){
        RequestApproval requestApproval = context.getMessage().getHeaders().get("requestApprovalObj", RequestApproval.class);
        MultipartHttpServletRequest req = (MultipartHttpServletRequest) context.getMessage().getHeaders().get("restRequest", HttpServletRequest.class);
        String comment = Optional.ofNullable(req.getParameter("comment")).orElse("");
        if(comment.isEmpty()) {
            context.getStateMachine().setStateMachineError(new ServiceException("We need a comment!"));
            throw new ServiceException("We need a comment!");
        }
        try {
            Request request = requestService.get(requestApproval.getRequestId());
            modifyRequestApproval(context, stage,toStage, BaseInfo.Status.UNDER_REVIEW);
            updatingPermissions(fromStage,toStage,request,"Downgrade",RequestApproval.class,requestApproval.getId());
        } catch (Exception e) {
            logger.error("Error occurred on approval of request " + requestApproval.getId(),e);
            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
            throw new ServiceException(e.getMessage());
        }
    }

    public void downgradePayment(StateContext<Stages, StageEvents> context, String fromStage, String toStage, Stage stage){
        RequestPayment requestPayment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
        MultipartHttpServletRequest req = (MultipartHttpServletRequest) context.getMessage().getHeaders().get("restRequest", HttpServletRequest.class);
        String comment = Optional.ofNullable(req.getParameter("comment")).orElse("");
        if(comment.isEmpty()) {
            context.getStateMachine().setStateMachineError(new ServiceException("We need a comment!"));
            throw new ServiceException("We need a comment!");
        }
        try {
            modifyRequestPayment(context, stage,toStage, BaseInfo.Status.UNDER_REVIEW);
            updatingPermissions(fromStage,toStage,requestService.get(requestPayment.getRequestId()),"Downgrade", RequestPayment.class,requestPayment.getId());
        } catch (Exception e) {
            logger.error("Error occurred on approval of payment " + requestPayment.getId(),e);
            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
            throw new ServiceException(e.getMessage());
        }
    }

    public void updatingPermissions(String from, String to, Request request, String mailType, Class persistentClass, String id){
        List<Sid> revokeAccess = new ArrayList<>();
        List<Sid> grantAccess = new ArrayList<>();
        Project project = projectService.get(request.getProjectId());
        Institute institute = instituteService.get(project.getInstituteId());
        Organization organization = organizationService.get(institute.getOrganizationId());

        switch (from){
            case "1":
                revokeAccess.add(new PrincipalSid(request.getUser().getEmail()));
                break;
            case "2":
                revokeAccess.add(new PrincipalSid(project.getScientificCoordinator().getEmail()));
                project.getScientificCoordinator().getDelegates().forEach(person -> revokeAccess.add(new PrincipalSid(person.getEmail())));
                break;
            case "3":
                project.getOperator().forEach(entry -> {
                    revokeAccess.add(new PrincipalSid(entry.getEmail()));
                    entry.getDelegates().forEach(person -> {
                        revokeAccess.add(new PrincipalSid(person.getEmail()));
                    });
                });
                break;
            case "9":
            case "4":
                revokeAccess.add(new PrincipalSid(organization.getPoy().getEmail()));
                organization.getPoy().getDelegates().forEach(delegate -> {
                    revokeAccess.add(new PrincipalSid(delegate.getEmail()));
                });
                break;
            case "10":
            case "5a":
                revokeAccess.add(new PrincipalSid(request.getDiataktis().getEmail()));
                request.getDiataktis().getDelegates().forEach( delegate -> {
                    revokeAccess.add(new PrincipalSid(delegate.getEmail()));
                });
                break;
            case "5b":
                revokeAccess.add(new PrincipalSid(organization.getDioikitikoSumvoulio().getEmail()));
                organization.getDioikitikoSumvoulio().getDelegates().forEach(delegate -> revokeAccess.add(new PrincipalSid(delegate.getEmail())));
                break;
            case "11":
            case "6":
                revokeAccess.add(new PrincipalSid(institute.getDiaugeia().getEmail()));
                institute.getDiaugeia().getDelegates().forEach(delegate -> revokeAccess.add(new PrincipalSid(delegate.getEmail())));
                break;
            case "7":
                if(request.getType() == Request.Type.TRIP) {
                    revokeAccess.add(new PrincipalSid(institute.getTravelManager().getEmail()));
                    institute.getTravelManager().getDelegates().forEach(delegate -> {
                        revokeAccess.add(new PrincipalSid(delegate.getEmail()));
                    });
                }else{
                    revokeAccess.add(new PrincipalSid(institute.getSuppliesOffice().getEmail()));
                    institute.getSuppliesOffice().getDelegates().forEach(delegate -> {
                        revokeAccess.add(new PrincipalSid(delegate.getEmail()));
                    });
                }
                break;
            case "8":
                organization.getInspectionTeam().forEach(inspector -> {
                    revokeAccess.add(new PrincipalSid(inspector.getEmail()));
                    inspector.getDelegates().forEach(delegate -> {
                        revokeAccess.add(new PrincipalSid(delegate.getEmail()));
                    });
                });
                break;
            case "12":
                revokeAccess.add(new PrincipalSid(institute.getAccountingRegistration().getEmail()));
                institute.getAccountingRegistration().getDelegates().forEach(delegate -> {
                    revokeAccess.add(new PrincipalSid(delegate.getEmail()));
                });
                break;
            case "13":
                revokeAccess.add(new PrincipalSid(institute.getAccountingPayment().getEmail()));
                institute.getAccountingPayment().getDelegates().forEach(delegate -> {
                    revokeAccess.add(new PrincipalSid(delegate.getEmail()));
                });
                break;
            default:
                break;

        }

        switch (to){
            case "1":
                grantAccess.add(new PrincipalSid(request.getUser().getEmail()));
                break;
            case "2":
                grantAccess.add(new PrincipalSid(project.getScientificCoordinator().getEmail()));
                project.getScientificCoordinator().getDelegates().forEach(person -> grantAccess.add(new PrincipalSid(person.getEmail())));
                break;
            case "3":
                project.getOperator().forEach(entry -> {
                    grantAccess.add(new PrincipalSid(entry.getEmail()));
                    entry.getDelegates().forEach(person -> {
                        grantAccess.add(new PrincipalSid(person.getEmail()));
                    });
                });
                break;
            case "9":
            case "4":
                grantAccess.add(new PrincipalSid(organization.getPoy().getEmail()));
                organization.getPoy().getDelegates().forEach(delegate -> {
                    grantAccess.add(new PrincipalSid(delegate.getEmail()));
                });
                break;

            case "10":
            case "5a":
                grantAccess.add(new PrincipalSid(request.getDiataktis().getEmail()));
                request.getDiataktis().getDelegates().forEach( delegate -> {
                    grantAccess.add(new PrincipalSid(delegate.getEmail()));
                });
                break;
            case "5b":
                grantAccess.add(new PrincipalSid(organization.getDioikitikoSumvoulio().getEmail()));
                organization.getDioikitikoSumvoulio().getDelegates().forEach(delegate -> {
                    grantAccess.add(new PrincipalSid(delegate.getEmail()));
                });
                break;
            case "11":
            case "6":
                grantAccess.add(new PrincipalSid(institute.getDiaugeia().getEmail()));
                institute.getDiaugeia().getDelegates().forEach(delegate -> {
                    grantAccess.add(new PrincipalSid(delegate.getEmail()));
                });
                break;
            case "7":
                if(request.getType() == Request.Type.TRIP) {
                    grantAccess.add(new PrincipalSid(institute.getTravelManager().getEmail()));
                    institute.getTravelManager().getDelegates().forEach(delegate -> {
                        grantAccess.add(new PrincipalSid(delegate.getEmail()));
                    });
                }else{
                    grantAccess.add(new PrincipalSid(institute.getSuppliesOffice().getEmail()));
                    institute.getSuppliesOffice().getDelegates().forEach(delegate -> {
                        grantAccess.add(new PrincipalSid(delegate.getEmail()));
                    });
                }
                break;
            case "8":
                organization.getInspectionTeam().forEach(inspector -> {
                    grantAccess.add(new PrincipalSid(inspector.getEmail()));
                    inspector.getDelegates().forEach(delegate -> {
                        grantAccess.add(new PrincipalSid(delegate.getEmail()));
                    });
                });
                break;
            case "12":
                grantAccess.add(new PrincipalSid(institute.getAccountingRegistration().getEmail()));
                institute.getAccountingRegistration().getDelegates().forEach(delegate -> {
                    grantAccess.add(new PrincipalSid(delegate.getEmail()));
                });
                break;
            case "13":
                grantAccess.add(new PrincipalSid(institute.getAccountingPayment().getEmail()));
                institute.getAccountingPayment().getDelegates().forEach(delegate -> {
                    grantAccess.add(new PrincipalSid(delegate.getEmail()));
                });
                break;
            default:
                break;
        }

        aclService.updateAclEntries(revokeAccess,grantAccess,id, persistentClass);
        if(!mailType.isEmpty())
            mailService.sendMail(mailType, grantAccess.stream().map(entry -> ((PrincipalSid) entry).getPrincipal()).collect(Collectors.toList()));

        List<String> pois = request.getPois();

        grantAccess.forEach(granted -> {
            if(!pois.contains(((PrincipalSid) granted).getPrincipal())){
                pois.add(((PrincipalSid) granted).getPrincipal());
            }
        });

        request.setPois(pois);
        try {
            requestService.update(request,request.getId());
        } catch (ResourceNotFoundException e) {
            logger.error("Failed to update request with POIs",e);
        }

    }


}
