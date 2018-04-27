package service;

import controller.ProjectController;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.*;
import eu.openminted.registry.core.validation.ResourceValidator;
import exception.ResourceException;
import gr.athenarc.request.Project;
import gr.athenarc.request.Request;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import utils.ParserPool;

import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

@Service("projectService")
public class ProjectServiceImpl extends AbstractGenericService<Project> implements ResourceCRUDService<Project> {

    private Logger LOGGER = Logger.getLogger(ProjectServiceImpl.class);

    @Autowired
    SearchService searchService;

    @Autowired
    ParserPool parserPool;

    @Autowired
    ResourceService resourceService;

    @Autowired
    ResourceValidator resourceValidator;

    public ProjectServiceImpl() {
        super(Project.class);
    }

    @Override
    public String getResourceType() {
        return "project";
    }

    @Override
    public Project get(String id) {
        Project project;
        Resource resource;
        try {
            resource = searchService.searchId("project",
                    new SearchService.KeyValue("project_id", id));
            project = parserPool.deserialize(resource,Project.class).get();
        } catch (UnknownHostException | ExecutionException | InterruptedException e) {
            LOGGER.fatal(e);
            return null;
        }
        return project;
    }

    @Override
    public Browsing<Project> getAll(FacetFilter facetFilter) {
        return null;
    }

    @Override
    public Browsing<Project> getMy(FacetFilter facetFilter) {
        return null;
    }

    @Override
    public Project add(Project project) {


        String serialized = null;

        try {
            serialized = parserPool.serialize(project, ParserService.ParserServiceTypes.JSON).get();
            Resource created = new Resource();
            created.setPayload(serialized);
            created.setResourceType(resourceType);
            resourceService.addResource(created);
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.fatal(e);
        }
        return project;
    }

    @Override
    public Project update(Project project) throws ResourceNotFoundException {
        return null;
    }

    @Override
    public void delete(Project project) throws ResourceNotFoundException {

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
