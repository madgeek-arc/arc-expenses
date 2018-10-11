package arc.expenses.listener;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.monitor.ResourceListener;
import gr.athenarc.domain.Request;
import arc.expenses.mail.JavaMailer;
import arc.expenses.messages.StageMessages;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import arc.expenses.utils.ParserPool;

import java.util.concurrent.ExecutionException;


@Component
public class StageResourceListener implements ResourceListener {

    final private static Logger logger = LogManager.getLogger(StageResourceListener.class);

    @Autowired
    ParserPool parserPool;

    @Autowired
    JavaMailer javaMailer;

    @Autowired
    StageMessages stageMessages;

    @Async
    @Override
    public void resourceAdded(Resource resource) {
        logger.debug("Adding a resource");

        if (resource.getResourceType().getName().equals("request")) {
            try {
                Request request = parserPool.deserialize(resource, Request.class).get();
                logger.info("New Request added: stage = " + request.getStage() + " " + request.getStatus());
                if (request.getStage().equals("2")) {
                    stageMessages
                            .createStageMessages("1", request.getStage(), request)
                            .forEach(e -> javaMailer.sendEmail(e.getRecipient(), e.getSubject(), e.getText()));
                }
            } catch (InterruptedException | ExecutionException e) {
                logger.error(e);
            }
        }
    }

    @Async
    @Override
    public void resourceUpdated(Resource previousResource, Resource newResource) {
        logger.info("Updating a resource");

        if ("request".equals(newResource.getResourceType().getName())) {
            try {
                Request previousRequest = parserPool.deserialize(previousResource, Request.class).get();
                Request newRequest = parserPool.deserialize(newResource, Request.class).get();
                if (!previousRequest.getStage().equals(newRequest.getStage()) ||
                        !previousRequest.getStatus().equals(newRequest.getStatus())) {
                    logger.debug("Stage changed: " + previousRequest.getStage() + " " + previousRequest.getStatus() +
                            " -> " + newRequest.getStage() + " " + newRequest.getStatus());
                    logger.debug("Prev Request: " + previousRequest.toString());
                    logger.debug("New Request : " + newRequest.toString());
                    stageMessages
                            .createStageMessages(previousRequest.getStage(), newRequest.getStage(), newRequest)
                            .forEach(e -> javaMailer.sendEmail(e.getRecipient(), e.getSubject(), e.getText()));
                }
            } catch (InterruptedException | ExecutionException e) {
                logger.error(e);
            }
        }
    }

    @Async
    @Override
    public void resourceDeleted(Resource resource) {
        // TODO
        logger.info("Deleting a resource");
    }
}