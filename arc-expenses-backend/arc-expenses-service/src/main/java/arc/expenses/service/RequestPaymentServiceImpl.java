package arc.expenses.service;

import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import gr.athenarc.domain.RequestApproval;
import gr.athenarc.domain.RequestPayment;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service("requestPayment")
public class RequestPaymentServiceImpl extends GenericService<RequestPayment> {


    public RequestPaymentServiceImpl() {
        super(RequestPayment.class);
    }

    @Override
    public String getResourceType() {
        return "payment";
    }

    private Logger LOGGER = Logger.getLogger(RequestApprovalServiceImpl.class);


    public Browsing<RequestPayment> getPayments(String id) throws Exception {
        FacetFilter facetFilter = new FacetFilter();
        facetFilter.setResourceType(getResourceType());
        facetFilter.addFilter("request_id",id);
        return getAll(facetFilter);
    }

    public List<RequestPayment> getPayments(List<String> stage, List<String> status, String requestID) throws Exception {
        return null;
    }

    public String generateID(String requestId) {
        String maxID = getMaxID();
        if(maxID == null)
            return requestId+"-p1";
        else{
            String number[] = maxID.split("-p");
            return requestId+"-p"+number[0]+1;
        }
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
            String payment = null;
            if(rs.size() > 0) {
                payment = ((Resource) rs.get(0)).getPayload();
                return parserPool.deserialize(payment, RequestPayment.class).get().getId();
            }
        } catch (IOException e) {
            LOGGER.debug("Error on search controller",e);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

}

