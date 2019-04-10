package arc.expenses;

import eu.openminted.registry.core.exception.ResourceNotFoundException;
import gr.athenarc.domain.Institute;
import gr.athenarc.domain.Organization;
import gr.athenarc.domain.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("cascadeService")
public class CascadeService {

    @Autowired
    ProjectServiceImpl projectService;

    @Autowired
    InstituteServiceImpl instituteService;

    @Autowired
    RequestServiceImpl requestService;


    public void cascadeAll(Organization organization, Authentication authentication) throws ResourceNotFoundException {
        instituteService.cascadeAll(organization,authentication);
        requestService.cascadeAll(organization,authentication);
        projectService.cascadeAll(organization,authentication);
    }

    public void cascadeAll(Project project, Authentication authentication) throws ResourceNotFoundException {
        requestService.cascadeAll(project,authentication);
    }

    public void cascadeAll(Institute institute, Authentication authentication) throws ResourceNotFoundException {
        projectService.cascadeAll(institute,authentication);
        requestService.cascadeAll(institute,authentication);
    }


}
