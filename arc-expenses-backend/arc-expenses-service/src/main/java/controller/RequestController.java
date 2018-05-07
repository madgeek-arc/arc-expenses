package controller;

import config.SAMLAuthentication;
import eu.openminted.registry.core.controllers.SearchController;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.SearchService;
import gr.athenarc.domain.Request;
import gr.athenarc.domain.User;
import io.swagger.annotations.Api;
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

import javax.xml.ws.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public Paging<Request> getAll(@RequestParam(value = "from",required=false) String from,
                         @RequestParam(value = "quantity",required=false) String quantity,
                         @RequestParam(value = "order",required=false) String orderD,
                         @RequestParam(value = "orderField",required=false) String orderF,
                         @RequestParam(value = "email") String email) {



        return searchService.cqlQuery(requestService.createQuery(email),"request",Integer.parseInt(quantity),Integer.parseInt(from));

       /* FacetFilter filter = new FacetFilter();
        filter.setResourceType("request");
        filter.setKeyword(keyword != null ? keyword : "");
        filter.setFrom(from != null ? Integer.parseInt(from) : 0);
        filter.setQuantity(quantity != null ? Integer.parseInt(quantity) : 10);

        Map<String,Object> sort = new HashMap<>();
        Map<String,Object> order = new HashMap<>();

        String orderDirection = orderD != null ? orderD : "asc";
        String orderField = orderF != null ? orderF : null;

        if (orderField != null) {
            order.put("order",orderDirection);
            sort.put(orderField, order);
            filter.setOrderBy(sort);
        }
//        searchFilter.setFilter(null);
        Browsing<Request> browsing = requestService.getAll(filter);
        browsing.setResults(policyCheckerService.searchFilter(browsing.getResults(),email));
        return browsing;*/





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
    ResponseEntity<Boolean> isEditable(@RequestBody Request request) {
        SAMLAuthentication authentication = (SAMLAuthentication) SecurityContextHolder.getContext().getAuthentication();

        if(policyCheckerService.updateFilter(request,authentication.getEmail()))
            return new ResponseEntity<>( true, HttpStatus.OK);
        return new ResponseEntity<>( false, HttpStatus.BAD_REQUEST);
    }




}