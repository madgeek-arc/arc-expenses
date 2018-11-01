package arc.expenses.service;

import arc.expenses.config.StoreRestConfig;
import arc.expenses.domain.RequestSummary;
import arc.expenses.utils.Converter;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.store.restclient.StoreRESTClient;
import gr.athenarc.domain.Attachment;
import gr.athenarc.domain.Request;
import org.apache.log4j.Logger;
import org.javatuples.Septet;
import org.javatuples.Sextet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

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

        FacetFilter filter = new FacetFilter();
        filter.setResourceType("request");
        filter.setKeyword("");
        filter.setFrom(0);
        filter.setQuantity(1);

        Map<String,Object> sort = new HashMap<>();
        Map<String,Object> order = new HashMap<>();

        String orderDirection = "desc";
        String orderField = "creation_date";

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
            LOGGER.debug("Error on search controller",e);
        }
        return null;
    }


    public Paging<RequestSummary> criteriaSearch(String from, String quantity,
                                 List<String> status,List<String> type, String searchField,
                                 List<String> stage, String orderType,
                                 String orderField, String email) {

        String query =  "select request_id,id,creation_date,request_project,request_institute,request_stage , count(*) over () as total_rows from (       ( select distinct(r.request_id) as request_id ,a.approval_id as id,res1.creation_date as creation_date,         r.request_project as request_project , r.request_institute as request_institute , a.stage as request_stage , r.request_type as request_type        from request_view r , approval_view a , resource res1, resource res2           where r.request_id = a.request_id  and res1.fk_name = 'approval'          and a.id  = res1.id AND r.id = res2.id          AND res2.fk_name = 'request' " ;



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
               " ( select distinct(r.request_id) as request_id,p.payment_id as id,res1.creation_date ," +
               " r.request_project as request_project , r.request_institute as request_institute , p.stage as request_stage , r.request_type as request_type" +
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
                                .append( "( ((res2.payload::json)->'project'->>'operator')::text ilike '%")
                                .append(searchField).append("%')")
                                .append( " or  ")
                                .append( "( ((res2.payload::json)->'project'->>'scientificCoordinator')::text ilike '%")
                                .append(searchField).append("%') )");

        return searchField_clause;
    }

    private StringBuilder getKeywordClause(String searchField) {

        StringBuilder keyword_clause = new StringBuilder();


        if(searchField.equals(""))
            return keyword_clause;

        keyword_clause.append(" ( request_requester = ")
                .append("'").append(searchField).append("'")
                .append(" or request_project = ")
                .append("'").append(searchField).append("'")
                .append(" or request_institute = ")
                .append("'").append(searchField).append("'")
                .append(" or request_institute_director = ")
                .append("'").append(searchField).append("'")
                .append(" or request_project_operator  ").append("<@  '{" + '"').append(searchField).append('"').append("}'").append(")");
        return keyword_clause;
    }

    private RowMapper<Septet<String,String,String,String,String,String,String>> requestSummaryMapper = (rs, i) ->
            Septet.with(rs.getString("request_id"),
                rs.getString("id"),
                rs.getString("creation_date"),
                rs.getString("request_project"),
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

        if(!admins.contains(email)) {
            user_clause.append(" ( r.request_requester = '"  + email + "' or " +
                    " r.request_project_operator @> '{"+'"' + email + '"' + "}' or " +
                    " r.request_project_operator_delegate @> '{"+'"' + email + '"' + "}' or " +
                    " r.request_project_scientificCoordinator = '"  + email + "' or " +
                    " r.request_organization_poy = '"  + email + "' or " +
                    " r.request_organization_poy_delegate @>  '{"+'"' + email + '"' + "}' or " +
                    " r.request_institute_accountingRegistration = '"  + email + "' or " +
                    " r.request_institute_diaugeia = '"  + email + "' or " +
                    " r.request_institute_accountingPayment = '"  + email + "' or " +
                    " r.request_institute_accountingDirector = '"  + email + "' or " +
                    " r.request_institute_accountingDirector_delegate @>  '{"+'"' + email + '"' + "}' or " +
                    " r.request_institute_accountingRegistration_delegate @>  '{"+'"' + email + '"' + "}' or " +
                    " r.request_institute_accountingPayment_delegate @>  '{"+'"' + email + '"' + "}' or " +
                    " r.request_institute_diaugeia_delegate @>  '{"+'"' + email + '"' + "}' or " +
                    " r.request_organization_director = '"  + email + "' or " +
                    " r.request_institute_director = '"  + email + "' or " +
                    " r.request_organization_director_delegate @>  '{"+'"' + email + '"' + "}' or " +
                    " r.request_institute_director_delegate @>  '{"+'"' + email + '"' + "}' ) ");
        }
        return user_clause;
    }

    public List<Request> getPendingRequests(String email) {

        //language=SQL
        String whereClause = " (  ( r.request_project_operator <@ '{"+'"' + email + '"' + "} or  request_project_operator_delegate <@ '{"+'"' + email + '"' + "}')"
                           + "      and ( request_stage = 3 ) "
                           + "    ) "
                           + " or ( request_institute_suppliesOffice = '" + email + "' and request_stage = 7 and request_type != trip ) "
                           + " or ( request_institute_travelManager = '" + email + "' and request_stage = 7 and request_type = trip ) "
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
                           + " or ( ( request_institute_accountingDirector =  '" + email + "' or  request_institute_accountingDirector_delegate <@ '{"+'"' + email + '"' + "}')"
                           + "      and request_stage = 8 "
                           + "    ) "
                           + " or ( ( request_institute_accountingRegistration =  '" + email + "' or  request_institute_accountingRegistration_delegate <@ '{"+'"' + email + '"' + "}')"
                           + "      and request_stage = 12 "
                           + "    ) "
                           + " or ( ( request_institute_accountingPayment =  '" + email + "' or  request_institute_accountingPayment_delegate <@ '{"+'"' + email + '"' + "}')"
                           + "      and request_stage = 13 "
                           + "    ) "
                            + " or (  request_requester =  '" + email + "'" + " and request_type = trip and request_stage = 7 "
                            + "    ) " ;


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
            LOGGER.info(e);
            return new ResponseEntity<>("ERROR",HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(archiveID+"/"+fileName,HttpStatus.OK);

    }

    public InputStream downloadFile(String mode,String id, String stage) {

        Attachment attachment = getAttachment(mode,id,stage);
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

    public Attachment getAttachment(String mode ,String id, String stage) {
        Attachment attachment;

        if(mode.equals("request"))
            return get(id).getStage1().getAttachment();
        else if(mode.equals("approval")){
            switch (stage) {
                case "2":
                    attachment = requestApprovalService.get(id).getStage2().getAttachment();
                    break;
                case "3":
                    attachment = requestApprovalService.get(id).getStage3().getAttachment();
                    break;
                case "4":
                    attachment = requestApprovalService.get(id).getStage4().getAttachment();
                    break;
                case "5":
                    attachment = requestApprovalService.get(id).getStage5().getAttachment();
                    break;
                case "5a":
                    attachment = requestApprovalService.get(id).getStage5a().getAttachment();
                    break;
                case "5b":
                    attachment = requestApprovalService.get(id).getStage5b().getAttachment();
                    break;
                case "6":
                    attachment = requestApprovalService.get(id).getStage6().getAttachment();
                    break;
                default:
                    return null;
            }
        }else{
            switch (stage) {
                case "7":
                    attachment = requestPaymentService.get(id).getStage7().getAttachment();
                    break;
                case "8":
                    attachment = requestPaymentService.get(id).getStage8().getAttachment();
                    break;
                case "9":
                    attachment = requestPaymentService.get(id).getStage9().getAttachment();
                    break;
                case "10":
                    attachment = requestPaymentService.get(id).getStage10().getAttachment();
                    break;
                case "11":
                    attachment = requestPaymentService.get(id).getStage11().getAttachment();
                    break;
                case "12":
                    attachment = requestPaymentService.get(id).getStage12().getAttachment();
                    break;
                case "13":
                    attachment = requestPaymentService.get(id).getStage13().getAttachment();
                    break;
                default:
                    return null;
            }
        }
        return attachment;
    }


}
