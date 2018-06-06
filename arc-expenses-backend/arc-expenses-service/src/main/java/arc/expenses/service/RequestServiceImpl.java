package arc.expenses.service;

import arc.expenses.config.StoreRestConfig;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.store.restclient.StoreRESTClient;
import gr.athenarc.domain.Request;
import org.apache.log4j.Logger;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service("requestService")
public class RequestServiceImpl extends GenericService<Request> {

    @Autowired
    private StoreRESTClient storeRESTClient;

    @Autowired
    private StoreRestConfig storeRestConfig;

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

    public String createWhereClause(String email, String status,String searchField,String stage) {

        String whereClause = "";
        String status_clause = "";
        String search_clause = "";
        String user_clause = "";
        String stage_clause = "";


        user_clause += " ( request_requester = " + email + " or " +
                             " request_project_operator =  "+ email + " or " +
                             " request_project_operator_delegates = " + email + " or " +
                             " request_project_scientificCoordinator = " + email + " or " +
                             " request_organization_POΙ = " + email + " or " +
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
                             " request_institute_director_delegate =  " + email + " ) ";
        whereClause += user_clause;

        if(!status.equals("all"))
            status_clause += " and  (   request_status = " + status + " ) ";
        whereClause+=status_clause;

        if(!stage.equals("all"))
            stage_clause += " and (  request_stage = " + stage + " ) ";

        whereClause+=stage_clause;

        if(!searchField.equals(""))
            search_clause+= " and searchableArea = " + searchField;
        whereClause += search_clause;

        
        return whereClause;
    }


    public Paging<Request> criteriaSearch(String from, String quantity,
                                          String status, String searchField,
                                          String stage,String orderType,
                                          String orderField, String email) {

        Paging<Resource> rs = searchService.cqlQuery(
                this.createWhereClause(email,status,searchField,stage),"request",
                Integer.parseInt(quantity),Integer.parseInt(from),
                orderField, SortOrder.valueOf(orderType));


        List<Request> resultSet = new ArrayList<>();
        for(Resource resource:rs.getResults()) {
            try {
                resultSet.add(parserPool.deserialize(resource,typeParameterClass).get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        return new Paging<>(rs.getTotal(),rs.getFrom(),rs.getTo(),resultSet,rs.getOccurrences());

    }

    public List<Request> getPendingRequests(String email) {

        String whereCaluse = " (  ( request_project_operator =  " + email + " or  request_project_operator_delegates = " + email + " ) "
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
                whereCaluse,"request",
                1000,0,"",SortOrder.ASC);


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
        return new ResponseEntity<>(storeRestConfig.getStoreHost()+"/store/downloadFile/?fileName="+archiveID+"/"+stage,
                HttpStatus.OK);
    }

}
