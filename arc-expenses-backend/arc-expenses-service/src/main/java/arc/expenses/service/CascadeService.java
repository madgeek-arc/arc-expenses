package arc.expenses.service;

import eu.openminted.registry.core.exception.ResourceNotFoundException;
import gr.athenarc.domain.Institute;
import gr.athenarc.domain.Organization;
import gr.athenarc.domain.Project;
import gr.athenarc.domain.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("cascadeService")
public class CascadeService {

    @Autowired
    ProjectServiceImpl projectService;

    @Autowired
    InstituteServiceImpl instituteService;

    @Autowired
    RequestServiceImpl requestService;

    public void cascadeAll(Organization organization, Authentication authentication) throws ResourceNotFoundException {

        List<Project> projects =  projectService.getAllProjects(String.valueOf(0),String.valueOf(1000),authentication).getResults();
        List<Institute> institutes =  instituteService.getAllInstitutes(authentication).getResults();
        List<Request> requests = requestService.getAllRequests(authentication).getResults();

        for(Institute institute:institutes){
            institute.setOrganization(organization);
            instituteService.update(institute,institute.getId());
        }

        for(Project project:projects){
            project.getInstitute().setOrganization(organization);
            projectService.update(project,project.getId());
        }

        for(Request request:requests){
            request.getProject().getInstitute().setOrganization(organization);
            requestService.update(request,request.getId());
        }

    }

    public void cascadeAll(Project project, Authentication authentication) throws ResourceNotFoundException {
        List<Request> requests = requestService.getAllRequests(authentication).getResults();
        for(Request request:requests){
            request.setProject(project);
            requestService.update(request,request.getId());
        }
    }

    public void cascadeAll(Institute institute, Authentication authentication) throws ResourceNotFoundException {
        List<Project> projects =  projectService.getAllProjects(String.valueOf(0),String.valueOf(1000),authentication).getResults();
        List<Request> requests = requestService.getAllRequests(authentication).getResults();


        for(Project project:projects){
            project.setInstitute(institute);
            projectService.update(project,project.getId());
        }

        for(Request request:requests){
            request.getProject().setInstitute(institute);
            requestService.update(request,request.getId());
        }

    }


}
