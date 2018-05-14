package service;

import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import gr.athenarc.domain.Request;
import org.apache.log4j.Logger;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service("requestService")
public class RequestServiceImpl extends GenericService<Request> {


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
                             "request_project_operator =  "+ email + " or " +
                             "request_project_operator_delegates = " + email + " or " +
                             "request_project_scientificCoordinator = " + email + " or " +
                             "request_project_scientificCoordinator_delegate =  " + email + " or "+
                             "request_organization_POY = " + email + " or " +
                             "request_organization_POY_delegate =  " + email+ " or "+
                             "request_institute_accountingRegistration = " + email + " or " +
                             "request_institute_diaugeia = " + email + " or " +
                             "request_institute_accountingPayment = " + email + " or " +
                             "request_institute_accountingDirector = " + email + " or " +
                             "request_institute_accountingDirector_delegate =  " + email +
                             "request_institute_accountingRegistration_delegate =  " + email +
                             "request_institute_accountingPayment_delegate =  " + email +
                             "request_institute_diaugeia_delegate =  " + email +
                             "request_organization_director = " + email + " or " +
                             "request_organization_director_delegate =  " + email + " ) ";
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
}
