package arc.expenses.controller;

import arc.expenses.domain.RequestResponse;
import arc.expenses.service.RequestPaymentServiceImpl;
import arc.expenses.service.RequestServiceImpl;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ServiceException;
import gr.athenarc.domain.Request;
import gr.athenarc.domain.RequestPayment;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(value = "/payment")
@Api(description = "Request API  ",  tags = {"Manage requests"})
public class PaymentController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    RequestServiceImpl requestService;

    @Autowired
    RequestPaymentServiceImpl requestPaymentService;

    @RequestMapping(value =  "/getById/{id}", method = RequestMethod.GET)
    public RequestResponse getPaymentById(@PathVariable("id") String id) throws Exception {
        RequestPayment requestPayment = requestPaymentService.get(id);
        if(requestPayment == null)
            throw new ServiceException("Request payment not found");
        RequestResponse requestResponse = requestPaymentService.getRequestResponse(requestPayment);
        if(requestResponse == null)
            throw new ResourceNotFoundException();
        return requestResponse;
    }

    @ApiOperation("Approve payment")
    @RequestMapping(value = "/approve/{paymentId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity approve(
            @PathVariable("paymentId") String paymentId,
            HttpServletRequest req){

        RequestPayment requestPayment = requestPaymentService.get(paymentId);

        if(requestPayment==null)
            throw new ServiceException("Payment not found");
        try {
            requestPaymentService.approve(requestPayment, req);
        }catch (Exception ex){
            ex.printStackTrace();
            throw new ServiceException(ex.getMessage());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation("Reject request")
    @RequestMapping(value = "/reject/{paymentId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity reject(
            @PathVariable("paymentId") String paymentId,
            HttpServletRequest req
    ){

        RequestPayment requestPayment = requestPaymentService.get(paymentId);

        if(requestPayment==null)
            throw new ServiceException("Payment not found");
        try {
            requestPaymentService.reject(requestPayment, req);
        }catch (Exception ex){
            ex.printStackTrace();
            throw new ServiceException(ex.getMessage());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation("Downgrade request")
    @RequestMapping(value = "/downgrade/{paymentId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity downgrade(
            @PathVariable("paymentId") String paymentId,
            HttpServletRequest req
    ){
        RequestPayment requestPayment = requestPaymentService.get(paymentId);

        if(requestPayment==null)
            throw new ServiceException("Payment not found");
        try {
            requestPaymentService.downgrade(requestPayment, req);
        }catch (Exception ex){
            ex.printStackTrace();
            throw new ServiceException(ex.getMessage());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation("Edit request")
    @RequestMapping(value = "/edit/{paymentId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity edit(
            @PathVariable("paymentId") String paymentId,
            HttpServletRequest req
    ){
        RequestPayment requestPayment = requestPaymentService.get(paymentId);

        if(requestPayment==null)
            throw new ServiceException("Payment not found");

        try {
            requestPaymentService.edit(requestPayment, req);
        }catch (Exception ex){
            ex.printStackTrace();
            throw new ServiceException(ex.getMessage());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ApiOperation("Cancel payment")
    @RequestMapping(value = "/cancel/{paymentId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity cancel(
            @PathVariable("paymentId") String paymentId,
            HttpServletRequest req
    ){
        RequestPayment requestPayment = requestPaymentService.get(paymentId);

        if(requestPayment==null)
            throw new ServiceException("Payment not found");
        try {
            return new ResponseEntity<>("{\"id\":\""+requestPaymentService.cancel(requestPayment, req)+"\"}",HttpStatus.OK);
        }catch (Exception ex){
            ex.printStackTrace();
            throw new ServiceException(ex.getMessage());
        }

    }

    @ApiOperation("Add payment")
    @RequestMapping(value = "/add/{requestId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity cancel(
            @PathVariable("requestId") String requestId
    ){
        Request request = requestService.get(requestId);
        if(request==null)
            throw new ServiceException("Request not found");
        try {
            RequestPayment requestPayment = requestPaymentService.createPayment(request);
            return new ResponseEntity<>("{\"id\":\""+requestPayment.getId()+"\"}",HttpStatus.OK);
        }catch (Exception ex){
            ex.printStackTrace();
            throw new ServiceException(ex.getMessage());
        }
    }

}