package service;

import gr.athenarc.domain.Organization;
import org.springframework.stereotype.Service;

@Service("organizationService")
public class OrganizationServiceImpl extends GenericService<Organization>{


    public OrganizationServiceImpl() {
        super(Organization.class);
    }

    @Override
    public String getResourceType() {
        return "organization";
    }
}
