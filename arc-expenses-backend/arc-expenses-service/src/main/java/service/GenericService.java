package service;

import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.AbstractGenericService;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.ResourceCRUDService;
import eu.openminted.registry.core.service.SearchService;

import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

public abstract class GenericService<T> extends AbstractGenericService<T> implements ResourceCRUDService<T> {


    public GenericService(Class<T> typeParameterClass) {
        super(typeParameterClass);
    }

    @Override
    public String getResourceType() {
        return null;
    }

    @Override
    public T get(String id) {
        try {
            String id_field = String.format("%s_id", resourceType.getName());
            Resource resource = searchService.searchId(resourceType.getName(),
                    new SearchService.KeyValue(id_field,id));
            return parserPool.deserialize(resource,typeParameterClass).get();
        } catch (UnknownHostException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Browsing<T> getAll(FacetFilter facetFilter) {
        return null;
    }

    @Override
    public Browsing<T> getMy(FacetFilter facetFilter) {
        return null;
    }

    @Override
    public T add(T t) {
        String serialized = null;

        try {
            serialized = parserPool.serialize(t, ParserService.ParserServiceTypes.JSON).get();
            Resource created = new Resource();
            created.setPayload(serialized);
            created.setResourceType(resourceType);
            resourceService.addResource(created);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public T update(T t) throws ResourceNotFoundException {
       return null;
    }

    @Override
    public void delete(T t) throws ResourceNotFoundException {

    }

    public T getByField(String key, String value) throws UnknownHostException, ExecutionException, InterruptedException {
        Resource resource;
        try {
            resource =  searchService.searchId(resourceType.getName(), new SearchService.KeyValue(key,value));
            return parserPool.deserialize(resource, typeParameterClass).get();
        } catch (UnknownHostException | InterruptedException | ExecutionException e) {
            throw e;
        }
    }


    public T update(T t,String id) throws ResourceNotFoundException {
        String serialized = null;
        try {
            serialized = parserPool.serialize(t, ParserService.ParserServiceTypes.JSON).get();
            Resource existing = searchService.searchId(resourceType.getName(),
                    new SearchService.KeyValue(String.format("%s_id", resourceType.getName()),id));
            existing.setPayload(serialized);
            resourceService.updateResource(existing);
            return t;
        } catch (InterruptedException | ExecutionException | UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

}
