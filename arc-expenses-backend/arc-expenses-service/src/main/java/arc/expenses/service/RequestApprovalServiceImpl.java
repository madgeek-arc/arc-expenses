package arc.expenses.service;

import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import gr.athenarc.domain.Request;
import gr.athenarc.domain.RequestApproval;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service("requestApproval")
public class RequestApprovalServiceImpl extends GenericService<RequestApproval> {


    public RequestApprovalServiceImpl() {
        super(RequestApproval.class);
    }

    @Override
    public String getResourceType() {
        return "approval";
    }

    private Logger LOGGER = Logger.getLogger(RequestApprovalServiceImpl.class);


    public RequestApproval getApproval(String id) throws Exception {
        return getByField("request_id",id);
    }

    public RequestApproval getApproval(List<String> stage, List<String> status, String id) throws Exception {
        return null;
    }

    public String generateID(String requestId) {
        //String maxID = getMaxID();
        //if(maxID == null)
        return requestId+"-a1";
        //else
         //   return requestId+"-a"+(Integer.valueOf(maxID.split("-a")[1])+1);

    }


    private String getMaxID() {

        FacetFilter filter = new FacetFilter();
        filter.setResourceType(getResourceType());
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
            String approval = null;
            if(rs.size() > 0) {
                approval = ((Resource) rs.get(0)).getPayload();
                return parserPool.deserialize(approval, RequestApproval.class).get().getId();
            }
        } catch (IOException e) {
            LOGGER.debug("Error on search controller",e);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }
}
