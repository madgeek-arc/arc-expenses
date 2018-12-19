package arc.expenses.service;

import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import gr.athenarc.domain.Institute;
import gr.athenarc.domain.Organization;
import gr.athenarc.domain.Project;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("instituteService")
public class InstituteServiceImpl extends GenericService<Institute>{

    @Autowired
    CascadeService cascadeService;

    private Logger LOGGER = Logger.getLogger(ProjectServiceImpl.class);

    public InstituteServiceImpl() {
        super(Institute.class);
    }

    @Override
    public String getResourceType() {
        return "institute";
    }

    public Paging<Institute> getAllInstitutes(Authentication authentication) {
        FacetFilter filter = new FacetFilter();
        filter.setResourceType(getResourceType());

        filter.setKeyword("");
        filter.setFrom(0);
        filter.setQuantity(1000);

        Map<String,Object> sort = new HashMap<>();
        Map<String,Object> order = new HashMap<>();

        order.put("order","desc");
        sort.put("creation_date", order);
        filter.setOrderBy(sort);

        filter.setFrom(0);
        filter.setQuantity(20);
        return getAll(filter,authentication);
    }

    @Override
    public Institute update(Institute institute, Authentication authentication) throws ResourceNotFoundException {
        update(institute,institute.getId());
        cascadeService.cascadeAll(institute,authentication);
        return institute;
    }


    public void cascadeAll(Organization organization, Authentication authentication) {
        List<Resource> resources = getProjectsPerOrganization(organization.getId(),authentication);

        for(Resource resource:resources){
            Institute institute = parserPool.deserialize(resource,typeParameterClass);
            institute.setOrganization(organization);
            try {
                update(institute,institute.getId());
            } catch (ResourceNotFoundException e) {
                LOGGER.debug("error on updating institute ( " + institute.getId() + " ) on cascade all ", e);
            }
        }
    }

    public List<Resource> getProjectsPerOrganization(String id, Authentication authentication) {
        return getByValue("searchableArea",id,authentication);
    }

    private List<Resource> getByValue(String field,String id,Authentication authentication){

        String query = field + "= \"" + id + "\"";

        Paging<Resource> rs = searchService.cqlQuery(
                query,"institute",
                1000,0,
                "", "ASC");
        return rs.getResults();
    }
}
