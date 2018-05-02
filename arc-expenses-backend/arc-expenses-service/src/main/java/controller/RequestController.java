package controller;

import eu.openminted.registry.core.exception.ResourceNotFoundException;
import gr.athenarc.domain.Request;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import service.RequestServiceImpl;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/request")
@Api(description = "Request API  ",  tags = {"Manage requests"})
public class RequestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestController.class);

    @Autowired
    RequestServiceImpl requestService;


    @RequestMapping(value =  "/getById/{id}", method = RequestMethod.GET)
    public Request getById(@PathVariable("id") String id) {
        return requestService.get(id);
    }

    @RequestMapping(value =  "/getAll", method = RequestMethod.GET)
    public List<Request> getAll() {
        return null;
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


}