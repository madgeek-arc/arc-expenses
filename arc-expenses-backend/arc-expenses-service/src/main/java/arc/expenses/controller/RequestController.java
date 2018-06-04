package arc.expenses.controller;

import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.SearchService;
import gr.athenarc.domain.Request;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import arc.expenses.service.PolicyCheckerService;
import arc.expenses.service.RequestServiceImpl;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
        request.setArchiveId(requestService.createArchive());
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

    @RequestMapping(value =  "/getUserPendingRequests", method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Request> getPendingRequests(@RequestParam(value = "email") String email) {
        return requestService.getPendingRequests(email);
    }

    @RequestMapping(value = "/store/uploadFile", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> uploadFile(@RequestParam("archiveID") String archiveID,
                                             @RequestParam("stage") String stage,
                                             @RequestParam("file") MultipartFile file) throws IOException {
        return requestService.upLoadFile(archiveID,stage,file);
    }
}