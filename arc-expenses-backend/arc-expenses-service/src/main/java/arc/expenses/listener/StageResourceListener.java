package arc.expenses.listener;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.monitor.ResourceListener;
import gr.athenarc.domain.Request;
import arc.expenses.mail.EmailMessage;
import arc.expenses.mail.JavaMailer;
import arc.expenses.messages.StageMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import arc.expenses.utils.ParserPool;

import java.util.List;
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
        if (resource.getResourceType().getName().equals("request")) {
            try {
                Request request = parserPool.deserialize(resource, Request.class).get();
                if (request.getStage().equals("2")) {
                    StageMessages stageMessages = new StageMessages();
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

        if (newResource.getResourceType().getName().equals("request")) {
            try {
                Request previousRequest = parserPool.deserialize(previousResource, Request.class).get();
                Request newRequest = parserPool.deserialize(previousResource, Request.class).get();
                if (previousRequest.getStage() != newRequest.getStage()) {
                    logger.info("Stage changed from '" + previousRequest.getStage() + "' to '" + newRequest.getStage() + "'");
                    StageMessages stageMessages = new StageMessages();
                    List<EmailMessage> emails = stageMessages
                            .createMessages(previousRequest.getStage(), newRequest.getStage(), newRequest);
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
    public void resourceDeleted(Resource resource) {
        // TODO
        logger.info("Deleting a resource");
    }
}