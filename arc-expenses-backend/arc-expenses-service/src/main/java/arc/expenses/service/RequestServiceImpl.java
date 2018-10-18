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
    @Qualifier("arc.dataSource")
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
            String request = null;
            if(rs.size() > 0) {
                request = ((Resource) rs.get(0)).getPayload();
                return parserPool.deserialize(request,Request.class).get().getId();
            }
        } catch (IOException e) {
            LOGGER.debug("Error on search controller",e);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String createWhereClause(String email, List<String> status, String searchField, List<String> stage) {

        StringBuilder status_clause = new StringBuilder();
        StringBuilder search_clause = new StringBuilder();
        StringBuilder user_clause = new StringBuilder();
        StringBuilder stage_clause = new StringBuilder();
        StringBuilder whereClause = new StringBuilder();


        if(!admins.contains(email)) {
            user_clause.append(" ( request_requester = " + email + " or " +
                    " request_project_operator =  " + email + " or " +
                    " request_project_operator_delegates = " + email + " or " +
                    " request_project_scientificCoordinator = " + email + " or " +
                    " request_organization_POI = " + email + " or " +
                    " request_organization_POΙ_delegate =  " + email + " or " +
                    " request_institute_accountingRegistration = " + email + " or " +
                    " request_institute_diaugeia = " + email + " or " +
                    " request_institute_accountingPayment = " + email + " or " +
                    " request_institute_accountingDirector = " + email + " or " +
                    " request_institute_accountingDirector_delegate =  " + email + " or " +
                    " request_institute_accountingRegistration_delegate =  " + email + " or " +
                    " request_institute_accountingPayment_delegate =  " + email + " or " +
                    " request_institute_diaugeia_delegate =  " + email + " or " +
                    " request_organization_director = " + email + " or " +
                    " request_institute_director = " + email + " or " +
                    " request_organization_director_delegate =  " + email + " or " +
                    " request_institute_director_delegate =  " + email + " ) ");
            whereClause.append(user_clause);
        }

        if(!status.get(0).equals("all")){
            if(user_clause.length() != 0)
                status_clause.append(" and " );
            status_clause.append("( status = ").append(status.get(0));

            if(status.get(0).equals("pending"))
                status_clause.append(" or status = under_review");

            for(int i=1;i<status.size();i++){
                if(status.get(i).equals("pending"))
                    status_clause.append(" or status = under_review");
                status_clause.append(" or status = ").append(status.get(i));
            }
            status_clause.append(")");
            whereClause.append(status_clause);
        }

        if(!stage.get(0).equals("all")){

            if(user_clause.length() != 0 || status_clause.length()!=0)
                stage_clause.append("and");

            stage_clause.append(" ( stage = ").append(stage.get(0));
            for(int i=1;i<stage.size();i++)
                stage_clause.append(" or stage = ").append(stage.get(i));
            stage_clause.append(")");
            whereClause.append(stage_clause);
        }


        if(searchField!=null && !searchField.equals("")){
            if(stage_clause.length()!=0 || status_clause.length()!=0)
                search_clause.append("and ");
            search_clause.append(" searchableArea = ").append(searchField);
            whereClause.append(search_clause);
        }

        if(whereClause.length() == 0)
            whereClause.append("status = accepted or status = pending or status = under_review");

        return whereClause.toString();
    }


    public Paging<RequestSummary> criteriaSearch(String from, String quantity,
                                 List<String> status, String searchField,
                                 List<String> stage, String orderType,
                                 String orderField, String email) {

        String query =  " ( select distinct(r.request_id) as request_id ,a.approval_id as id,creation_date ," +
                        "   r.request_project as request_project , r.request_institute as request_institute , a.stage as request_stage" +
                        " from request_view r , approval_view a , resource res  " +
                        " where r.request_id = a.request_id  and res.fk_name = 'approval' " +
                        " and a.id  = res.id  and a.stage = (select data.value as value from resource ," +
                                                           " json_each_text(resource.payload::json) as data " +
                                                           " where fk_name = 'approval' and key = 'stage' and id = a.id) ";



        StringBuilder user_clause = this.getUserClause(email);
        if(user_clause.length()!=0)
            query+= " and " + user_clause.toString();

        StringBuilder stage_clause = this.getStageClause("a",stage);
        if( stage_clause.length()!=0)
            query+= " and  " + stage_clause.toString();

        StringBuilder status_clause = this.getStatusClause("a",status);
        if(status_clause.length()!=0)
            query+= " and " + status_clause.toString();

        StringBuilder keyword_clause = this.getKeywordClause(searchField);
        if(keyword_clause.length()!=0)
            query+= " and " + keyword_clause.toString();



        query+=" )" +
               " union " +
               " ( select distinct(r.request_id) as request_id,p.payment_id as id,creation_date ," +
               " r.request_project as request_project , r.request_institute as request_institute , p.stage as request_stage" +
               " from request_view r , payment_view p , resource res  " +
               " where r.request_id = p.request_id  and res.fk_name = 'payment' " +
               " and p.id  = res.id  and p.stage = (select data.value as value from resource ," +
                                                  " json_each_text(resource.payload::json) as data " +
                                                  " where fk_name = 'payment' and key = 'stage' and id = p.id ) ";

        user_clause = this.getUserClause(email);
        if(user_clause.length()!=0)
            query+= " and " + user_clause.toString();

        stage_clause = this.getStageClause("p",stage);
        if( stage_clause.length()!=0)
            query+= " and  " + stage_clause.toString();

        status_clause = this.getStatusClause("p",status);
        if(status_clause.length()!=0)
            query+= " and " + status_clause.toString();

        keyword_clause = this.getKeywordClause(searchField);
        if(keyword_clause.length()!=0)
            query+= " and " + keyword_clause.toString();

        query += ")";
        query +=  "  order by "+orderField + " "  + orderType +
                  "  limit " + quantity +
                  "  offset " + from;


        LOGGER.info(query);
        List<Sextet<String,String,String,String,String,String>> resultSet =  new JdbcTemplate(dataSource)
                .query(query,requestSummaryMapper);


        List<RequestSummary> rs = new ArrayList<>();
        for(Sextet<String,String,String,String,String,String> sextet : resultSet){
            RequestSummary requestSummary = new RequestSummary();

            if(sextet.getValue1().contains("a"))
                requestSummary.setBaseInfo(Converter.toBaseInfo(requestApprovalService.get(sextet.getValue1())));
            else
                requestSummary.setBaseInfo(Converter.toBaseInfo(requestPaymentService.get(sextet.getValue1())));

            requestSummary.setRequest(get(requestSummary.getBaseInfo().getRequestId()));
            rs.add(requestSummary);
        }

        return new Paging<>(rs.size(),Integer.parseInt(from),Integer.parseInt(quantity),rs,null);
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

    private RowMapper<Sextet<String,String,String,String,String,String>> requestSummaryMapper = (rs, i) ->
            Sextet.with(rs.getString("request_id"),
                rs.getString("id"),
                rs.getString("creation_date"),
                rs.getString("request_project"),
                rs.getString("request_institute"),
                rs.getString("request_stage"));

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
                    " r.request_project_operator <@ '{"+'"' + email + '"' + "}' or " +
                    " r.request_project_operator_delegate <@ '{"+'"' + email + '"' + "}' or " +
                    " r.request_project_scientificCoordinator = '"  + email + "' or " +
                    " r.request_organization_POI = '"  + email + "' or " +
                    " r.request_organization_POI_delegate <@  '{"+'"' + email + '"' + "}' or " +
                    " r.request_institute_accountingRegistration = '"  + email + "' or " +
                    " r.request_institute_diaugeia = '"  + email + "' or " +
                    " r.request_institute_accountingPayment = '"  + email + "' or " +
                    " r.request_institute_accountingDirector = '"  + email + "' or " +
                    " r.request_institute_accountingDirector_delegate <@  '{"+'"' + email + '"' + "}' or " +
                    " r.request_institute_accountingRegistration_delegate <@  '{"+'"' + email + '"' + "}' or " +
                    " r.request_institute_accountingPayment_delegate <@  '{"+'"' + email + '"' + "}' or " +
                    " r.request_institute_diaugeia_delegate <@  '{"+'"' + email + '"' + "}' or " +
                    " r.request_organization_director = '"  + email + "' or " +
                    " r.request_institute_director = '"  + email + "' or " +
                    " r.request_organization_director_delegate <@  '{"+'"' + email + '"' + "}' or " +
                    " r.request_institute_director_delegate <@  '{"+'"' + email + '"' + "}' ) ");
        }
        return user_clause;
    }

    public List<Request> getPendingRequests(String email) {

        String whereClause = " (  ( request_project_operator =  " + email + " or  request_project_operator_delegates = " + email + " ) "
                           + "      and ( request_stage = 3 or request_stage = 7 ) "
                           + "    ) "
                           + " or ( request_project_scientificCoordinator = " + email + " and request_stage = 2 ) "
                           + " or ( ( request_organization_POΙ =  " + email + " or  request_organization_POΙ_delegate = "  + email + " ) "
                           + "      and ( request_stage = 4 or request_stage = 9 ) "
                           + "    ) "
                           + " or ( ( request_organization_director =  " + email + " or  request_organization_director_delegate = "  + email + " )"
                           + "      and ( request_stage = 5a or request_stage = 10 ) "
                           + "    ) "
                           + " or ( ( request_organization_dioikitikoSumvoulio =  " + email + " or  request_organization_dioikitikoSumvoulio_delegate = "  + email + " )"
                           + "      and request_stage = 5b "
                           + "    ) "
                           + " or ( ( request_institute_diaugeia =  " + email + " or  request_institute_diaugeia_delegate = "  + email + " )"
                           + "      and ( request_stage = 6 or request_stage = 11 ) "
                           + "    ) "
                           + " or ( ( request_institute_accountingDirector =  " + email + " or  request_institute_accountingDirector_delegate = "  + email + " )"
                           + "      and request_stage = 8 "
                           + "    ) "
                           + " or ( ( request_institute_accountingRegistration =  " + email + " or  request_institute_accountingRegistration_delegate = "  + email + " )"
                           + "      and request_stage = 12 "
                           + "    ) "
                           + " or ( ( request_institute_accountingPayment =  " + email + " or  request_institute_accountingPayment_delegate = "  + email + " )"
                           + "      and request_stage = 13 "
                           + "    ) ";

        Paging<Resource> rs = searchService.cqlQuery(
                whereClause,"request",
                1000,0,"", "ASC");


        List<Request> resultSet = new ArrayList<>();
        for(Resource resource:rs.getResults()) {
            try {
                resultSet.add(parserPool.deserialize(resource,typeParameterClass).get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
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
        return new ResponseEntity<>(storeRestConfig.getStoreHost()+"/store/download/filename="+fileName,
                HttpStatus.OK);

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
        Attachment attachment = null;

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
