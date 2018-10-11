package arc.expenses.service;

import arc.expenses.config.StoreRestConfig;
import arc.expenses.domain.RequestSummary;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.store.restclient.StoreRESTClient;
import gr.athenarc.domain.Attachment;
import gr.athenarc.domain.BaseInfo;
import gr.athenarc.domain.Request;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    StoreRESTClient storeClient;

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


        if(!admins.contains(email))
            user_clause.append(" ( request_requester = " + email + " or " +
                                 " request_project_operator =  "+ email + " or " +
                                 " request_project_operator_delegates = " + email + " or " +
                                 " request_project_scientificCoordinator = " + email + " or " +
                                 " request_organization_POI = " + email + " or " +
                                 " request_organization_POΙ_delegate =  " + email+ " or "+
                                 " request_institute_accountingRegistration = " + email + " or " +
                                 " request_institute_diaugeia = " + email + " or " +
                                 " request_institute_accountingPayment = " + email + " or " +
                                 " request_institute_accountingDirector = " + email + " or " +
                                 " request_institute_accountingDirector_delegate =  " + email + " or " +
                                 " request_institute_accountingRegistration_delegate =  " + email +" or " +
                                 " request_institute_accountingPayment_delegate =  " + email +" or " +
                                 " request_institute_diaugeia_delegate =  " + email +" or " +
                                 " request_organization_director = " + email + " or " +
                                 " request_institute_director = " + email + " or " +
                                 " request_organization_director_delegate =  " + email + " or "+
                                 " request_institute_director_delegate =  " + email + " ) ");




        whereClause.append(user_clause);

        if(!status.get(0).equals("all")){
            if(user_clause.length() != 0)
                status_clause.append(" and " );
            status_clause.append("( request_status = ").append(status.get(0));

            if(status.get(0).equals("pending"))
                status_clause.append(" or request_status = under_review");

            for(int i=1;i<status.size();i++){
                if(status.get(i).equals("pending"))
                    status_clause.append(" or request_status = under_review");
                status_clause.append(" or request_status = ").append(status.get(i));
            }
            status_clause.append(")");
        }

        whereClause.append(status_clause);


        if(!stage.get(0).equals("all")){
            if(status_clause.length()!=0)
                stage_clause.append("and");
            stage_clause.append(" ( request_stage = ").append(stage.get(0));
            for(int i=1;i<stage.size();i++)
                stage_clause.append(" or request_status = ").append(stage.get(i));
            stage_clause.append(")");
        }
        whereClause.append(stage_clause);

        if(!searchField.equals("")){
            if(stage_clause.length()!=0 || status_clause.length()!=0)
                search_clause.append("and ");
            search_clause.append(" searchableArea = ").append(searchField);
        }
        whereClause.append(search_clause);
        return whereClause.toString();
    }


    public Paging<RequestSummary> criteriaSearch(String from, String quantity,
                                                 List<String> status, String searchField,
                                                 List<String> stage, String orderType,
                                                 String orderField, String email) {

        Paging<Resource> rs = searchService.cqlQuery(
                this.createWhereClause(email,status,searchField,stage),"requestGroup",
                Integer.parseInt(quantity),Integer.parseInt(from),
                orderField, orderType);


        List<RequestSummary> resultSet = new ArrayList<>();
        for(Resource resource:rs.getResults()) {
            try {
                RequestSummary requestSummary = new RequestSummary();
                requestSummary.setBaseInfo(parserPool.deserialize(resource, BaseInfo.class).get());
                requestSummary.setRequest(get(requestSummary.getBaseInfo().getRequestId()));
                resultSet.add(requestSummary);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        return new Paging<>(rs.getTotal(),rs.getFrom(),rs.getTo(),resultSet,rs.getOccurrences());

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

    public ResponseEntity<Object> upLoadFile(String archiveID,String stage, MultipartFile file) {

        if(Boolean.parseBoolean(storeRESTClient.fileExistsInArchive(archiveID,stage).getResponse()))
            storeRESTClient.deleteFile(archiveID,stage);

        try {
            storeRESTClient.storeFile(file.getBytes(),archiveID,stage);
        } catch (IOException e) {
            LOGGER.info(e);
            return new ResponseEntity<>("ERROR",HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(storeRestConfig.getStoreHost()+"/store/downloadFile/?filename="+archiveID+"/"+stage,
                HttpStatus.OK);
    }

    public InputStream downloadFile(String requestId, String stage) {

        Attachment attachment = getAttachment(requestId,stage);
        try {
            File temp = File.createTempFile("file", "tmp");

            temp.deleteOnExit();
            storeClient.downloadFile(attachment.getUrl(), temp.getAbsolutePath());
            return new FileInputStream(temp);
        } catch (Exception e) {
            LOGGER.error("error downloading file", e);
        }

        return null;
    }

    public Attachment getAttachment(String requestId, String stage) {
        Attachment attachment = null;
        Request request = get(requestId);

//        switch (stage) {
//            case "1":
//                attachment = request.getStage1().getAttachment();
//                break;
//            case "2":
//                attachment = request.getStage2().getAttachment();
//                break;
//            case "3":
//                attachment = request.getStage3().getAttachment();
//                break;
//            case "4":
//                attachment = request.getStage4().getAttachment();
//                break;
//            case "5":
//                attachment = request.getStage5().getAttachment();
//                break;
//            case "5a":
//                attachment = request.getStage5a().getAttachment();
//                break;
//            case "5b":
//                attachment = request.getStage5b().getAttachment();
//                break;
//            case "UploadInvoice":
//                attachment = request.getStageUploadInvoice().getAttachment();
//                break;
//            case "6":
//                attachment = request.getStage6().getAttachment();
//                break;
//            case "7":
//                attachment = request.getStage7().getAttachment();
//                break;
//            case "8":
//                attachment = request.getStage8().getAttachment();
//                break;
//            case "9":
//                attachment = request.getStage9().getAttachment();
//                break;
//            case "10":
//                attachment = request.getStage10().getAttachment();
//                break;
//            case "11":
//                attachment = request.getStage11().getAttachment();
//                break;
//            case "12":
//                attachment = request.getStage12().getAttachment();
//                break;
//            case "13":
//                attachment = request.getStage13().getAttachment();
//                break;
//            default:
//                return null;
//        }
        return attachment;
    }


}
