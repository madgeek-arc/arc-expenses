package service;

import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.ResourceCRUDService;
import eu.openminted.registry.core.service.SearchService;
import gr.athenarc.request.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("requestService")
public class RequestServiceImpl implements ResourceCRUDService<Request> {

    @Autowired
    SearchService searchService;

    @Override
    public Request get(String s) {
        return null;
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
        return null;
    }

    @Override
    public Request update(Request request) throws ResourceNotFoundException {
        return null;
    }

    @Override
    public void delete(Request request) throws ResourceNotFoundException {

    }
}
