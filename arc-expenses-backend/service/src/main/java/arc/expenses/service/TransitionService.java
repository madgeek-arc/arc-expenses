package arc.expenses.service;


import arc.expenses.acl.ArcPermission;
import arc.expenses.domain.StageEvents;
import arc.expenses.domain.Stages;
import eu.openminted.registry.core.service.ServiceException;
import eu.openminted.store.restclient.StoreRESTClient;
import gr.athenarc.domain.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
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
import java.time.LocalDate;
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


    public boolean checkContains(Message<StageEvents> message, Class stageClass){

        if(message.getHeaders().get("requestObj", Request.class) == null && message.getHeaders().get("paymentObj", RequestPayment.class)==null) {
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

    public void cancelRequestApproval(
            StateContext<Stages, StageEvents> context,
            String stage) throws Exception {

        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
        request.setRequestStatus(Request.RequestStatus.CANCELLED);

        RequestApproval requestApproval = requestApprovalService.getByField("request_id", request.getId());
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
        Request request = requestService.get(requestPayment.getRequestId());
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
        MultipartHttpServletRequest multiPartRequest = context.getMessage().getHeaders().get("restRequest", MultipartHttpServletRequest.class);

        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
        String comment = Optional.ofNullable(req.getParameter("comment")).orElse("");

        ArrayList<Attachment> attachments = new ArrayList<>();
        for(MultipartFile file : multiPartRequest.getFiles("attachments")){
            storeRESTClient.storeFile(file.getBytes(), request.getArchiveId(), file.getOriginalFilename());
            attachments.add(new Attachment(file.getOriginalFilename(), FileUtils.extension(file.getOriginalFilename()),new Long(file.getSize()+""), request.getArchiveId()+"/stage1"));
        }

        RequestApproval requestApproval = requestApprovalService.getByField("request_id",request.getId());
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
        MultipartHttpServletRequest multiPartRequest = context.getMessage().getHeaders().get("restRequest", MultipartHttpServletRequest.class);

        RequestPayment requestPayment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
        Request request = requestService.get(requestPayment.getRequestId());
        String comment = Optional.ofNullable(req.getParameter("comment")).orElse("");

        ArrayList<Attachment> attachments = new ArrayList<>();
        for(MultipartFile file : multiPartRequest.getFiles("attachments")){
            storeRESTClient.storeFile(file.getBytes(), request.getArchiveId(), file.getOriginalFilename());
            attachments.add(new Attachment(file.getOriginalFilename(), FileUtils.extension(file.getOriginalFilename()),new Long(file.getSize()+""), request.getArchiveId()+"/stage1"));
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

    public void movingToStage7(StateContext<Stages, StageEvents> context) throws Exception {
        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
        String toStage = "7";
        if(request.getType() == Request.Type.CONTRACT) {
            modifyRequestApproval(context, new Stage6(), "13", BaseInfo.Status.ACCEPTED);
            toStage = "13";
        }else{
            RequestPayment requestPayment = new RequestPayment();
            requestPayment.setId(request.getId()+"-p1");
            requestPayment.setRequestId(request.getId());
            requestPayment.setCreationDate(LocalDate.now().toEpochDay());
            requestPayment.setStage("7");
            requestPayment.setStatus(BaseInfo.Status.PENDING);
            requestPayment.setCurrentStage(Stages.Stage7.name());
            requestPaymentService.add(requestPayment,null);
            toStage = "7";

        }
        Map<String,List<Sid>> returnValues = updatingPermissions("6",toStage, request,"Approve");

        List<String> pois = request.getPois();

        returnValues.get("grant").forEach(granted -> {
            if(!pois.contains(((PrincipalSid) granted).getPrincipal())){
                pois.add(((PrincipalSid) granted).getPrincipal());
            }
        });

        aclService.removePermissionFromSid(Collections.singletonList(ArcPermission.CANCEL),new PrincipalSid(request.getUser().getEmail()),request.getId(),Request.class); //request can no longer get canceled
        request.setPois(pois);
        requestService.update(request,request.getId());

    }

    public void approveApproval(StateContext<Stages, StageEvents> context, String fromStage, String toStage, Stage stage) throws Exception {
        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
        modifyRequestApproval(context, stage, toStage, BaseInfo.Status.PENDING);
        Map<String,List<Sid>> returnValues = updatingPermissions(fromStage,toStage,request, "Approve");

        List<String> pois = request.getPois();

        returnValues.get("grant").forEach(granted -> {
            if(!pois.contains(((PrincipalSid) granted).getPrincipal())){
                pois.add(((PrincipalSid) granted).getPrincipal());
            }
        });

        request.setPois(pois);
        requestService.update(request,request.getId());
    }

    public void approvePayment(StateContext<Stages, StageEvents> context, String fromStage, String toStage, Stage stage) throws Exception {
        RequestPayment requestPayment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
        modifyRequestPayment(context, stage, toStage, BaseInfo.Status.PENDING);

        Request request = requestService.get(requestPayment.getRequestId());

        Map<String,List<Sid>> returnValues = updatingPermissions(fromStage,toStage, request, "Approve");

        List<String> pois = request.getPois();

        returnValues.get("grant").forEach(granted -> {
            if(!pois.contains(((PrincipalSid) granted).getPrincipal())){
                pois.add(((PrincipalSid) granted).getPrincipal());
            }
        });

        request.setPois(pois);
        requestService.update(request,request.getId());
    }

    public void rejectApproval(StateContext<Stages, StageEvents> context, Stage stage, String rejectedAt) throws Exception {
        modifyRequestApproval(context, stage,rejectedAt, BaseInfo.Status.REJECTED);
    }

    public void rejectPayment(StateContext<Stages, StageEvents> context,Stage stage, String rejectedAt) throws Exception {
        modifyRequestPayment(context, stage,rejectedAt, BaseInfo.Status.REJECTED);
    }


    public void downgradeApproval(StateContext<Stages, StageEvents> context, String fromStage, String toStage, Stage stage){
        Request request = context.getMessage().getHeaders().get("requestObj", Request.class);
        MultipartHttpServletRequest req = context.getMessage().getHeaders().get("restRequest", MultipartHttpServletRequest.class);
        String comment = Optional.ofNullable(req.getParameter("comment")).orElse("");
        if(comment.isEmpty()) {
            context.getStateMachine().setStateMachineError(new ServiceException("We need a comment!"));
            throw new ServiceException("We need a comment!");
        }
        try {
            modifyRequestApproval(context, stage,toStage, BaseInfo.Status.UNDER_REVIEW);
            updatingPermissions(fromStage,toStage,request,"Downgrade");
        } catch (Exception e) {
            logger.error("Error occurred on approval of request " + request.getId(),e);
            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
            throw new ServiceException(e.getMessage());
        }
    }

    public void downgradePayment(StateContext<Stages, StageEvents> context, String fromStage, String toStage, Stage stage){
        RequestPayment requestPayment = context.getMessage().getHeaders().get("paymentObj", RequestPayment.class);
        MultipartHttpServletRequest req = context.getMessage().getHeaders().get("restRequest", MultipartHttpServletRequest.class);
        String comment = Optional.ofNullable(req.getParameter("comment")).orElse("");
        if(comment.isEmpty()) {
            context.getStateMachine().setStateMachineError(new ServiceException("We need a comment!"));
            throw new ServiceException("We need a comment!");
        }
        try {
            modifyRequestPayment(context, stage,toStage, BaseInfo.Status.UNDER_REVIEW);
            updatingPermissions(fromStage,toStage,requestService.get(requestPayment.getRequestId()),"Downgrade");
        } catch (Exception e) {
            logger.error("Error occurred on approval of payment " + requestPayment.getId(),e);
            context.getStateMachine().setStateMachineError(new ServiceException(e.getMessage()));
            throw new ServiceException(e.getMessage());
        }
    }

    private Map<String,List<Sid>> updatingPermissions(String from, String to, Request request, String mailType){
        List<Sid> revokeAccess = new ArrayList<>();
        List<Sid> grantAccess = new ArrayList<>();
        Project project = projectService.get(request.getProjectId());
        Institute institute = instituteService.get(project.getInstituteId());
        Organization organization = organizationService.get(institute.getOrganizationId());

        switch (from){
            case "2":
                revokeAccess.add(new PrincipalSid(project.getScientificCoordinator().getEmail()));
                project.getScientificCoordinator().getDelegates().forEach(person -> revokeAccess.add(new PrincipalSid(person.getEmail())));
                break;
            case "3":
                PersonOfInterest scientificCoordinator = projectService.get(request.getProjectId()).getScientificCoordinator();
                revokeAccess.add(new PrincipalSid(scientificCoordinator.getEmail()));
                scientificCoordinator.getDelegates().forEach(person -> revokeAccess.add(new PrincipalSid(person.getEmail())));
                break;
            case "4":
                revokeAccess.add(new PrincipalSid(organization.getPoy().getEmail()));
                organization.getPoy().getDelegates().forEach(delegate -> {
                    revokeAccess.add(new PrincipalSid(delegate.getEmail()));
                });
                break;
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
            default:
                break;

        }

        switch (to){
            case "1":
                grantAccess.add(new PrincipalSid(request.getUser().getEmail()));
                break;
            case "2":
                grantAccess.add(new PrincipalSid(request.getUser().getEmail()));
                break;
            case "3":
                project.getOperator().forEach(entry -> {
                    grantAccess.add(new PrincipalSid(entry.getEmail()));
                    entry.getDelegates().forEach(person -> {
                        grantAccess.add(new PrincipalSid(person.getEmail()));
                    });
                });
                break;
            case "4":
                grantAccess.add(new PrincipalSid(organization.getPoy().getEmail()));
                organization.getPoy().getDelegates().forEach(delegate -> {
                    grantAccess.add(new PrincipalSid(delegate.getEmail()));
                });
                break;
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
            default:
                break;
        }

        aclService.updateAclEntries(revokeAccess,grantAccess,request.getId());
        mailService.sendMail(mailType, grantAccess.stream().map(entry -> ((PrincipalSid) entry).getPrincipal()).collect(Collectors.toList()));

        HashMap<String,List<Sid>> hashMap = new HashMap<>();
        hashMap.put("revoke",revokeAccess);
        hashMap.put("grant",grantAccess);

        return hashMap;

    }


}
