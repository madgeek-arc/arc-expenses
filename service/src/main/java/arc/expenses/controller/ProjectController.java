package arc.expenses.controller;

import arc.expenses.domain.Vocabulary;
import arc.expenses.service.ProjectServiceImpl;
import eu.openminted.registry.core.domain.Paging;
import gr.athenarc.domain.Project;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/project")
@Api(description = "Project API  ",  tags = {"Manage projects"})
public class ProjectController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectController.class);

    @Autowired
    ProjectServiceImpl projectService;


    @RequestMapping(value =  "/getById/{id}", method = RequestMethod.GET)
    public Project getById(@PathVariable("id") String id) {
        return projectService.get(id);
    }

    @RequestMapping(value = "/add", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    Project addProject(@RequestBody Project project, Authentication auth) {
        return projectService.add(project, auth);
    }

    @RequestMapping(value =  "/getAll/{from}/{quantity}", method = RequestMethod.GET)
    public Paging<Project> getAll(@PathVariable("from") String from,
                                  @PathVariable("quantity") String quantity,
                                   Authentication auth) {
        return projectService.getAllProjects(from,quantity,auth);
    }

    @RequestMapping(value =  "/getAllProjectNames", method = RequestMethod.GET)
    public List<Vocabulary> getAllProjectNames() {
        return projectService.getAllProjectNames();
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Project updateProject(
            @RequestBody Project project
    ) throws Exception {
        return projectService.update(project);
    }

    @RequestMapping(value =  "/getProjectsOfOperator", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Vocabulary> getProjectsOfOperator(@RequestParam("email") String email) {
        return projectService.getProjectsOfOperator(email);
    }

}
