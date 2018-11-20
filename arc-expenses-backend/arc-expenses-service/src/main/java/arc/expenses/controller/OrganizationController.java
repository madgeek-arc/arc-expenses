package arc.expenses.controller;

import eu.openminted.registry.core.exception.ResourceNotFoundException;
import gr.athenarc.domain.Organization;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import arc.expenses.service.OrganizationServiceImpl;

import java.util.List;

@RestController
@RequestMapping(value = "/organization")
@Api(description = "Organization API  ",  tags = {"Manage organization"})
public class OrganizationController {

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
    Organization addOrganization(@RequestBody Organization organization, Authentication auth) {
        return organizationService.add(organization, auth);
    }

    @RequestMapping(value =  "/getAll", method = RequestMethod.GET)
    public List<Organization> getAll() {
        return null;
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    Organization updateOrganization(@RequestBody Organization organization, Authentication auth) throws ResourceNotFoundException {
        return organizationService.update(organization, auth);
    }

}
