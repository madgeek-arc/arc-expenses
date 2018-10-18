package arc.expenses.listener;

import arc.expenses.domain.RequestFatClass;
import arc.expenses.mail.EmailMessage;
import arc.expenses.mail.JavaMailer;
import arc.expenses.messages.StageMessages;
import arc.expenses.service.EmailService;
import arc.expenses.service.RequestServiceImpl;
import arc.expenses.utils.Converter;
import arc.expenses.utils.ParserPool;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.monitor.ResourceListener;
import gr.athenarc.domain.Request;
import gr.athenarc.domain.RequestApproval;
import gr.athenarc.domain.RequestPayment;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutionException;


@SuppressWarnings("ALL")
@Component
public class StageResourceListener implements ResourceListener {

    final private static Logger logger = LogManager.getLogger(StageResourceListener.class);

    @Autowired
    ParserPool parserPool;

    @Autowired
    JavaMailer javaMailer;

    @Autowired
    StageMessages stageMessages;

    @Autowired
    EmailService emailService;

    @Autowired
    RequestServiceImpl requestService;

    @Async
    @Override
    public void resourceAdded(Resource resource) {
        logger.debug("Adding a resource");

        if (resource.getResourceType().getName().equals("approval")) {
            try {
                RequestApproval requestApproval = parserPool.deserialize(resource, RequestApproval.class).get();
                if(requestApproval.getStage().equals("2")){
                    sendEmails("1","2",requestApproval.getStatus(),
                            Converter.toRequestFatClass(requestService.get(requestApproval.getRequestId()),requestApproval));
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
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

        Request request = null;
        if( "payment".equals(newResource.getResourceType().getName())){
            try {
                RequestPayment previousRequestPayment = parserPool.deserialize(previousResource, RequestPayment.class).get();
                RequestPayment newRequestPayment = parserPool.deserialize(newResource, RequestPayment.class).get();
                request = requestService.get(newRequestPayment.getRequestId());
                sendEmails(previousRequestPayment,newRequestPayment,request);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        if( "approval".equals(newResource.getResourceType().getName())){
            try {
                RequestApproval previousRequestApproval = parserPool.deserialize(previousResource, RequestApproval.class).get();
                RequestApproval newRequestApproval = parserPool.deserialize(newResource, RequestApproval.class).get();
                request = requestService.get(newRequestApproval.getRequestId());
                sendEmails(previousRequestApproval,newRequestApproval,request);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

//        if(previousBaseInfo != null && newBaseInfo != null){
//            if (!previousBaseInfo.getStage().equals(newBaseInfo.getStage()) ||
//                    !previousBaseInfo.getStatus().equals(newBaseInfo.getStatus())) {
//                logger.debug("Stage changed: " + previousBaseInfo.getStage() + " " + previousBaseInfo.getStatus() +
//                        " -> " + newBaseInfo.getStage() + " " + newBaseInfo.getStatus());
//                logger.debug("Prev Request: " + previousBaseInfo.toString());
//                logger.debug("New Request : " + newBaseInfo.toString());
//
//                List<String> emails = emailService.prepareMessages(previousBaseInfo.getStage(),newBaseInfo.getStage(),
//                                                                  newBaseInfo.getStatus(),Converter.toRequestFatClass(request,newBaseInfo));
//
////                emailService
////                        .createEmail(previousBaseInfo.getStage(), newBaseInfo.getStage(), newBaseInfo)
////                        .forEach(e -> javaMailer.sendEmail(e.getRecipient(), e.getSubject(), e.getText()));
//            }
//        }


    }

    private void sendEmails(RequestApproval previousRequestApproval, RequestApproval newRequestApproval, Request request) {

        if(previousRequestApproval != null && newRequestApproval != null){
            if(!previousRequestApproval.getStatus().equals(newRequestApproval.getStatus())){
                logger.debug("Stage changed: " + previousRequestApproval.getStage() + " " + previousRequestApproval.getStatus() +
                        " -> " + newRequestApproval.getStage() + " " + newRequestApproval.getStatus());
                logger.debug("Prev Request: " + previousRequestApproval.toString());
                logger.debug("New Request : " + newRequestApproval.toString());

                sendEmails(previousRequestApproval.getStage(),newRequestApproval.getStage(),
                        newRequestApproval.getStatus(),Converter.toRequestFatClass(request,newRequestApproval));
            }
        }


    }

    private void sendEmails(RequestPayment previousRequestPayment,RequestPayment newRequestPayment, Request request) {
        if(previousRequestPayment != null && newRequestPayment != null){
            if(!previousRequestPayment.getStatus().equals(newRequestPayment.getStatus())){
                logger.debug("Stage changed: " + previousRequestPayment.getStage() + " " + previousRequestPayment.getStatus() +
                        " -> " + newRequestPayment.getStage() + " " + newRequestPayment.getStatus());
                logger.debug("Prev Request: " + previousRequestPayment.toString());
                logger.debug("New Request : " + newRequestPayment.toString());

                sendEmails(previousRequestPayment.getStage(),newRequestPayment.getStage(),
                        newRequestPayment.getStatus(),Converter.toRequestFatClass(request,newRequestPayment));
            }
        }
    }


    private void sendEmails(String oldStage, String newStage , String status, RequestFatClass requestFatClass){

        List<EmailMessage> messages = emailService.prepareMessages(oldStage,newStage,status,requestFatClass);
//        messages.forEach(e -> javaMailer.sendEmail(e.getRecipient(), e.getSubject(), e.getText()));
//        messages.forEach(e -> logger.info(e.toString()));
    }

    @Async
    @Override
    public void resourceDeleted(Resource resource) {
        // TODO
        logger.info("Deleting a resource");
    }
}