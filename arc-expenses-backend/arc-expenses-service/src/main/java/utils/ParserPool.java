package utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.ServiceException;
import gr.athenarc.domain.Project;
import gr.athenarc.domain.Request;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static javax.xml.bind.JAXBContext.newInstance;


@Component("parserPool")
public class ParserPool  implements ParserService {

    private ExecutorService executor;

    private static Logger logger = LogManager.getLogger(ParserPool.class);

    private JAXBContext jaxbContext = null;

    public ParserPool() {
        executor = Executors.newCachedThreadPool();
        try {
            jaxbContext = newInstance(Request.class, Project.class);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }


    @SuppressWarnings("unchecked")
    @Override
    public <T> Future<T> deserialize(Resource resource, Class<T> returnType) {
        return executor.submit(() -> {
            T type;
            if (resource == null) {
                throw new ServiceException("null resource");
            }
            try {
                if (resource.getPayloadFormat().equals("xml")) {
                    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                    type = (T) unmarshaller.unmarshal(new StringReader(resource.getPayload()));
                } else if (resource.getPayloadFormat().equals("json")) {
                    ObjectMapper mapper = new ObjectMapper();
                    type = mapper.readValue(resource.getPayload(), returnType);
                } else {
                    throw new ServiceException("Unsupported media type");
                }
            } catch (JAXBException je) {
                throw new ServiceException(je);
            }
            return type;
        });
    }

    @Override
    public <T> Future<T> deserialize(String json, Class<T> returnType) throws IOException {
        return executor.submit(() -> {
            ObjectMapper mapper = new ObjectMapper();

            return mapper.readValue(json, returnType);
        });
    }

    @Override
    public Resource deserializeResource(File file, ParserServiceTypes mediaType) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            if (mediaType == ParserServiceTypes.JSON)
                return mapper.readValue(file, Resource.class);
            else if (mediaType == ParserServiceTypes.XML) {
                Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                return (Resource) unmarshaller.unmarshal(file);
            } else
                return null;
        } catch (IOException | ClassCastException e) {
            return null;
        } catch (JAXBException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Future<String> serialize(Object resource, ParserServiceTypes mediaType) {
        return executor.submit(() -> {
            if (mediaType == ParserServiceTypes.XML) {
                Marshaller marshaller = jaxbContext.createMarshaller();
                StringWriter sw = new StringWriter();
                marshaller.marshal(resource, sw);
                return sw.toString();
            } else if (mediaType == ParserServiceTypes.JSON) {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.writeValueAsString(resource);
            } else {
                throw new ServiceException("Unsupported media type");
            }
        });
    }
}
