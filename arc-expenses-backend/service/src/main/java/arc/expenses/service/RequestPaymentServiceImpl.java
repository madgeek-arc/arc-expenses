package arc.expenses.service;

import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import gr.athenarc.domain.RequestPayment;
import org.apache.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


    public Browsing<RequestPayment> getPayments(String id, Authentication u) throws Exception {
        FacetFilter filter = new FacetFilter();
        filter.setResourceType(getResourceType());
        filter.addFilter("request_id",id);

        filter.setKeyword("");
        filter.setFrom(0);
        filter.setQuantity(1000);

        Map<String,Object> sort = new HashMap<>();
        Map<String,Object> order = new HashMap<>();

        String orderDirection = "desc";
        String orderField = "creation_date";

        order.put("order",orderDirection);
        sort.put(orderField, order);
        filter.setOrderBy(sort);

        return getAll(filter,u);
    }

    public List<RequestPayment> getPayments(List<String> stage, List<String> status, String requestID) throws Exception {
        return null;
    }

    public String generateID(String requestId) {
        String maxID = getMaxID();
        if(maxID == null)
            return requestId+"-p1";
        else
            return requestId+"-p"+(Integer.valueOf(maxID.split("-p")[1])+1);
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
        String orderField = "payment_id";

        order.put("order",orderDirection);
        sort.put(orderField, order);
        filter.setOrderBy(sort);

        try {
            List rs = searchService.search(filter).getResults();
            Resource payment;
            if(rs.size() > 0) {
                payment = ((Resource) rs.get(0));
                return parserPool.deserialize(payment, RequestPayment.class).getId();
            }
        } catch (IOException e) {
            LOGGER.debug("Error on search controller",e);
        }
        return null;
    }

}

