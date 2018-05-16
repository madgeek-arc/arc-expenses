package listener;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.monitor.ResourceListener;
import gr.athenarc.domain.Request;
import mail.JavaMailer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import utils.ParserPool;

import java.util.concurrent.ExecutionException;

@Component
public class StageResourceListener implements ResourceListener {

    private Logger logger = LoggerFactory.getLogger(StageResourceListener.class);

    @Autowired
    ParserPool parserPool;

    @Autowired
    JavaMailer javaMailer;

    @Override
    public void resourceAdded(Resource resource) {
        // TODO
        logger.info("Adding a resource");
    }

    @Override
    public void resourceUpdated(Resource previousResource, Resource newResource) {
        logger.info("Updating a resource");
        try {
            Request previousRequest = parserPool.deserialize(previousResource, Request.class).get();
            Request newRequest = parserPool.deserialize(previousResource, Request.class).get();
            if (previousRequest.getStage() != newRequest.getStage()) {
                logger.info("Stage changed from '"+previousRequest.getStage()+"' to '"+newRequest.getStage()+"'");
//                javaMailer.sendEmail();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void resourceDeleted(Resource resource) {
        // TODO
        logger.info("Deleting a resource");
    }
}