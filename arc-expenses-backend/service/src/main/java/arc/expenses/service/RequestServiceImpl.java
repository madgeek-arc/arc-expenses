package arc.expenses.service;

import arc.expenses.acl.ArcPermission;
import arc.expenses.domain.OrderByField;
import arc.expenses.domain.OrderByType;
import arc.expenses.domain.RequestSummary;
import arc.expenses.domain.Stages;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.domain.AclImpl;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.AlreadyExistsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service("requestService")
public class RequestServiceImpl extends GenericService<Request> {

    private static Logger logger = LogManager.getLogger(RequestServiceImpl.class);

    @Autowired
    private StoreRESTClient storeRESTClient;

    @Autowired
    private RequestApprovalServiceImpl requestApprovalService;

    @Autowired
    private ProjectServiceImpl projectService;

    @Autowired
    private InstituteServiceImpl instituteService;

    @Autowired
    private OrganizationServiceImpl organizationService;

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MailService mailService;

    @Autowired
    private AclService aclService;

    @Autowired
    private RequestPaymentServiceImpl requestPaymentService;

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


    @Override
    public Request get(String id) {
        return super.get(id);
    }

    public Request add(Request.Type type, String projectId, String subject, Request.RequesterPosition requesterPosition, String supplier, Stage1.SupplierSelectionMethod supplierSelectionMethod, double amount, Optional<List<MultipartFile>> files, String destination, String firstName, String lastName, String email, int cycles, boolean onBehalf) throws Exception {

        if((type == Request.Type.REGULAR || type == Request.Type.SERVICES_CONTRACT) && supplierSelectionMethod ==null)
            throw new ServiceException("Supplier selection method cannot be empty");

        if(type == Request.Type.REGULAR || type == Request.Type.SERVICES_CONTRACT) {
            if (supplierSelectionMethod != Stage1.SupplierSelectionMethod.AWARD_PROCEDURE && supplier.isEmpty())
                throw new ServiceException("Supplier cannot be empty");

            if (((supplierSelectionMethod == Stage1.SupplierSelectionMethod.AWARD_PROCEDURE || supplierSelectionMethod == Stage1.SupplierSelectionMethod.MARKET_RESEARCH) || amount > 2500) && !files.isPresent())
                throw new ServiceException("Files must be included");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Request request = new Request();
        request.setType(type);
        request.setId(generateID());
        request.setArchiveId(createArchive());
        request.setPaymentCycles(cycles);


        Project project = projectService.get(projectId);
        if(project == null)
            throw new ServiceException("Project with id "+projectId+" not found");
        Institute institute = instituteService.get(project.getInstituteId());
        if(institute == null)
            throw new ServiceException("Institute with id "+ project.getInstituteId()+ " not found");

        Organization organization = organizationService.get(institute.getOrganizationId());
        if(organization == null)
            throw new ServiceException("Organization with id "+ institute.getOrganizationId()+ " not found");

        User user = userService.getByField("user_email",(String) authentication.getPrincipal());

        List<String> pois = new ArrayList<>();

        if(onBehalf) {
            request.setOnBehalfOf(new PersonOfInterest(email,firstName,lastName,new ArrayList<>()));
            pois.add(email);
        }

        request.setUser(user);
        request.setProjectId(projectId);
        request.setRequesterPosition(requesterPosition);
        request.setDiataktis(institute.getDiataktis());

        ArrayList<Attachment> attachments = new ArrayList<>();
        if(files.isPresent()){
            for(MultipartFile file : files.get()){
                storeRESTClient.storeFile(file.getBytes(), request.getArchiveId()+"/stage1", file.getOriginalFilename());
                attachments.add(new Attachment(file.getOriginalFilename(), FileUtils.extension(file.getOriginalFilename()), file.getSize(), request.getArchiveId()+"/stage1"));
            }
        }

        request.setRequestStatus(Request.RequestStatus.PENDING);

        if(!destination.isEmpty()) {
            Trip trip = new Trip();
            trip.setDestination(destination);
            trip.setEmail((email.isEmpty() ? user.getEmail() : email));
            trip.setFirstname((firstName.isEmpty() ? user.getFirstname() : firstName));
            trip.setLastname((lastName.isEmpty() ? user.getLastname() : lastName));
            request.setTrip(trip);
            if (!email.isEmpty()) {
                if (!pois.contains(email))
                    pois.add(email);
            }
        }

        if(!pois.contains(project.getScientificCoordinator().getEmail()))
            pois.add(project.getScientificCoordinator().getEmail());

        if(!pois.contains(user.getEmail()))
            pois.add(user.getEmail());

        for(Delegate delegate : project.getScientificCoordinator().getDelegates())
            if(!pois.contains(delegate.getEmail()))
                pois.add(delegate.getEmail());

        request.setCurrentStage(Stages.Stage2.name());
        request.setPois(pois);
        request.setFinalAmount(amount);


        request = super.add(request, authentication);


        Stage1 stage1 = new Stage1(new Date().toInstant().toEpochMilli()+"", amount, subject, supplier, supplierSelectionMethod, amount);
        stage1.setAttachments(attachments);

        RequestApproval requestApproval = createRequestApproval(request);
        requestApproval.setCurrentStage(Stages.Stage2.name());
        requestApproval.setStage1(stage1);

        requestApprovalService.update(requestApproval,requestApproval.getId());
        mailService.sendMail("Initial", request.getPois());

        return request;
    }


    private RequestApproval createRequestApproval(Request request) {
        logger.debug("Request with id " + request.getId() + " has just been created");

        RequestApproval requestApproval = new RequestApproval();
        requestApproval.setId(request.getId()+"-a1");
        requestApproval.setRequestId(request.getId());
        requestApproval.setCreationDate(new Date().toInstant().toEpochMilli());
        requestApproval.setStage("2");
        requestApproval.setStatus(BaseInfo.Status.PENDING);

        requestApproval = requestApprovalService.add(requestApproval, null);

        try{
            aclService.createAcl(new ObjectIdentityImpl(RequestApproval.class, requestApproval.getId()));
        }catch (AlreadyExistsException ex){
            logger.debug("Object identity already exists");
        }
        Project project = projectService.get(request.getProjectId());
        AclImpl acl = (AclImpl) aclService.readAclById(new ObjectIdentityImpl(RequestApproval.class, requestApproval.getId()));
        acl.insertAce(acl.getEntries().size(), ArcPermission.CANCEL, new PrincipalSid(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString()), true);
        acl.insertAce(acl.getEntries().size(), ArcPermission.CANCEL, new GrantedAuthoritySid("ROLE_ADMIN"), true);

        acl.insertAce(acl.getEntries().size(), ArcPermission.READ, new GrantedAuthoritySid("ROLE_ADMIN"), true);
        acl.insertAce(acl.getEntries().size(), ArcPermission.READ, new PrincipalSid(project.getScientificCoordinator().getEmail()), true);
        acl.insertAce(acl.getEntries().size(), ArcPermission.READ, new PrincipalSid(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString()), true);
        if(request.getOnBehalfOf()!=null)
            acl.insertAce(acl.getEntries().size(), ArcPermission.READ, new PrincipalSid(request.getOnBehalfOf().getEmail()), true);

        acl.insertAce(acl.getEntries().size(), ArcPermission.WRITE, new GrantedAuthoritySid("ROLE_ADMIN"), true);
        acl.insertAce(acl.getEntries().size(), ArcPermission.WRITE, new PrincipalSid(project.getScientificCoordinator().getEmail()), true);
        acl.insertAce(acl.getEntries().size(), ArcPermission.WRITE, new PrincipalSid(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString()), true);

        acl.insertAce(acl.getEntries().size(), ArcPermission.EDIT, new PrincipalSid(project.getScientificCoordinator().getEmail()), true);
        for(Delegate person : project.getScientificCoordinator().getDelegates())
            acl.insertAce(acl.getEntries().size(), ArcPermission.EDIT, new PrincipalSid(person.getEmail()), true);

        acl.setOwner(new GrantedAuthoritySid(("ROLE_EXECUTIVE")));
        aclService.updateAcl(acl);


        return requestApproval;
    }


    public boolean exceedsProjectBudget(PersonOfInterest scientificCoordinator, String projectId, Double amount){

        String budgetQuery = "select CASE WHEN sum(request_final_amount) + "+amount+" >(0.25 * project_view.project_total_cost) THEN false ELSE true END AS canBeDiataktis from request_view INNER JOIN project_view ON project_view.project_id=request_view.request_project WHERE request_view.request_diataktis='"+scientificCoordinator.getEmail()+"' AND project_view.project_id='"+projectId+"' GROUP BY project_view.project_total_cost;";

        return new JdbcTemplate(dataSource).query(budgetQuery , rs -> {
            if(rs.next())
                return rs.getBoolean("canbediataktis");
            else
                return false;
        });
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
                                                 List<String> stages, OrderByType orderType,
                                                 OrderByField orderField,
                                                 boolean canEdit) {

        //TODO prepare statement for stages

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String roles = "";
        for(GrantedAuthority grantedAuthority : authentication.getAuthorities()){
            roles = roles.concat(" or acl_sid.sid='"+grantedAuthority.getAuthority()+"'");
        }
        String aclEntriesQuery = "SELECT acl_class.class as class, object_id_identity, canEdit FROM acl_object_identity INNER JOIN (select distinct on(acl_object_identity) acl_object_identity, CASE WHEN mask=32 THEN true ELSE false END AS canEdit from acl_entry INNER JOIN acl_sid ON acl_sid.id=acl_entry.sid where acl_sid.sid='"+authentication.getPrincipal()+"' "+roles+" ORDER BY acl_object_identity,canedit DESC ) as acl_entries ON acl_entries.acl_object_identity=acl_object_identity.id INNER JOIN acl_class ON acl_class.id=acl_object_identity.object_id_class";


        String viewQuery = "select acls.class, acls.canEdit as canEdit, request_view.creation_date as creation_date, project_view.project_scientificcoordinator as scientificCoordinator, request_view.request_type as type, approval_view.status as approval_status, payment_view.status as payment_status, request_view.request_id as request_id, approval_view.stage as approval_stage, payment_view.stage as payment_stage, project_view.project_operator as operator, project_view.project_acronym as acronym, institute_view.institute_name as institute, approval_view.approval_id as approval_id, CASE WHEN acls.class='gr.athenarc.domain.RequestApproval' THEN approval_view.approval_id ELSE payment_view.payment_id END AS baseinfo_id from request_view inner join project_view on request_project=project_view.project_id inner join institute_view on institute_view.institute_id=project_view.project_institute left join approval_view on approval_view.request_id=request_view.request_id left join payment_view on payment_view.request_id=request_view.request_id";
        viewQuery+=" inner join (" + aclEntriesQuery+") as acls on acls.object_id_identity=approval_view.approval_id or acls.object_id_identity=payment_view.payment_id ";

        viewQuery+= " where (approval_view.status in ("+status.stream().map(p -> "'"+p.toString()+"'").collect(Collectors.joining(","))+") or payment_view.status in ("+status.stream().map(p -> "'"+p.toString()+"'").collect(Collectors.joining(","))+")) and request_view.request_type in ("+types.stream().map(p -> "'"+p.toString()+"'").collect(Collectors.joining(","))+") "+(canEdit ? "and canEdit=true" : "" )+" and (approval_view.stage in ("+stages.stream().map(p -> "'"+p+"'").collect(Collectors.joining(","))+") or payment_view.stage in ("+stages.stream().map(p -> "'"+p+"'").collect(Collectors.joining(","))+")) "+(!searchField.isEmpty() ? "and ( project_view.project_scientificcoordinator=? or project_view.project_operator=? or request_view.request_id=? or project_view.project_acronym=? or institute_view.institute_name=? )" : "")+" order by "+orderField+" "  +  orderType + " offset ? limit ?";

        System.out.println(viewQuery);

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
                if(rs.getString("approval_status") !=null && !rs.getString("approval_status").isEmpty() && rs.getString("class").equals("gr.athenarc.domain.RequestApproval"))
                    baseInfo.setStatus(BaseInfo.Status.valueOf(rs.getString("approval_status")));
                if(rs.getString("payment_status") !=null && !rs.getString("payment_status").isEmpty() && rs.getString("class").equals("gr.athenarc.domain.RequestPayment"))
                    baseInfo.setStatus(BaseInfo.Status.valueOf(rs.getString("payment_status")));

                if(rs.getString("approval_stage") !=null && !rs.getString("approval_stage").isEmpty())
                    baseInfo.setStage(rs.getString("approval_stage"));
                if(rs.getString("payment_stage") !=null && !rs.getString("payment_stage").isEmpty())
                    baseInfo.setStage(rs.getString("payment_stage"));

                Request request = get(rs.getString("request_id"));
                Project project = projectService.get(request.getProjectId());
                Institute institute = instituteService.get(project.getInstituteId());

                baseInfo.setId(rs.getString("baseinfo_id"));

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                try {
                    baseInfo.setCreationDate(sdf.parse(rs.getString("creation_date")).getTime());
                } catch (ParseException e) {
                    logger.warn("Failed to parse creation date from sql query");
                }
                baseInfo.setRequestId(request.getId());
                RequestSummary requestSummary = new RequestSummary();

                requestSummary.setBaseInfo(baseInfo);
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

    @PreAuthorize("hasPermission(#request,'READ')")
    public File downloadFile(File file,Request request,String url) {
        try {
            storeRESTClient.downloadFile(url, file.getAbsolutePath());
            return file;
        } catch (Exception e) {
            logger.error("error downloading file", e);
        }
        return null;
    }




}
