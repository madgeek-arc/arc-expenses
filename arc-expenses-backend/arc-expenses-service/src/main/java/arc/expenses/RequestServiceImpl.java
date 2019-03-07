package arc.expenses;

import arc.expenses.config.StoreRestConfig;
import arc.expenses.domain.RequestSummary;
import arc.expenses.utils.Converter;
import com.sun.org.apache.xpath.internal.operations.Mult;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.store.restclient.StoreRESTClient;
import gr.athenarc.domain.*;
import org.apache.log4j.Logger;
import org.javatuples.Septet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service("requestService")
public class RequestServiceImpl extends GenericService<Request> {

    @Autowired
    private StoreRESTClient storeRESTClient;

    @Autowired
    private StoreRestConfig storeRestConfig;

    @Autowired
    RequestApprovalServiceImpl requestApprovalService;

    @Autowired
    RequestPaymentServiceImpl requestPaymentService;

    @Autowired
    DataSource dataSource;

    @Value("#{'${admin.emails}'.split(',')}")
    private List<String> admins;


    private Logger LOGGER = Logger.getLogger(RequestServiceImpl.class);

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


    public String getMaxID() {
        return new JdbcTemplate(dataSource)
                .query("select r.request_id as maxID\n" +
                        "from request_view r , resource res\n" +
                        "where fk_name = 'request' and r.id = res.id\n" +
                        "order by r.creation_date desc\n" +
                        "limit 1",maxIDRowMapper).get(0);
    }

    private RowMapper<String> maxIDRowMapper = (rs, i) -> rs.getString("maxID");


    public Paging<RequestSummary> criteriaSearch(String from, String quantity,
                                 List<String> status,List<String> type, String searchField,
                                 List<String> stage, String orderType,
                                 String orderField, String email) {

        String query =  "select request_id,id,creation_date,request_project_acronym,request_institute,request_stage , count(*) over () as total_rows from (       ( select distinct(r.request_id) as request_id ,a.approval_id as id,(res2.payload::json)->'stage1'->>'requestDate' as creation_date,         r.request_project_acronym as request_project_acronym , r.request_institute as request_institute , a.stage as request_stage , r.request_type as request_type        from request_view r , approval_view a , resource res1, resource res2           where r.request_id = a.request_id  and res1.fk_name = 'approval'          and a.id  = res1.id AND r.id = res2.id          AND res2.fk_name = 'request' " ;



        StringBuilder searchField_clause = this.getSearchFieldClause(searchField);
        if(searchField_clause.length()!=0)
            query+= "  and  " + searchField_clause.toString();


        StringBuilder user_clause = this.getUserClause(email);
        if(user_clause.length()!=0)
            query+= " and " + user_clause.toString();

        StringBuilder stage_clause = this.getStageClause("a",stage);
        if( stage_clause.length()!=0)
            query+= " and  " + stage_clause.toString();

        StringBuilder status_clause = this.getStatusClause("a",status);
        if(status_clause.length()!=0)
            query+= " and " + status_clause.toString();

        StringBuilder type_clause = this.getTypeClause(type);
        if(type_clause.length()!=0)
            query+= " and " + type_clause.toString();


        query+=" )" +
               " union " +
               " ( select distinct(r.request_id) as request_id,p.payment_id as id,(res2.payload::json)->'stage1'->>'requestDate' as creation_date ," +
               " r.request_project_acronym as request_project_acronym , r.request_institute as request_institute , p.stage as request_stage , r.request_type as request_type" +
               " from request_view r , payment_view p , resource res1, resource res2   " +
               " where r.request_id = p.request_id  and res1.fk_name = 'payment' " +
               " and p.id  = res1.id AND r.id = res2.id "+
                " AND res2.fk_name = 'request' " ;

        searchField_clause = this.getSearchFieldClause(searchField);
        if(searchField_clause.length()!=0)
            query+= "  and  " + searchField_clause.toString();

        user_clause = this.getUserClause(email);
        if(user_clause.length()!=0)
            query+= " and " + user_clause.toString();

        stage_clause = this.getStageClause("p",stage);
        if( stage_clause.length()!=0)
            query+= " and  " + stage_clause.toString();

        status_clause = this.getStatusClause("p",status);
        if(status_clause.length()!=0)
            query+= " and " + status_clause.toString();

        type_clause = this.getTypeClause(type);
        if(type_clause.length()!=0)
            query+= " and " + type_clause.toString();

        query += ")) as foo ";


        query +=  "  order by "+orderField + " "  + orderType;
        query +=  "  limit " + quantity +
                  "  offset " + from;

        LOGGER.info(query);
        List<Septet<String,String,String,String,String,String,String>> resultSet =  new JdbcTemplate(dataSource)
                .query(query,requestSummaryMapper);

        List<RequestSummary> rs = new ArrayList<>();
        int total = 0;
        for(Septet<String,String,String,String,String,String,String> septet : resultSet){
            RequestSummary requestSummary = new RequestSummary();
            total = Integer.parseInt(septet.getValue6());
            if(septet.getValue1().contains("a"))
                requestSummary.setBaseInfo(Converter.toBaseInfo(requestApprovalService.get(septet.getValue1())));
            else
                requestSummary.setBaseInfo(Converter.toBaseInfo(requestPaymentService.get(septet.getValue1())));

            requestSummary.setRequest(get(requestSummary.getBaseInfo().getRequestId()));
            rs.add(requestSummary);
        }

        return new Paging<>(total,Integer.parseInt(from),Integer.parseInt(quantity),rs,null);
    }

    private StringBuilder getTypeClause(List<String> type) {

        StringBuilder status_clause = new StringBuilder();
        StringBuilder in_clause = new StringBuilder();

        if(!type.get(0).equals("all")){
            status_clause.append("r.request_type in ");
            in_clause.append("(");
            for(int i=0;i<type.size();i++)
                in_clause.append("'").append(type.get(i)).append("'").append(",");
            in_clause.deleteCharAt(in_clause.length()-1);
            in_clause.append(")");

            status_clause.append(in_clause.toString());
        }
        return status_clause;

    }

    private StringBuilder getSearchFieldClause(String searchField) {

        StringBuilder searchField_clause = new StringBuilder();

        if(searchField!=null && !searchField.equals(""))
            searchField_clause.append( "( (res2.payload::json->>'user')::text ilike '%")
                                .append(searchField).append("%'")
                                .append( " or  ")
                                .append( "( r.request_project ilike '%")
                                .append(searchField).append("%')")
                                .append( " or  ")
                                .append( "( r.request_institute ilike '%")
                                .append(searchField).append("%')")
                                .append( " or  ")
                                .append( "( r.request_id ilike '%")
                                .append(searchField).append("%')")
                                .append( " or  ")
                                .append( "( ((res2.payload::json)->'project'->>'operator')::text ilike '%")
                                .append(searchField).append("%')")
                                .append( " or  ")
                                .append( "( ((res2.payload::json)->'project'->>'scientificCoordinator')::text ilike '%")
                                .append(searchField).append("%') )");

        return searchField_clause;
    }

    private RowMapper<Septet<String,String,String,String,String,String,String>> requestSummaryMapper = (rs, i) ->
            Septet.with(rs.getString("request_id"),
                rs.getString("id"),
                rs.getString("creation_date"),
                rs.getString("request_project_acronym"),
                rs.getString("request_institute"),
                rs.getString("request_stage"),
                rs.getString("total_rows"));

    private StringBuilder getStatusClause(String table,List<String> status) {
        StringBuilder status_clause = new StringBuilder();
        StringBuilder in_clause = new StringBuilder();

        if(!status.get(0).equals("all")){
            status_clause.append(table).append(".status in ");

            in_clause.append("(");
            for(int i=0;i<status.size();i++){
                if(status.get(i).equals("'pending'"))
                    in_clause.append(" 'under_review', ");
                in_clause.append("'").append(status.get(i)).append("'").append(",");
            }
            in_clause.deleteCharAt(in_clause.length()-1);
            in_clause.append(")");

            status_clause.append(in_clause.toString());
        }
        return status_clause;
    }

    private StringBuilder getStageClause(String table,List<String> stage) {

        StringBuilder stage_clause = new StringBuilder();
        StringBuilder in_clause = new StringBuilder();

        if(!stage.get(0).equals("all")){
            stage_clause.append(table).append(".stage in ");
            in_clause.append("(");
            for(int i=0;i<stage.size();i++)
                in_clause.append("'").append(stage.get(i)).append("'").append(",");
            in_clause.deleteCharAt(in_clause.length()-1);
            in_clause.append(")");

            stage_clause.append(in_clause.toString());
        }
        return stage_clause;
    }

    private StringBuilder getUserClause(String email) {

        StringBuilder user_clause = new StringBuilder();
        String requestOrganizationDirector = getOrganizationDirector();
        String diataktis = getDiataktis();

        if(!admins.contains(email)) {
            user_clause.append(" ( lower(r.request_requester) = '"  + email.toLowerCase() + "' or " +
                    " r.request_project_operator @> '{"+'"' + email.toLowerCase() + '"' + "}' or " +
                    " r.request_organization_inspectionteam @> '{"+'"' + email.toLowerCase() + '"' + "}' or " +
                    " r.request_organization_inspectionteam_delegate @> '{"+'"' + email.toLowerCase() + '"' + "}' or " +
                    " r.request_project_operator_delegate @> '{"+'"' + email.toLowerCase() + '"' + "}' or " +
                    " lower(r.request_project_scientificCoordinator) = '"  + email.toLowerCase() + "' or " +
                    " lower(r.request_institute_travelmanager) = '"  + email.toLowerCase() + "' or " +
                    " lower(r.request_organization_poy) = '"  + email.toLowerCase() + "' or " +
                    " lower(r.request_organization_dioikitikoSumvoulio) = '"  + email.toLowerCase() + "' or " +
                    " r.request_organization_dioikitikoSumvoulio_delegate @>  '{"+'"' + email.toLowerCase() + '"' + "}' or " +
                    " r.request_organization_poy_delegate @>  '{"+'"' + email.toLowerCase() + '"' + "}' or " +
                    " r.request_institute_travelmanager_delegate @>  '{"+'"' + email.toLowerCase() + '"' + "}' or " +
                    " lower(r.request_institute_accountingRegistration) = '"  + email.toLowerCase() + "' or " +
                    " lower(r.request_institute_diaugeia) = '"  + email.toLowerCase() + "' or " +
                    " lower(r.request_institute_accountingPayment) = '"  + email.toLowerCase() + "' or " +
                    " lower(r.request_institute_accountingDirector) = '"  + email.toLowerCase() + "' or " +
                    " r.request_institute_accountingDirector_delegate @>  '{"+'"' + email.toLowerCase() + '"' + "}' or " +
                    " r.request_institute_accountingRegistration_delegate @>  '{"+'"' + email.toLowerCase() + '"' + "}' or " +
                    " r.request_institute_accountingPayment_delegate @>  '{"+'"' + email.toLowerCase() + '"' + "}' or " +
                    " r.request_institute_diaugeia_delegate @>  '{"+'"' + email.toLowerCase() + '"' + "}' or " +
                    " lower(r.request_organization_director) = '"  + email.toLowerCase() + "' or " +
                    " lower(r.request_institute_director) = '"  + email.toLowerCase() + "' or " +
                    " (res2.payload::json)->'trip'->>'email' = '" +  email.toLowerCase()  + "' or " +
                    " r.request_organization_director_delegate @>  '{"+'"' + email.toLowerCase() + '"' + "}' or " +
                    " r.request_institute_director_delegate @>  '{"+'"' + email.toLowerCase() + '"' + "}' or " +
                    " ( (r.request_requester in (" + diataktis + ") or (res2.payload::json)->'trip'->>'email' in (" +diataktis +") " +
                    "   and not(r.request_requester = '"+ requestOrganizationDirector + "' or (res2.payload::json)->'trip'->>'email' = '" +  requestOrganizationDirector  + "')" +
                    "   and r.request_organization_director = '"  + email.toLowerCase() + "' ))" +
                    " or ( (r.request_requester in (" + diataktis + ") or (res2.payload::json)->'trip'->>'email' in (" + diataktis +") " +
                    "   and (r.request_requester = '"+ requestOrganizationDirector + "' or (res2.payload::json)->'trip'->>'email' = '" +  requestOrganizationDirector  + "')" +
                    "   and r.request_organization_vicedirector = '"  + email.toLowerCase() + "' )))");
        }
        return user_clause;
    }

    private String getDiataktis() {
        List<String> rs =  new JdbcTemplate(dataSource)
                .query("select distinct(request_institute_diataktis) rd from request_view ;",rowMapper);

        StringBuilder result = new StringBuilder();
        for(int i=0;i<rs.size();i++)
            result.append("'").append(rs.get(i)).append("'").append(",");
        result.deleteCharAt(result.length()-1);
        return result.toString();
    }

    private String getOrganizationDirector() {
        return new JdbcTemplate(dataSource)
                .query("select request_organization_director as rd from request_view limit 1;",rowMapper).get(0);
    }

    private RowMapper<String> rowMapper = (rs, i) -> rs.getString("rd");



    public List<Request> getPendingRequests(String email) {

        //language=SQL
        String whereClause = " (  ( r.request_project_operator <@ '{"+'"' + email.toLowerCase() + '"' + "} or  request_project_operator_delegate <@ '{"+'"' + email.toLowerCase() + '"' + "}')"
                           + "      and ( request_stage = 3 ) "
                           + "    ) "
                           + " or ( ( request_institute_suppliesOffice = '" + email.toLowerCase() + "' or request_institute_suppliesOffice_delegate <@ '{"+'"' + email.toLowerCase() + '"' + "}')" +
                                    "and request_stage = 7 and request_type != trip ) "
                           + " or ( request_institute_travelManager = '" + email.toLowerCase() + "' or request_institute_travelManager_delegate <@ '{"+'"' + email.toLowerCase() + '"' + "}')" +
                                    "' and request_stage = 7 and request_type = trip ) "
                           + " or ( request_project_scientificCoordinator = '" + email.toLowerCase() + "' and request_stage = 2 ) "
                           + " or ( ( request_organization_poy =  '" + email.toLowerCase() + "' or  request_organization_poy_delegate = "  + email.toLowerCase() + " ) "
                           + "      and ( request_stage = 4 or request_stage = 9 ) "
                           + "    ) "
                           + " or ( ( request_institute_director =  " + email.toLowerCase() + " or  request_institute_director_delegate <@ '{"+'"' + email.toLowerCase() + '"' + "}')"
                           + "      and ( request_stage = 5a or request_stage = 10 ) "
                           + "    ) "
                           + " or ( ( request_organization_dioikitikoSumvoulio =  '" + email.toLowerCase() + "' or  request_organization_dioikitikoSumvoulio_delegate <@ '{"+'"' + email.toLowerCase() + '"' + "}')"
                           + "      and request_stage = 5b "
                           + "    ) "
                           + " or ( ( request_institute_diaugeia =  '" + email.toLowerCase() + "' or  request_institute_diaugeia_delegate <@ '{"+'"' + email.toLowerCase() + '"' + "}')"
                           + "      and ( request_stage = 6 or request_stage = 11 ) "
                           + "    ) "
                           + " or ( ( request_organization_inspectionTeam <@ '{"+'"' + email.toLowerCase() + '"' + "} or  request_organization_inspectionTeam_delegate <@ '{"+'"' + email.toLowerCase() + '"' + "}')"
                           + "      and request_stage = 8 "
                           + "    ) "
                           + " or ( ( request_institute_accountingRegistration =  '" + email.toLowerCase() + "' or  request_institute_accountingRegistration_delegate <@ '{"+'"' + email.toLowerCase() + '"' + "}')"
                           + "      and request_stage = 12 "
                           + "    ) "
                           + " or ( ( request_institute_accountingPayment =  '" + email.toLowerCase() + "' or  request_institute_accountingPayment_delegate <@ '{"+'"' + email.toLowerCase() + '"' + "}')"
                           + "      and request_stage = 13 "
                           + "    ) "
                            + " or (  request_requester =  '" + email.toLowerCase() + "' and  request_stage = 7 )";

        LOGGER.info(whereClause);


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

    public ResponseEntity<Object> upLoadFile(String id,String mode,String archiveID,
                                             String stage, MultipartFile[] files) {

        List<String> urls = new ArrayList<>();
        String fileName;
        String url;

        if(!mode.equals("request"))
            archiveID += "/"+mode+ "/" + id + "/"+stage;


        for(MultipartFile file : files){
            fileName = file.getOriginalFilename();
            try {
                if(Boolean.parseBoolean(storeRESTClient.fileExistsInArchive(archiveID,fileName).getResponse()))
                    storeRESTClient.deleteFile(archiveID,fileName);

                url = archiveID+"/"+fileName;
                storeRESTClient.storeFile(file.getBytes(),archiveID,fileName);
            } catch (IOException e) {
                LOGGER.info(e);
                return new ResponseEntity<>("ERROR",HttpStatus.INTERNAL_SERVER_ERROR);
            }
            urls.add(url);
        }
        return new ResponseEntity<>(urls,HttpStatus.OK);
    }

    public InputStream downloadFile(Attachment attachment) {
        try {
            File temp = File.createTempFile("file", "tmp");
            temp.deleteOnExit();
            storeRESTClient.downloadFile(attachment.getUrl(), temp.getAbsolutePath());
            return new FileInputStream(temp);
        } catch (Exception e) {
            LOGGER.error("error downloading file", e);
        }
        return null;
    }

    public Attachment getAttachment(String mode ,String id, String stage,String filename) {
        Attachment attachment;

        if(mode.equals("request"))
            return getAttachmentByFilename(get(id).getStage1().getAttachments(),filename);
        else if(mode.equals("approval")){
            switch (stage) {
                case "2":
                    attachment = getAttachmentByFilename(requestApprovalService.get(id).getStage2().getAttachments(),filename);
                    break;
                case "3":
                    attachment = getAttachmentByFilename(requestApprovalService.get(id).getStage3().getAttachments(),filename);
                    break;
                case "4":
                    attachment = getAttachmentByFilename(requestApprovalService.get(id).getStage4().getAttachments(),filename);
                    break;
                case "5":
                    attachment = getAttachmentByFilename(requestApprovalService.get(id).getStage5().getAttachments(),filename);
                    break;
                case "5a":
                    attachment = getAttachmentByFilename(requestApprovalService.get(id).getStage5a().getAttachments(),filename);
                    break;
                case "5b":
                    attachment = getAttachmentByFilename(requestApprovalService.get(id).getStage5b().getAttachments(),filename);
                    break;
                case "6":
                    attachment = getAttachmentByFilename(requestApprovalService.get(id).getStage6().getAttachments(),filename);
                    break;
                default:
                    return null;
            }
        }else{
            switch (stage) {
                case "7":
                    attachment = getAttachmentByFilename(requestPaymentService.get(id).getStage7().getAttachments(),filename);
                    break;
                case "8":
                    attachment = getAttachmentByFilename(requestPaymentService.get(id).getStage8().getAttachments(),filename);
                    break;
                case "9":
                    attachment = getAttachmentByFilename(requestPaymentService.get(id).getStage9().getAttachments(),filename);
                    break;
                case "10":
                    attachment = getAttachmentByFilename(requestPaymentService.get(id).getStage10().getAttachments(),filename);
                    break;
                case "11":
                    attachment = getAttachmentByFilename(requestPaymentService.get(id).getStage11().getAttachments(),filename);
                    break;
                case "12":
                    attachment = getAttachmentByFilename(requestPaymentService.get(id).getStage12().getAttachments(),filename);
                    break;
                case "13":
                    attachment = getAttachmentByFilename(requestPaymentService.get(id).getStage13().getAttachments(),filename);
                    break;
                default:
                    return null;
            }
        }
        return attachment;
    }

    private Attachment getAttachmentByFilename(List<Attachment> attachments,String filename){

        for(Attachment attachment:attachments)
            if(attachment.getFilename().equals(filename))
                return attachment;
        return null;
    }


    public void cascadeAll(Organization organization, Authentication authentication) {
        List<Resource> resources = getRequestsPerOrganization(organization.getId(),authentication);

        for(Resource resource:resources){
            Request request = parserPool.deserialize(resource,typeParameterClass);

            Request request_new = get(request.getId());
            request_new.getProject().getInstitute().setOrganization(organization);
            try {
                update(request_new,request_new.getId());
            } catch (ResourceNotFoundException e) {
                LOGGER.debug("error on updating request ( " + request.getId() + " ) on cascade all ", e);
            }
        }
    }

    public void cascadeAll(Project project, Authentication authentication) {
        List<Resource> resources = getRequestsPerProject(project.getId(),authentication);

        for(Resource resource:resources){
            Request request = parserPool.deserialize(resource,typeParameterClass);
            request.setProject(project);
            try {
                update(request,request.getId());
            } catch (ResourceNotFoundException e) {
                LOGGER.debug("error on updating request ( " + request.getId() + " ) on cascade all ", e);
            }
        }
    }

    public void cascadeAll(Institute institute, Authentication authentication) {
        List<Resource> resources = getRequestsPerInstitute(institute.getId(),authentication);

        for(Resource resource:resources){
            Request request = parserPool.deserialize(resource,typeParameterClass);
            request.getProject().setInstitute(institute);
            try {
                update(request,request.getId());
            } catch (ResourceNotFoundException e) {
                LOGGER.debug("error on updating request ( " + request.getId() + " ) on cascade all ", e);
            }
        }
    }

    public List<Resource> getRequestsPerProject(String id, Authentication authentication) {
        return getByValue("request_project",id,authentication);
    }

    public List<Resource> getRequestsPerInstitute(String id,Authentication authentication) {
        return getByValue("request_institute",id,authentication);
    }

    public List<Resource> getRequestsPerOrganization(String id,Authentication authentication) {
        return getByValue("request_institute_organization",id,authentication);
    }


    private List<Resource> getByValue(String field,String id,Authentication authentication){

        String query = field + "= \"" + id + "\"";

        Paging<Resource> rs = searchService.cqlQuery(
                query,"request",
                1000,0,
                "", "ASC");
        return rs.getResults();
    }

}
