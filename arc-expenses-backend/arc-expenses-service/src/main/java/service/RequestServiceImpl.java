package service;

import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import gr.athenarc.domain.Request;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public String createQuery(String email) {
        return "request_requester = " + email + " or " +
                "request_project_operator = " + email + " or " +
                "request_project_operator_delegates = " + email;
    }
}
