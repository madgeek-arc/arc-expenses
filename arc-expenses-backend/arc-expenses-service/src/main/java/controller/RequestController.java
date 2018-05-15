package controller;

import config.SAMLAuthentication;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.SearchService;
import gr.athenarc.domain.Request;
import io.swagger.annotations.Api;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import service.PolicyCheckerService;
import service.RequestServiceImpl;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(value = "/request")
@Api(description = "Request API  ",  tags = {"Manage requests"})
public class RequestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestController.class);

    @Autowired
    RequestServiceImpl requestService;

    @Autowired
    PolicyCheckerService policyCheckerService;

    @Autowired
    SearchService searchService;


    @RequestMapping(value =  "/getById/{id}", method = RequestMethod.GET)
    public Request getById(@PathVariable("id") String id) {
        return requestService.get(id);
    }

    @RequestMapping(value =  "/getAll", method = RequestMethod.GET)
    public Paging<Request> getAll(@RequestParam(value = "from",required=false,defaultValue = "0") String from,
                         @RequestParam(value = "quantity",required=false,defaultValue = "10") String quantity,
                         @RequestParam(value = "status") String status,
                         @RequestParam(value = "searchField") String searchField,
                         @RequestParam(value = "stage") String stage,
                         @RequestParam(value = "order",required=false,defaultValue = "ASC") String orderType,
                         @RequestParam(value = "orderField") String orderField,
                         @RequestParam(value = "email") String email) {

        return requestService.criteriaSearch(from,quantity,status,searchField,stage,orderType,orderField,email);

    }

    @RequestMapping(value = "/add", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    Request addRequest(@RequestBody Request request) {
        request.setId(requestService.generateID());
        return requestService.add(request);
    }

    @RequestMapping(value = "/updateRequest", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    Request updateRequest(@RequestBody Request request) throws ResourceNotFoundException {
        return requestService.update(request,request.getId());
    }

    @RequestMapping(value = "/isEditable", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Boolean> isEditable(@RequestBody Request request,
                                       @RequestParam("email") String email) {


        if(policyCheckerService.updateFilter(request,email))
            return new ResponseEntity<>( true, HttpStatus.OK);
        return new ResponseEntity<>( false, HttpStatus.OK);
    }




}