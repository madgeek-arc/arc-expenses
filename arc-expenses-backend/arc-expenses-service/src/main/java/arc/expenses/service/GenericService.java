package arc.expenses.service;

import arc.expenses.acl.ArcPermission;
import arc.expenses.mail.JavaMailer;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.service.AbstractGenericService;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.ResourceCRUDService;
import eu.openminted.registry.core.service.SearchService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.domain.AclImpl;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

public abstract class GenericService<T> extends AbstractGenericService<T> implements ResourceCRUDService<T, Authentication> {

    private static Logger logger = LogManager.getLogger(GenericService.class);


    public GenericService(Class<T> typeParameterClass) {
        super(typeParameterClass);
    }

    @Override
    public String getResourceType() {
        return null;
    }

    @Autowired
    SearchService searchService;

    @Override
    public T get(String id) {
        try {
            String id_field = String.format("%s_id", resourceType.getName());
            Resource resource = searchService.searchId(resourceType.getName(),
                    new SearchService.KeyValue(id_field,id));
            if(resource != null)
                return parserPool.deserialize(resource,typeParameterClass);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Browsing<T> getAll(FacetFilter facetFilter, Authentication u) {
        return getResults(facetFilter);
    }

    @Override
    public Browsing<T> getMy(FacetFilter facetFilter, Authentication u) {
        return null;
    }

    @Override
    public T add(T t, Authentication u) {
        logger.info("Creating a new " + typeParameterClass.getName() + " object");

        String serialized = null;

        serialized = parserPool.serialize(t, ParserService.ParserServiceTypes.JSON);
        Resource created = new Resource();
        created.setPayload(serialized);
        created.setResourceType(resourceType);
        resourceService.addResource(created);
        return t;
    }

    @Override
    public T update(T t, Authentication u) throws ResourceNotFoundException {
       return null;
    }

    @Override
    public void delete(T t) throws ResourceNotFoundException {

    }

    public T getByField(String key, String value) throws Exception {
        Resource resource;
        try {
            resource =  searchService.searchId(resourceType.getName(), new SearchService.KeyValue(key,value));
            if(resource != null)
                return parserPool.deserialize(resource, typeParameterClass);
            return null;
        } catch (UnknownHostException e) {
            throw new Exception("Null resource!");
        }
    }



    public T update(T t,String id) throws ResourceNotFoundException {
        String serialized = null;
        try {
            serialized = parserPool.serialize(t, ParserService.ParserServiceTypes.JSON);
            Resource existing = searchService.searchId(resourceType.getName(),
                    new SearchService.KeyValue(String.format("%s_id", resourceType.getName()),id));
            existing.setResourceType(resourceType);
            existing.setPayload(serialized);
            resourceService.updateResource(existing);
            return t;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }


    public T update(T t,String id, String fieldAsId) throws ResourceNotFoundException {
        String serialized = null;
        try {
            serialized = parserPool.serialize(t, ParserService.ParserServiceTypes.JSON);
            Resource existing = searchService.searchId(resourceType.getName(),
                    new SearchService.KeyValue(String.format("%s_"+fieldAsId, resourceType.getName()),id));
            existing.setPayload(serialized);
            existing.setResourceType(resourceType);
            resourceService.updateResource(existing);
            return t;
        } catch ( UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

}
