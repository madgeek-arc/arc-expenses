package arc.expenses.controller;

import arc.expenses.domain.RequestSummary;
import arc.expenses.service.PolicyCheckerService;
import arc.expenses.service.RequestApprovalServiceImpl;
import arc.expenses.service.RequestPaymentServiceImpl;
import arc.expenses.service.RequestServiceImpl;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.SearchService;
import gr.athenarc.domain.Attachment;
import gr.athenarc.domain.Request;
import gr.athenarc.domain.RequestApproval;
import gr.athenarc.domain.RequestPayment;
import io.swagger.annotations.Api;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

    @Autowired
    RequestApprovalServiceImpl requestApprovalService;

    @Autowired
    RequestPaymentServiceImpl requestPaymentService;


    @RequestMapping(value =  "/getById/{id}", method = RequestMethod.GET)
    //@PostAuthorize("@annotationChecks.isValidRequest(returnObject,authentication.principal)")
    public Request getById(@PathVariable("id") String id) throws ResourceNotFoundException {
        Request request = requestService.get(id);
        if(request == null)
            throw new ResourceNotFoundException();
        return request;
    }

    @RequestMapping(value =  "/getAll", method = RequestMethod.GET)
    public Paging<RequestSummary> getAllRequests(@RequestParam(value = "from",required=false,defaultValue = "0") String from,
                                         @RequestParam(value = "quantity",required=false,defaultValue = "10") String quantity,
                                         @RequestParam(value = "status") List<String> status,
                                         @RequestParam(value = "searchField",required=false) String searchField,
                                         @RequestParam(value = "stage") List<String> stage,
                                         @RequestParam(value = "order",required=false,defaultValue = "ASC") String orderType,
                                         @RequestParam(value = "orderField") String orderField,
                                         @RequestParam(value = "email") String email) {

        return requestService.criteriaSearch(from,quantity,status,searchField,stage,orderType,orderField,email);

    }

    @RequestMapping(value = "/addRequest", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)

    @ResponseBody
    synchronized Request addRequest(@RequestBody Request request, Authentication auth) {
        request.setId(requestService.generateID());
        request.setArchiveId(requestService.createArchive());
        return requestService.add(request, auth);
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
    ResponseEntity<Boolean> isEditable(@RequestBody RequestSummary requestSummary,
                                       @RequestParam("email") String email) {


        if(policyCheckerService.updateFilter(requestSummary,email))
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
                                             @RequestParam("mode") String mode,
                                                @RequestParam("file") MultipartFile file) throws IOException {
        return requestService.upLoadFile(mode,archiveID,stage,file);
    }

    @RequestMapping(value = "/store/download", method = RequestMethod.GET)
    @ResponseBody
    //@PreAuthorize("@annotationChecks.validateDownload(#requestId,authentication.principal)")
    public void downloadFile(@RequestParam("mode") String mode,
                             @RequestParam("requestId") String id,
                             @RequestParam("stage") String stage,
                             HttpServletResponse response) throws IOException, ResourceNotFoundException {
        Attachment attachment = requestService.getAttachment(mode,id,stage);

        if(attachment == null)
            throw new ResourceNotFoundException();

        response.setContentType(attachment.getMimetype());
        response.setHeader("Content-Disposition", "attachment; filename=\"" + attachment.getFilename() + "\"");
        IOUtils.copyLarge(requestService.downloadFile(mode,id,stage), response.getOutputStream());
    }

    @RequestMapping(value = "/addRequestApproval", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    synchronized RequestApproval addRequestApproval(@RequestBody RequestApproval requestApproval, Authentication auth) {
        requestApproval.setId(requestApprovalService.generateID(requestApproval.getRequestId()));
        return requestApprovalService.add(requestApproval,auth);
    }

    @RequestMapping(value = "/updateRequestApproval", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestApproval updateRequestApproval(@RequestBody RequestApproval requestApproval) throws ResourceNotFoundException {
        return requestApprovalService.update(requestApproval,requestApproval.getId());
    }

    @RequestMapping(value = "/addRequestPayment", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)

    @ResponseBody
    synchronized RequestPayment addRequestPayment(@RequestBody RequestPayment requestPayment, Authentication auth) {
        requestPayment.setId(requestPaymentService.generateID(requestPayment.getRequestId()));
        return requestPaymentService.add(requestPayment,auth);
    }

    @RequestMapping(value = "/updateRequestPayment", method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    RequestPayment updateRequestPayment(@RequestBody RequestPayment requestPayment) throws ResourceNotFoundException {
        return requestPaymentService.update(requestPayment,requestPayment.getId());
    }

    @RequestMapping(value =  "/approval/getById/{id}", method = RequestMethod.GET)
    //@PostAuthorize("@annotationChecks.isValidRequest(returnObject,authentication.principal)")
    public RequestApproval getApprovalById(@PathVariable("id") String id) throws ResourceNotFoundException {
        RequestApproval requestApproval = requestApprovalService.get(id);
        if(requestApproval == null)
            throw new ResourceNotFoundException();
        return requestApproval;
    }

    @RequestMapping(value =  "/payment/getById/{id}", method = RequestMethod.GET)
    //@PostAuthorize("@annotationChecks.isValidRequest(returnObject,authentication.principal)")
    public RequestPayment getPaymentById(@PathVariable("id") String id) throws ResourceNotFoundException {
        RequestPayment requestPayment = requestPaymentService.get(id);
        if(requestPayment == null)
            throw new ResourceNotFoundException();
        return requestPayment;
    }

    @RequestMapping(value =  "/payments/getByRequestId/{request_id}", method = RequestMethod.GET)
    //@PostAuthorize("@annotationChecks.isValidRequest(returnObject,authentication.principal)")
    public Browsing<RequestPayment> getPaymentsByRequestId(@PathVariable("request_id") String request_id, Authentication auth) throws Exception {
        return requestPaymentService.getPayments(request_id,auth);
    }
}