package arc.expenses.service;

import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import gr.athenarc.domain.Organization;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service("organizationService")
public class OrganizationServiceImpl extends GenericService<Organization>{


    public OrganizationServiceImpl() {
        super(Organization.class);
    }

    @Override
    public String getResourceType() {
        return "organization";
    }

    public Paging<Organization> getAllOrganizations(Authentication auth) {
        FacetFilter filter = new FacetFilter();
        filter.setResourceType(getResourceType());

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

        filter.setFrom(0);
        filter.setQuantity(20);
        return getAll(filter,auth);
    }
}
