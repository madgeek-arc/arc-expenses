package controller;

import gr.athenarc.domain.Organization;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import service.OrganizationServiceImpl;

import java.util.List;

@RestController
@RequestMapping(value = "/organization")
@Api(description = "Organization API  ",  tags = {"Manage organization"})
public class OrganizationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectController.class);

    @Autowired
    OrganizationServiceImpl organizationService;


    @RequestMapping(value =  "/getById/{id}", method = RequestMethod.GET)
    public Organization getById(@PathVariable("id") String id) {
        return organizationService.get(id);
    }


    @RequestMapping(value = "/add", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    Organization addOrganization(@RequestBody Organization organization) {
        return organizationService.add(organization);
    }

    @RequestMapping(value =  "/getAll", method = RequestMethod.GET)
    public List<Organization> getAll() {
        return null;
    }


}
