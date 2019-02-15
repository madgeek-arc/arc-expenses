package arc.expenses.service;

import arc.expenses.domain.*;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ServiceException;
import eu.openminted.store.restclient.StoreRESTClient;
import gr.athenarc.domain.*;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.statemachine.support.StateMachineInterceptorAdapter;
import org.springframework.statemachine.transition.Transition;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service("requestService")
public class RequestServiceImpl extends GenericService<Request> {

    private static Logger logger = LogManager.getLogger(RequestServiceImpl.class);

    @Autowired
    private StoreRESTClient storeRESTClient;

    @Autowired
    RequestApprovalServiceImpl requestApprovalService;

    @Autowired
    RequestPaymentServiceImpl requestPaymentService;

    @Autowired
    ProjectServiceImpl projectService;

    @Autowired
    InstituteServiceImpl instituteService;

    @Autowired
    OrganizationServiceImpl organizationService;

    @Autowired
    UserServiceImpl userService;

    @Autowired
    DataSource dataSource;

    @Autowired
    MailService mailService;

    @Autowired
    private StateMachineFactory<Stages, StageEvents> factory;


    @Value("#{'${admin.emails}'.split(',')}")
    private List<String> admins;


    public RequestServiceImpl() {
        super(Request.class);
    }

    @Override
    public String getResourceType() {
        return "request";
    }


    public String generateID() {
        String maxID = getMaxID();
        if(maxID == null)
            return new SimpleDateFormat("yyyyMMdd").format(new Date())+"-1";
        else{
            String number[] = maxID.split("-");
            if(number[0].equals(new SimpleDateFormat("yyyyMMdd").format(new Date())))
                return number[0]+"-"+(Integer.parseInt(number[1])+1);
            else
                return new SimpleDateFormat("yyyyMMdd").format(new Date())+"-1";
        }
    }


    private StateMachine<Stages, StageEvents> build(Request request){

        StateMachine<Stages, StageEvents> sm = this.factory.getStateMachine(request.getId());
        sm.stop();

        sm.getStateMachineAccessor()
                .doWithAllRegions(sma -> {

                    sma.addStateMachineInterceptor( new StateMachineInterceptorAdapter<Stages, StageEvents>(){

                        @Override
                        public void postStateChange(State state, Message message, Transition transition, StateMachine stateMachine) {
                            Optional.ofNullable(message).ifPresent(msg -> {
                                Optional.ofNullable((Request) msg.getHeaders().get("requestObj"))
                                        .ifPresent(request ->{
                                            request.setCurrentStage(state.getId()+""); // <-- casting to String causes uncertain behavior. Keep it this way
                                            try {
                                                logger.info("Updating "+ request.getId()+" request's stage to " + state.getId());
                                                update(request, request.getId());
                                            } catch (ResourceNotFoundException e) {
                                                throw new ServiceException("Request with id " + request.getId() + " not found");
                                            }
                                            msg.getHeaders().replace("requestObj",request);
                                        });
                            });
                        }
                    });

                    sma.resetStateMachine(new DefaultStateMachineContext<>(
                            Stages.valueOf((request.getCurrentStage() == null ? Stages.Stage1.name() : request.getCurrentStage())), null, null, null));

                    logger.info("Resetting machine of request " + request.getId() + " at state " + sm.getState().getId());
                });

        sm.start();
        return sm;
    }

    @Override
    @PostAuthorize("hasPermission(#returnObject,'EDIT')")
    public Request get(String id) {
        return super.get(id);
    }

    @PreAuthorize("hasPermission(#request,'APPROVE')")
    public void approve(Request request, HttpServletRequest req) {
        logger.info("Approving request with id " + request.getId());
        StateMachine<Stages, StageEvents> sm = this.build(request);
        Message<StageEvents> eventsMessage = MessageBuilder.withPayload(StageEvents.APPROVE)
                .setHeader("requestObj", request)
                .setHeader("restRequest", req)
                .build();

        sm.sendEvent(eventsMessage);

        sm.stop();

    }


    @PreAuthorize("hasPermission(#request,'EDIT')")
    public void reject(Request request, HttpServletRequest req) {
        logger.info("Rejecting request with id " + request.getId());
        StateMachine<Stages, StageEvents> sm = this.build(request);
        Message<StageEvents> eventsMessage = MessageBuilder.withPayload(StageEvents.REJECT)
                .setHeader("requestObj", request)
                .setHeader("restRequest", req)
                .build();
        sm.sendEvent(eventsMessage);

        sm.stop();

    }


    @PreAuthorize("hasPermission(#request,'EDIT')")
    public void downgrade(Request request, HttpServletRequest req) {
        logger.info("Downgrading request with id " + request.getId());
        StateMachine<Stages, StageEvents> sm = this.build(request);
        Message<StageEvents> eventsMessage = MessageBuilder.withPayload(StageEvents.DOWNGRADE)
                .setHeader("requestObj", request)
                .setHeader("restRequest", req)
                .build();

        sm.sendEvent(eventsMessage);
        if(sm.hasStateMachineError())
            throw new ServiceException((String) sm.getExtendedState().getVariables().get("error"));
        sm.stop();

    }

    @PreAuthorize("hasPermission(#request,'CANCEL')")
    public void cancel(Request request) throws Exception {
        logger.info("Canceling request with id " + request.getId());
        request.setRequestStatus(Request.RequestStatus.CANCELLED);
        RequestApproval requestApproval = requestApprovalService.getByField("request_id", request.getId());
        if(requestApproval!=null)
            requestApproval.setStatus(BaseInfo.Status.CANCELLED);

        requestApprovalService.update(requestApproval, requestApproval.getId());
        update(request,request.getId());
    }

    public Request add(Request.Type type, String projectId, String subject, Request.RequesterPosition requesterPosition, String supplier, Stage1.SupplierSelectionMethod supplierSelectionMethod, double amount, Optional<List<MultipartFile>> files, String destination, String firstName, String lastName, String email) throws Exception {

        if((type == Request.Type.REGULAR || type == Request.Type.CONTRACT) && supplierSelectionMethod ==null)
            throw new ServiceException("Supplier selection method cannot be empty");

        if(supplierSelectionMethod != Stage1.SupplierSelectionMethod.AWARD_PROCEDURE && supplier.isEmpty())
            throw new ServiceException("Supplier cannot be empty");

        if(((supplierSelectionMethod == Stage1.SupplierSelectionMethod.AWARD_PROCEDURE || supplierSelectionMethod == Stage1.SupplierSelectionMethod.MARKET_RESEARCH) || amount>2500) && !files.isPresent())
           throw new ServiceException("Files must be included");

        List<String> emailReceivers = new ArrayList<>();


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Request request = new Request();
        request.setType(type);
        request.setId(generateID());
        request.setArchiveId(createArchive());
        Project project = projectService.get(projectId);
        if(project == null)
            throw new ServiceException("Project with id "+projectId+" not found");


        ArrayList<Attachment> attachments = new ArrayList<>();
        if(files.isPresent()){
            for(MultipartFile file : files.get()){
                storeRESTClient.storeFile(file.getBytes(), request.getArchiveId(), file.getOriginalFilename());
                attachments.add(new Attachment(file.getOriginalFilename(), FileUtils.extension(file.getOriginalFilename()),new Long(file.getSize()+""), request.getArchiveId()+"/stage1"));
            }
        }
        User user = userService.getByField("user_email",(String) authentication.getPrincipal());

        request.setUser(user);
        request.setProjectId(projectId);
        request.setRequesterPosition(requesterPosition);


        Stage1 stage1 = new Stage1(LocalDate.now().toEpochDay()+"", subject, supplier, supplierSelectionMethod, amount, amount);
        stage1.setAttachments(attachments);
        request.setStage1(stage1);

        request.setRequestStatus(Request.RequestStatus.PENDING);

        if(!destination.isEmpty()){
            Trip trip = new Trip();
            trip.setDestination(destination);
            trip.setEmail((email.isEmpty() ? user.getEmail() : email));
            trip.setFirstname((firstName.isEmpty() ? user.getFirstname() : firstName));
            trip.setLastname((lastName.isEmpty() ? user.getLastname() : lastName));
            request.setTrip(trip);
            if(!email.isEmpty())
                emailReceivers.add(email);
        }

//        emailReceivers.add(project.getScientificCoordinator().getEmail());
        emailReceivers.add(user.getEmail());

        request.setCurrentStage(Stages.Stage2.name());

        request = super.add(request, authentication);

        mailService.sendMail("Initial", emailReceivers);

        return request;
    }

    public String getMaxID() {

        FacetFilter filter = new FacetFilter();
        filter.setResourceType("request");
        filter.setKeyword("");
        filter.setFrom(0);
        filter.setQuantity(1);

        Map<String,Object> sort = new HashMap<>();
        Map<String,Object> order = new HashMap<>();

        String orderDirection = "desc";
        String orderField = "request_id";

        if (orderField != null) {
            order.put("order",orderDirection);
            sort.put(orderField, order);
            filter.setOrderBy(sort);
        }

        try {
            List rs = searchService.search(filter).getResults();
            Resource request;
            if(rs.size() > 0) {
                request = (Resource) rs.get(0);
                return parserPool.deserialize(request,Request.class).getId();
            }
        } catch (IOException e) {
            logger.debug("Error on search controller",e);
        }
        return null;
    }


    public Paging<RequestSummary> criteriaSearch(int from, int quantity,
                                                 List<BaseInfo.Status> status, List<Request.Type> types, String searchField,
                                                 List<Integer> stages, OrderByType orderType,
                                                 OrderByField orderField) {

        //TODO prepare statement for stages

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String roles = "";
        for(GrantedAuthority grantedAuthority : authentication.getAuthorities()){
            roles = roles.concat(" or acl_sid.sid='"+grantedAuthority.getAuthority()+"'");
        }
        String aclEntriesQuery = "SELECT object_id_identity, canEdit FROM acl_object_identity INNER JOIN (select distinct acl_object_identity, CASE WHEN mask=256 THEN true ELSE false END AS canEdit from acl_entry INNER JOIN acl_sid ON acl_sid.id=acl_entry.sid where acl_sid.sid='"+authentication.getPrincipal()+"' "+roles+") as acl_entries ON acl_entries.acl_object_identity=acl_object_identity.id";


        String viewQuery = "select acls.canEdit as canEdit, request_view.creation_date as creation_date, project_view.project_scientificcoordinator as scientificCoordinator, request_view.request_type as type, approval_view.status as approval_status, payment_view.status as payment_status, request_view.request_id as request_id, approval_view.stage as approval_stage, payment_view.stage as payment_stage, project_view.project_operator as operator, project_view.project_acronym as acronym, institute_view.institute_name as institute from request_view inner join project_view on request_project=project_view.project_id inner join institute_view on institute_view.institute_id=project_view.project_institute left join approval_view on approval_view.request_id=request_view.request_id left join payment_view on payment_view.request_id=request_view.request_id";
        viewQuery+=" inner join (" + aclEntriesQuery+") as acls on acls.object_id_identity=request_view.request_id";

        viewQuery+= " where (approval_view.status in ("+status.stream().map(p -> "'"+p.toString()+"'").collect(Collectors.joining(","))+") or payment_view.status in ("+status.stream().map(p -> "'"+p.toString()+"'").collect(Collectors.joining(","))+")) and request_view.request_type in ("+types.stream().map(p -> "'"+p.toString()+"'").collect(Collectors.joining(","))+") and (approval_view.stage in ("+stages.stream().map(p -> "'"+p+"'").collect(Collectors.joining(","))+") or payment_view.stage in ("+stages.stream().map(p -> "'"+p+"'").collect(Collectors.joining(","))+")) "+(!searchField.isEmpty() ? "and ( project_view.project_scientificcoordinator=? or project_view.project_operator=? or request_view.request_id=? or project_view.project_acronym=? or institute_view.institute_name=? )" : "")+" order by "+orderField+" "  +  orderType + " offset ? limit ?";


        return new JdbcTemplate(dataSource).query(viewQuery, ps -> {
            if(!searchField.isEmpty()) {
                ps.setString(1, searchField);
                ps.setString(2, searchField);
                ps.setString(3, searchField);
                ps.setString(4, searchField);
                ps.setString(5, searchField);
                ps.setInt(6, from);
                ps.setInt(7, quantity);
            }else{
                ps.setInt(1, from);
                ps.setInt(2, quantity);
            }
        }, rs -> {
            List<RequestSummary> results = new ArrayList<>();
            while(rs.next()){
                BaseInfo baseInfo = new BaseInfo();
                if(rs.getString("approval_status") !=null && !rs.getString("approval_status").isEmpty())
                    baseInfo.setStatus(BaseInfo.Status.valueOf(rs.getString("approval_status")));
                if(rs.getString("payment_status") !=null && !rs.getString("payment_status").isEmpty())
                    baseInfo.setStatus(BaseInfo.Status.valueOf(rs.getString("payment_status")));

                if(rs.getString("approval_stage") !=null && !rs.getString("approval_stage").isEmpty())
                    baseInfo.setStage(rs.getString("approval_stage"));
                if(rs.getString("payment_stage") !=null && !rs.getString("payment_stage").isEmpty())
                    baseInfo.setStage(rs.getString("payment_stage"));

                Request request = get(rs.getString("request_id"));
                Project project = projectService.get(request.getProjectId());
                Institute institute = instituteService.get(project.getInstituteId());
                Organization organization = organizationService.get(institute.getId());


                baseInfo.setCreationDate(rs.getString("creation_date"));
                baseInfo.setRequestId(request.getId());
                RequestSummary requestSummary = new RequestSummary();

                requestSummary.setBaseInfo(baseInfo);
                requestSummary.setRequestId(request.getId());
                requestSummary.setCanEdit(rs.getBoolean("canedit"));
                requestSummary.setRequestFullName(request.getUser().getFirstname() + " " + request.getUser().getLastname());
                requestSummary.setRequestType(request.getType().toString());
                requestSummary.setProjectAcronym(project.getAcronym());
                requestSummary.setInstituteName(institute.getName());


                results.add(requestSummary);
            }
            return new Paging<>(results.size(),from, from + results.size(), results, new ArrayList<>());
        });
    }

    public List<Request> getPendingRequests(String email) {

        //language=SQL
        String whereClause = " (  ( r.request_project_operator <@ '{"+'"' + email + '"' + "} or  request_project_operator_delegate <@ '{"+'"' + email + '"' + "}')"
                           + "      and ( request_stage = 3 ) "
                           + "    ) "
                           + " or ( ( request_institute_suppliesOffice = '" + email + "' or request_institute_suppliesOffice_delegate <@ '{"+'"' + email + '"' + "}')" +
                                    "and request_stage = 7 and request_type != trip ) "
                           + " or ( request_institute_travelManager = '" + email + "' or request_institute_travelManager_delegate <@ '{"+'"' + email + '"' + "}')" +
                                    "' and request_stage = 7 and request_type = trip ) "
                           + " or ( request_project_scientificCoordinator = '" + email + "' and request_stage = 2 ) "
                           + " or ( ( request_organization_poy =  '" + email + "' or  request_organization_poy_delegate = "  + email + " ) "
                           + "      and ( request_stage = 4 or request_stage = 9 ) "
                           + "    ) "
                           + " or ( ( request_institute_director =  " + email + " or  request_institute_director_delegate <@ '{"+'"' + email + '"' + "}')"
                           + "      and ( request_stage = 5a or request_stage = 10 ) "
                           + "    ) "
                           + " or ( ( request_organization_dioikitikoSumvoulio =  '" + email + "' or  request_organization_dioikitikoSumvoulio_delegate <@ '{"+'"' + email + '"' + "}')"
                           + "      and request_stage = 5b "
                           + "    ) "
                           + " or ( ( request_institute_diaugeia =  '" + email + "' or  request_institute_diaugeia_delegate <@ '{"+'"' + email + '"' + "}')"
                           + "      and ( request_stage = 6 or request_stage = 11 ) "
                           + "    ) "
                           + " or ( ( request_organization_inspectionTeam <@ '{"+'"' + email + '"' + "} or  request_organization_inspectionTeam_delegate <@ '{"+'"' + email + '"' + "}')"
                           + "      and request_stage = 8 "
                           + "    ) "
                           + " or ( ( request_institute_accountingRegistration =  '" + email + "' or  request_institute_accountingRegistration_delegate <@ '{"+'"' + email + '"' + "}')"
                           + "      and request_stage = 12 "
                           + "    ) "
                           + " or ( ( request_institute_accountingPayment =  '" + email + "' or  request_institute_accountingPayment_delegate <@ '{"+'"' + email + '"' + "}')"
                           + "      and request_stage = 13 "
                           + "    ) "
                            + " or (  request_requester =  '" + email + "' and  request_stage = 7 )";

        logger.info(whereClause);


        Paging<Resource> rs = searchService.cqlQuery(
                whereClause,"request",
                1000,0,"", "ASC");


        List<Request> resultSet = new ArrayList<>();
        for(Resource resource:rs.getResults()) {
            resultSet.add(parserPool.deserialize(resource,typeParameterClass));
        }
        return resultSet;

    }

    public String createArchive() {
        return storeRESTClient.createArchive().getResponse();
    }

    public ResponseEntity<Object> upLoadFile(String mode,String archiveID,
                                             String stage, MultipartFile file) {


        if(!mode.equals("request"))
            archiveID += "/"+mode;

        String fileName = stage;
        if(Boolean.parseBoolean(storeRESTClient.fileExistsInArchive(archiveID,fileName).getResponse()))
            storeRESTClient.deleteFile(archiveID,fileName);

        try {
            storeRESTClient.storeFile(file.getBytes(),archiveID,fileName);
        } catch (IOException e) {
            logger.info(e);
            return new ResponseEntity<>("ERROR",HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(archiveID+"/"+fileName,HttpStatus.OK);

    }

}
