package arc.expenses.messages;

import arc.expenses.mail.EmailMessage;
import arc.expenses.mail.JavaMailer;
import arc.expenses.service.RequestServiceImpl;
import arc.expenses.service.UserServiceImpl;
import eu.openminted.registry.core.domain.FacetFilter;
import gr.athenarc.domain.Request;
import gr.athenarc.domain.User;
import joptsimple.internal.Strings;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Configuration
@EnableScheduling
public class MessageScheduler  {

    private static final Logger logger = Logger.getLogger(MessageScheduler.class);

    @Autowired
    private UserServiceImpl userService;

    @Autowired
    private RequestServiceImpl requestService;

    @Autowired
    private JavaMailer javaMailer;

    @Autowired
    private StageMessages stageMessages;

    @Scheduled(cron = "${messages.cronExpr:0 0 8 * * ?}")
//    @Scheduled(cron = "* * * * * ?") // use this for debugging
    public void scheduledEmails() {
        logger.info("Sending scheduled emails");
        List<EmailMessage> mailList = new ArrayList<>();
        List<User> users = userService.getAll(new FacetFilter()).getResults();

        for (User user: users) {
            if ( "true".equals(user.getReceiveEmails()) /*&& "false".equals(user.getImmediateEmails())*/ ) {
                List<Request> user_requests = requestService.getPendingRequests(user.getEmail());
                if (user_requests.size() > 0) {
                    String text = createDigest(user_requests);
                    mailList.add(new EmailMessage(user.getEmail(), "[ARC-ν.4485] Daily Digest", text));
                }
            }
        }
        mailList.forEach(email -> {
            logger.info(email);
            javaMailer.sendEmail(email.getRecipient(), email.getSubject(), email.getText());
        });
    }

    private String createDigest(@NotNull List<Request> requests) {
        StringBuilder text = new StringBuilder("Αγαπητέ χρήστη,\nΕκκρεμεί η διεκπεραίωση των παρακάτω αιτημάτων:\n");

        List<String> pending = requests
                .stream()
                .map(r -> stageMessages.getRequestInfo(r))
                .collect(Collectors.toList());
        text.append(Strings.join(pending,"\n\n\n"));

        return text.toString();
    }
}
