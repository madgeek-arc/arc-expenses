package arc.expenses.listener;

import arc.expenses.domain.RequestFatClass;
import arc.expenses.mail.EmailMessage;
import arc.expenses.mail.JavaMailer;
import arc.expenses.messages.StageMessages;
import arc.expenses.EmailService;
import arc.expenses.RequestServiceImpl;
import arc.expenses.utils.Converter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.monitor.ResourceListener;
import eu.openminted.registry.core.service.ParserService;
import gr.athenarc.domain.Request;
import gr.athenarc.domain.RequestApproval;
import gr.athenarc.domain.RequestPayment;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class StageResourceListener implements ResourceListener {

    final private static Logger logger = LogManager.getLogger(StageResourceListener.class);

    @Autowired
    ParserService parserPool;

    @Autowired
    JavaMailer javaMailer;

    @Autowired
    StageMessages stageMessages;

    @Autowired
    EmailService emailService;

    @Autowired
    RequestServiceImpl requestService;

    @Value("${mail.restore:false}")
    private String restore;

    private Boolean adminResourceUpdate = false;

    @Async
    @Override
    public void resourceAdded(Resource resource) {
        logger.debug("Adding a resource");

       /* if (resource.getResourceType().getName().equals("approval") && !Boolean.parseBoolean(restore) &&!adminResourceUpdate) {
            RequestApproval requestApproval = parserPool.deserialize(resource, RequestApproval.class);
            if(requestApproval.getStage().equals("2")){
                sendEmails("1","2",requestApproval.getStatus(),
                        Converter.toRequestFatClass(requestService.get(requestApproval.getRequestId()),requestApproval));
            }
        }*/
    }

    @Async
    @Override
    public void resourceUpdated(Resource previousResource, Resource newResource) {
        logger.info("Updating a resource");

        Request request = null;
        if( "payment".equals(newResource.getResourceType().getName()) && !Boolean.parseBoolean(restore) && !adminResourceUpdate){
            RequestPayment previousRequestPayment = parserPool.deserialize(previousResource, RequestPayment.class);
            RequestPayment newRequestPayment = parserPool.deserialize(newResource, RequestPayment.class);
            request = requestService.get(newRequestPayment.getRequestId());
            sendEmails(previousRequestPayment,newRequestPayment,request);
        }
        if( "approval".equals(newResource.getResourceType().getName()) && !Boolean.parseBoolean(restore) && !adminResourceUpdate){
            RequestApproval previousRequestApproval = parserPool.deserialize(previousResource, RequestApproval.class);
            RequestApproval newRequestApproval = parserPool.deserialize(newResource, RequestApproval.class);
            request = requestService.get(newRequestApproval.getRequestId());
            sendEmails(previousRequestApproval,newRequestApproval,request);
        }
    }

    private void sendEmails(RequestApproval previousRequestApproval, RequestApproval newRequestApproval, Request request) {

        if(previousRequestApproval != null && newRequestApproval != null){
            if(!previousRequestApproval.getStatus().equals(newRequestApproval.getStatus()) ||
                    !previousRequestApproval.getStage().equals(newRequestApproval.getStage())){
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
            if(!previousRequestPayment.getStatus().equals(newRequestPayment.getStatus()) ||
                    !previousRequestPayment.getStage().equals(newRequestPayment.getStage())){
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
       // messages.forEach(e -> javaMailer.sendEmail(e.getRecipient(), e.getSubject(), e.getText()));
        messages.forEach(logger::info);
    }

    @Async
    @Override
    public void resourceDeleted(Resource resource) {
        // TODO
        logger.info("Deleting a resource");
    }
}
