package arc.expenses.listener;

import arc.expenses.mail.JavaMailer;
import arc.expenses.messages.StageMessages;
import arc.expenses.utils.Converter;
import arc.expenses.utils.ParserPool;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.monitor.ResourceListener;
import gr.athenarc.domain.BaseInfo;
import gr.athenarc.domain.RequestApproval;
import gr.athenarc.domain.RequestPayment;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

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

        if (resource.getResourceType().getName().equals("requestApproval")) {
            try {
                BaseInfo baseInfo = Converter.toBaseInfo(parserPool.deserialize(resource, RequestApproval.class).get());
                logger.info("New Request added: stage = " + baseInfo.getStage() + " " + baseInfo.getStatus());
                //if (baseInfo.getStage().equals("2")) {
                    stageMessages
                            .createStageMessages("1", baseInfo.getStage(), baseInfo)
                            .forEach(e -> javaMailer.sendEmail(e.getRecipient(), e.getSubject(), e.getText()));
               // }
            } catch (InterruptedException | ExecutionException e) {
                logger.error(e);
            }
        }
    }

    @Async
    @Override
    public void resourceUpdated(Resource previousResource, Resource newResource) {
        logger.info("Updating a resource");

        /*if ("requestPayment".equals(newResource.getResourceType().getName())) {
            try {
                RequestPayment previousRequestPayment = parserPool.deserialize(previousResource, RequestPayment.class).get();
                RequestPayment newRequestPayment = parserPool.deserialize(newResource, RequestPayment.class).get();
                if (!previousRequestPayment.getStage().equals(newRequestPayment.getStage()) ||
                        !previousRequestPayment.getStatus().equals(newRequestPayment.getStatus())) {
                    logger.debug("Stage changed: " + previousRequestPayment.getStage() + " " + previousRequestPayment.getStatus() +
                            " -> " + newRequestPayment.getStage() + " " + newRequestPayment.getStatus());
                    logger.debug("Prev Request: " + previousRequestPayment.toString());
                    logger.debug("New Request : " + newRequestPayment.toString());
                    stageMessages
                            .createStageMessages(previousRequestPayment.getStage(), newRequestPayment.getStage(), newRequestPayment)
                            .forEach(e -> javaMailer.sendEmail(e.getRecipient(), e.getSubject(), e.getText()));
                }
            } catch (InterruptedException | ExecutionException e) {
                logger.error(e);
            }
        }*/

        BaseInfo previousBaseInfo = null;
        BaseInfo newBaseInfo = null;

        if( "requestPayment".equals(newResource.getResourceType().getName())){
            try {
                previousBaseInfo = Converter.toBaseInfo(parserPool.deserialize(previousResource, RequestPayment.class).get());
                newBaseInfo = Converter.toBaseInfo(parserPool.deserialize(newResource, RequestPayment.class).get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        if( "requestApproval".equals(newResource.getResourceType().getName())){
            try {
                previousBaseInfo = Converter.toBaseInfo(parserPool.deserialize(previousResource, RequestApproval.class).get());
                newBaseInfo = Converter.toBaseInfo(parserPool.deserialize(newResource, RequestApproval.class).get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        if(previousBaseInfo != null && newBaseInfo != null){
            if (!previousBaseInfo.getStage().equals(newBaseInfo.getStage()) ||
                    !previousBaseInfo.getStatus().equals(newBaseInfo.getStatus())) {
                logger.debug("Stage changed: " + previousBaseInfo.getStage() + " " + previousBaseInfo.getStatus() +
                        " -> " + newBaseInfo.getStage() + " " + newBaseInfo.getStatus());
                logger.debug("Prev Request: " + previousBaseInfo.toString());
                logger.debug("New Request : " + newBaseInfo.toString());
                stageMessages
                        .createStageMessages(previousBaseInfo.getStage(), newBaseInfo.getStage(), newBaseInfo)
                        .forEach(e -> javaMailer.sendEmail(e.getRecipient(), e.getSubject(), e.getText()));
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