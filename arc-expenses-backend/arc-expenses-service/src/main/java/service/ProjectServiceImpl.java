package service;

import eu.openminted.registry.core.service.SearchService;
import eu.openminted.registry.core.service.ServiceException;
import gr.athenarc.domain.Project;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

@Service("projectService")
public class ProjectServiceImpl extends GenericService<Project> {

    private Logger LOGGER = Logger.getLogger(ProjectServiceImpl.class);

    public ProjectServiceImpl() {
        super(Project.class);
    }

    @Override
    public String getResourceType() {
        return "project";
    }

    public Project getByAcronym(String acronym) {
        Project project;
        try {
            project = parserPool.deserialize(searchService.searchId("project",new SearchService.KeyValue("project_acronym", acronym)), Project.class).get();
        } catch (UnknownHostException | ExecutionException | InterruptedException e) {
            LOGGER.fatal(e);
            throw new ServiceException(e);
        }
        return project;
    }
}
