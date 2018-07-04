package arc.expenses.controller;

import arc.expenses.service.PolicyCheckerService;
import arc.expenses.service.RequestServiceImpl;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.SearchService;
import gr.athenarc.domain.Attachment;
import gr.athenarc.domain.Request;
import io.swagger.annotations.Api;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
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
    @PostAuthorize("@annotationChecks.isValidRequest(returnObject,authentication.principal)")
    public Request getById(@PathVariable("id") String id) throws ResourceNotFoundException {
        Request request = requestService.get(id);
        if(request == null)
            throw new ResourceNotFoundException();
        return request;
    }

    @RequestMapping(value =  "/getAll", method = RequestMethod.GET)
    public Paging<Request> getAll(@RequestParam(value = "from",required=false,defaultValue = "0") String from,
                         @RequestParam(value = "quantity",required=false,defaultValue = "10") String quantity,
                         @RequestParam(value = "status") List<String> status,
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
    synchronized Request addRequest(@RequestBody Request request) {
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

    @RequestMapping(value = "/store/download", method = RequestMethod.GET)
    @ResponseBody
    @PreAuthorize("@annotationChecks.validateDownload(#requestId,authentication.principal)")
    public void downloadFile(@RequestParam("requestId") String requestId,
                             @RequestParam("stage") String stage,
                             HttpServletResponse response) throws IOException, ResourceNotFoundException {
        Attachment attachment = requestService.getAttachment(requestId,stage);

        if(attachment == null)
            throw new ResourceNotFoundException();

        response.setContentType(attachment.getMimetype());
        response.setHeader("Content-Disposition", "attachment; filename=\"" + attachment.getFilename() + "\"");
        IOUtils.copyLarge(requestService.downloadFile(requestId,stage), response.getOutputStream());
    }

}