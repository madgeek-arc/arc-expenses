package arc.expenses.listener;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.monitor.ResourceListener;
import gr.athenarc.domain.Request;
import arc.expenses.mail.EmailMessage;
import arc.expenses.mail.JavaMailer;
import arc.expenses.messages.StageMessages;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import arc.expenses.utils.ParserPool;

import java.util.List;
import java.util.concurrent.ExecutionException;


@Component
public class StageResourceListener implements ResourceListener {

    private Logger logger = Logger.getLogger(StageResourceListener.class);

    @Autowired
    ParserPool parserPool;

    @Autowired
    JavaMailer javaMailer;

    @Autowired
    StageMessages stageMessages;

    @Override
    public void resourceAdded(Resource resource) {
        // TODO
        logger.info("Adding a resource");
        if (resource.getResourceType().getName().equals("request")) {
            try {
                Request request = parserPool.deserialize(resource, Request.class).get();
                logger.info("New Request added: stage = " + request.getStage() + " " + request.getStatus());
                if (request.getStage().equals("2")) {
                    List<EmailMessage> emails = stageMessages
                            .createMessages(null, request.getStage(), request);
                    emails.forEach(e -> javaMailer.sendEmail(e.getRecipient(), e.getSubject(), e.getText()));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void resourceUpdated(Resource previousResource, Resource newResource) {
        logger.info("Updating a resource");

        if ("request".equals(newResource.getResourceType().getName())) {
            try {
                Request previousRequest = parserPool.deserialize(previousResource, Request.class).get();
                Request newRequest = parserPool.deserialize(newResource, Request.class).get();
                if (!previousRequest.getStage().equals(newRequest.getStage()) ||
                        !previousRequest.getStatus().equals(newRequest.getStatus())) {
                    logger.info("Stage changed: " + previousRequest.getStage() + " " + previousRequest.getStatus() +
                            " -> " + newRequest.getStage() + " " + newRequest.getStatus());
                    logger.info("Prev Request: " + previousRequest.toString());
                    logger.info("New Request : " + newRequest.toString());
                    List<EmailMessage> emails = stageMessages
                            .createMessages(previousRequest.getStage(), newRequest.getStage(), newRequest);
                    emails.forEach(e -> javaMailer.sendEmail(e.getRecipient(), e.getSubject(), e.getText()));
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void resourceDeleted(Resource resource) {
        // TODO
        logger.info("Deleting a resource");
    }
}