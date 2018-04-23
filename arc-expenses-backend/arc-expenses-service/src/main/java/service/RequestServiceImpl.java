package service;

import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.*;

import eu.openminted.registry.core.validation.ResourceValidator;
import exception.ResourceException;
import gr.athenarc.request.Request;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import utils.ParserPool;

import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

@Service("requestService")
public class RequestServiceImpl extends AbstractGenericService<Request> implements ResourceCRUDService<Request> {

    @Autowired
    SearchService searchService;

    @Autowired
    ParserPool parserPool;

    @Autowired
    ResourceService resourceService;

    @Autowired
    ResourceValidator resourceValidator;

    private Logger LOGGER = Logger.getLogger(RequestServiceImpl.class);

    public RequestServiceImpl(Class<Request> typeParameterClass) {
        super(Request.class);
    }

    public RequestServiceImpl(){
        super(Request.class);
    }

    @Override
    public Request get(String id){
        Request request;
        try {
            request = parserPool.deserialize(searchService.searchId("request",
                    new SearchService.KeyValue("request_id",id)), Request.class).get();
        } catch (UnknownHostException | ExecutionException | InterruptedException e) {
            LOGGER.fatal(e);
            throw new ServiceException(e);
        }
        return request;
    }

    @Override
    public Browsing<Request> getAll(FacetFilter facetFilter) {
        return null;
    }

    @Override
    public Browsing<Request> getMy(FacetFilter facetFilter) {
        return null;
    }


    @Override
    public Request add(Request request) {
        String serialized = null;

        if (get(request.getId()) != null) {
            throw new ResourceException(String.format("%s already exists!", resourceType.getName()), HttpStatus.CONFLICT);
        }

        try {
            serialized = parserPool.serialize(request, ParserService.ParserServiceTypes.XML).get();
            Resource created = new Resource();
            created.setPayload(serialized);
            created.setResourceType(resourceType);
            resourceService.addResource(created);
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.fatal(e);
        }
        return request;
    }


    @Override
    public Request update(Request request) throws ResourceNotFoundException {

        String serialized = null;
        try {
            serialized = parserPool.serialize(request, ParserService.ParserServiceTypes.XML).get();
            Resource existing = searchService.searchId("request", new SearchService.KeyValue("request_id",request.getId()));
            existing.setPayload(serialized);
            resourceService.updateResource(existing);
        } catch (InterruptedException | ExecutionException | UnknownHostException e) {
            LOGGER.fatal(e);
        }
        return request;
    }

    @Override
    public void delete(Request request) throws ResourceNotFoundException {

    }

    @Override
    public String getResourceType() {
        return "request";
    }
}
