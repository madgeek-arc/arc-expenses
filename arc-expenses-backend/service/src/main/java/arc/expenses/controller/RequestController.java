package arc.expenses.controller;

import arc.expenses.domain.OrderByField;
import arc.expenses.domain.OrderByType;
import arc.expenses.domain.RequestResponse;
import arc.expenses.domain.RequestSummary;
import arc.expenses.service.RequestApprovalServiceImpl;
import arc.expenses.service.RequestPaymentServiceImpl;
import arc.expenses.service.RequestServiceImpl;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ParserPool;
import eu.openminted.registry.core.service.SearchService;
import eu.openminted.registry.core.service.ServiceException;
import eu.openminted.store.restclient.StoreRESTClient;
import gr.athenarc.domain.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(value = "/request")
@Api(description = "Request API  ",  tags = {"Manage requests"})
public class RequestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestController.class);

    @Autowired
    private StoreRESTClient storeRESTClient;

    @Autowired
    RequestServiceImpl requestService;

    @Autowired
    SearchService searchService;

    @Autowired
    ParserPool parserPool;

    @Autowired
    RequestApprovalServiceImpl requestApprovalService;

    @Autowired
    RequestPaymentServiceImpl requestPaymentService;


    @ApiOperation("Finalize request approval")
    @RequestMapping(value = "/finalize/{requestId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity finalize(
            @PathVariable("requestId") String requestId) throws Exception {

        RequestApproval requestApproval = requestApprovalService.getApproval(requestId);
        if(requestApproval==null)
            throw new ServiceException("Request approval not found");
        try {
            requestApprovalService.finalize(requestApproval);
        }catch (Exception ex){
            ex.printStackTrace();
            throw new ServiceException(ex.getMessage());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation("Approve request approval")
    @RequestMapping(value = "/approve/{requestId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity approve(
            @PathVariable("requestId") String requestId,
            HttpServletRequest req) throws Exception {

        RequestApproval requestApproval = requestApprovalService.getApproval(requestId);
        if(requestApproval==null)
            throw new ServiceException("Request approval not found");
        try {
            requestApprovalService.approve(requestApproval ,req);
        }catch (Exception ex){
            ex.printStackTrace();
            throw new ServiceException(ex.getMessage());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation("Reject request approval ")
    @RequestMapping(value = "/reject/{requestId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity reject(
            @PathVariable("requestId") String requestId,
            HttpServletRequest req
    ) throws Exception {

        RequestApproval requestApproval = requestApprovalService.getApproval(requestId);
        if(requestApproval==null)
            throw new ServiceException("Request approval not found");
        try {
            requestApprovalService.reject(requestApproval,req);
        }catch (Exception ex){
            throw new ServiceException(ex.getMessage());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation("Downgrade request approval")
    @RequestMapping(value = "/downgrade/{requestId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity downgrade(
            @PathVariable("requestId") String requestId,
            HttpServletRequest req
    ) throws Exception {
        RequestApproval requestApproval = requestApprovalService.getApproval(requestId);
        if(requestApproval==null)
            throw new ServiceException("Request approval not found");
        try {
            requestApprovalService.downgrade(requestApproval,req);
        }catch (Exception ex){
            throw new ServiceException(ex.getMessage());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation("Cancel request approval")
    @RequestMapping(value = "/cancel/{requestId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity cancel(
            @PathVariable("requestId") String requestId) throws Exception {

        RequestApproval requestApproval = requestApprovalService.getApproval(requestId);
        if(requestApproval==null)
            throw new ServiceException("Request approval not found");
        requestApprovalService.cancel(requestApproval);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation("Add request")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "projectId", value = "Id of project of request", required = true, dataType = "string", paramType = "form"),
            @ApiImplicitParam(name = "requester_position", value = "The position of requester", required = true, dataType = "string", paramType = "form"),
            @ApiImplicitParam(name = "subject", value = "Subject of request", required = true, dataType = "string", paramType = "form"),
            @ApiImplicitParam(name = "type", value = "Request's type", required = true, dataType = "string", paramType = "form"),
            @ApiImplicitParam(name = "supplier", value = "Who is the supplier of the request", dataType = "string", paramType = "form"),
            @ApiImplicitParam(name = "supplier_selection_method", value = "The method of supply", dataType = "string", paramType = "form"),
            @ApiImplicitParam(name = "amount", value = "Money of request", required = true, dataType = "double", paramType = "form"),
            @ApiImplicitParam(name = "attachments", value = "Attachments about request", dataType = "file", paramType = "form"),
            @ApiImplicitParam(name = "destination", value = "Destination for trip (if such request)", dataType = "string", paramType = "form"),
            @ApiImplicitParam(name = "firstName", value = "First name of the yoloman (if such request)", dataType = "string", paramType = "form"),
            @ApiImplicitParam(name = "lastName", value = "Last name of the yoloman (if such request)", dataType = "string", paramType = "form"),
            @ApiImplicitParam(name = "email", value = "Email of the yoloman (if such request)", dataType = "string", paramType = "form")
    })
    @RequestMapping(value = "/add", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Request addRequest(
            @RequestParam(value = "projectId") String projectId,
            @RequestParam(value = "onBehalf", required = false, defaultValue = "false") boolean onBehalf,
            @RequestParam(value = "requester_position") Request.RequesterPosition requesterPosition,
            @RequestParam(value = "subject") String subject,
            @RequestParam(value = "type") Request.Type type,
            @RequestParam(value = "supplier", required = false, defaultValue = "") String supplier,
            @RequestParam(value = "supplier_selection_method", required = false, defaultValue = "") Stage1.SupplierSelectionMethod supplierSelectionMethod,
            @RequestParam(value = "amount") double amount,
            @RequestParam(value = "cycles", required = false, defaultValue = "0") int cycles,
            @RequestParam(value = "attachments") Optional<List<MultipartFile>> files,
            @RequestParam(value = "destination", required = false, defaultValue = "") String destination,
            @RequestParam(value = "firstName", required = false, defaultValue = "") String firstName,
            @RequestParam(value = "lastName", required = false, defaultValue = "") String lastName,
            @RequestParam(value = "email", required = false, defaultValue = "") String email
    ) throws Exception {
        return requestService.add(type,projectId,subject,requesterPosition,supplier,supplierSelectionMethod,amount,files,destination,firstName,lastName,email, cycles, onBehalf);
    }

    @RequestMapping(value =  "/getById/{id}", method = RequestMethod.GET)
    public Request getById(@PathVariable("id") String id) throws ResourceNotFoundException {
        Request request = requestService.get(id);
        if(request == null)
            throw new ResourceNotFoundException();
        return request;
    }

    @RequestMapping(value =  "/getAll", method = RequestMethod.GET)
    public Paging<RequestSummary> getAllRequests(@RequestParam(value = "from",required=false,defaultValue = "0") int from,
                                                 @RequestParam(value = "quantity",required=false,defaultValue = "10") int quantity,
                                                 @RequestParam(value = "status") List<BaseInfo.Status> status,
                                                 @RequestParam(value = "type") List<Request.Type> type,
                                                 @RequestParam(value = "searchField",required=false, defaultValue = "") String searchField,
                                                 @RequestParam(value = "stage") List<String> stage,
                                                 @RequestParam(value = "order",required=false,defaultValue = "ASC") OrderByType orderType,
                                                 @RequestParam(value = "orderField") OrderByField orderField,
                                                 @RequestParam(value = "editable", required = false, defaultValue = "false") boolean canEdit) {

        return requestService.criteriaSearch(from,quantity,status,type,searchField,stage,orderType,orderField, canEdit);

    }

    /*////////////////////////////////////////////////////////////////////////////////////////
                                        OLD STUFF
      ////////////////////////////////////////////////////////////////////////////////////////
     */

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
    public void downloadFile(@RequestParam("archiveId") String archiveId,
                               @RequestParam("filename") String filename,
                               HttpServletResponse response) throws IOException {
        Request request = new Request();

        List rs = searchService.cqlQuery("searchableArea=*"+ archiveId.substring(0,archiveId.lastIndexOf("/"))+"*","*").getResults();
        Resource resource;
        if(rs.size() > 0) {
            resource = (Resource) rs.get(0);
            if(resource.getResourceTypeName().equals("approval"))
                request = requestService.get(parserPool.deserialize(resource, RequestApproval.class).getRequestId());
            else if(resource.getResourceTypeName().equals("payment"))
                request = requestService.get(parserPool.deserialize(resource, RequestPayment.class).getRequestId());
            else if (resource.getResourceTypeName().equals("request"))
                request = parserPool.deserialize(resource, Request.class);
        }else{
            request = null;
        }
        File temp = File.createTempFile(filename, "tmp");
        temp = requestService.downloadFile(temp,request,archiveId+"/"+filename);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        IOUtils.copyLarge(new FileInputStream(temp), response.getOutputStream());
    }


    @RequestMapping(value =  "/approval/getById/{id}", method = RequestMethod.GET)
    public ResponseEntity<RequestResponse> getApprovalById(@PathVariable("id") String id) throws Exception {
        return new ResponseEntity(requestApprovalService.getRequestResponse(id),HttpStatus.OK);
    }

    @RequestMapping(value =  "/payments/getByRequestId/{request_id}", method = RequestMethod.GET)
//    @PostAuthorize("@annotationChecks.isValidRequest(returnObject,authentication.principal)")
    public Browsing<RequestPayment> getPaymentsByRequestId(@PathVariable("request_id") String request_id, Authentication auth) throws Exception {
        return requestPaymentService.getPayments(request_id,auth);
    }
}