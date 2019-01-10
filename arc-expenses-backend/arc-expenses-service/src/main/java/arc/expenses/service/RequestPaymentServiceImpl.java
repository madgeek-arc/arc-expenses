package arc.expenses.service;

import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import gr.athenarc.domain.RequestPayment;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("requestPayment")
public class RequestPaymentServiceImpl extends GenericService<RequestPayment> {

    @Autowired
    DataSource dataSource;

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


    public String getMaxID() {
        return new JdbcTemplate(dataSource)
                .query("select max(request_id) as maxID from request_view",maxIDRowMapper).get(0);
    }

    private RowMapper<String> maxIDRowMapper = (rs, i) -> rs.getString("maxID");


}

