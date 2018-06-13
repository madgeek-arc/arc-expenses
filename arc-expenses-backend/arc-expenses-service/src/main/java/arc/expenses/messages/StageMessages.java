package arc.expenses.messages;

import arc.expenses.service.GenericService;
import arc.expenses.service.UserServiceImpl;
import arc.expenses.ApplicationStages;
import eu.openminted.registry.core.domain.FacetFilter;
import gr.athenarc.domain.POI;
import gr.athenarc.domain.Request;
import arc.expenses.mail.EmailMessage;
import gr.athenarc.domain.User;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class StageMessages {
    public enum UserType {USER, POI, nextPOI}

    public enum RequestState {INITIALIZED, ACCEPTED, ACCEPTED_DIAVGEIA, REVIEW, REJECTED, COMPLETED}

    @Autowired
    UserServiceImpl userServiceImpl;

    @Autowired
    private StageMessages(GenericService<User> userServiceImpl) {
        this.userServiceImpl = (UserServiceImpl) userServiceImpl;
    }

    @Value("${request.url}")
    String url;

    Logger logger = Logger.getLogger(StageMessages.class);

    public List<EmailMessage> createStageMessages(String prevStage, String nextStage, Request request) {
        List<EmailMessage> emails = new ArrayList<>();
        String transition = null;
        String subject = "[ARC-REQUEST] Αίτηση " + request.getId();
        RequestState state = RequestState.ACCEPTED;

        logger.info("Request Link: " + url + request.getId());

        if ("rejected".equals(request.getStatus())) {
            emails.add(new EmailMessage(request.getRequester().getEmail(), subject, messageTemplates(
                    null, null, request.getId(), UserType.USER, RequestState.REJECTED,
                    request.getStage1().getRequestDate())));
            state = RequestState.REJECTED;
            transition = "rejected";

        } else if (ApplicationStages.underReview(prevStage, nextStage)) {
//        } else if ("review".equals(request.getStatus())) {
            nextStage = prevStage;
            prevStage = ApplicationStages.getPreviousStage(request); // FIXME DEBUG this function
            state = RequestState.REVIEW;
            transition = "<-";
        } else {
            transition = prevStage + "->" + nextStage;
        }

        switch (transition) {

            case "1->2":
                if (state == RequestState.ACCEPTED) {
                    state = RequestState.INITIALIZED;
                }
                // email to USER
                emails.addAll(getEmailMessages(request, UserType.USER, state, subject));
                // email to nextPOIs and delegates
                emails.addAll(getEmailMessages(request, UserType.nextPOI, state, subject));
                break;

            case "6->7":
                if (state == RequestState.ACCEPTED) {
                    state = RequestState.ACCEPTED_DIAVGEIA;
                }
                // email to USER
                emails.addAll(getEmailMessages(request, UserType.USER, state, subject));
                // email to POI and delegates
                emails.addAll(getEmailMessages(request, UserType.POI, state, subject));
                // email to nextPOI and delegates
                emails.addAll(getEmailMessages(request, UserType.nextPOI, state, subject));
                break;

            case "13->13":
                if (state == RequestState.ACCEPTED) {
                    state = RequestState.COMPLETED;
                }
                if (state != RequestState.REVIEW) {
                    // email to USER
                    emails.addAll(getEmailMessages(request, UserType.USER, state, subject));
                }
                // email to POI and delegates
                emails.addAll(getEmailMessages(request, UserType.POI, state, subject));
                break;

            case "<-":

                break;

            case "rejected":

                break;

            default:
                // email to POIs and delegates
                emails.addAll(getEmailMessages(request, UserType.POI, state, subject));
                // email to nextPOI and delegates
                emails.addAll(getEmailMessages(request, UserType.nextPOI, state, subject));
        }
        return emails;
    }


    private List<EmailMessage> getEmailMessages(Request request, UserType type, RequestState state, String subject) {
        List<EmailMessage> messages = new ArrayList<>();
        ApplicationStages appStages = new ApplicationStages(request);
        String completedStage = ApplicationStages.getPreviousStage(request);
        String date = appStages.getDate(completedStage);

        if (type == UserType.USER) {
            User user = appStages.getUser(completedStage); // get User of completed stage (previous stage)
            List<POI> poi = appStages.getPersonsOfInterest(completedStage); // get POIs of completed stage (previous stage)
            messages.add(new EmailMessage(request.getRequester().getEmail(), subject,
                    messageTemplates(user.getFirstname(), user.getLastname(), request.getId(), type, state, date)));
        } else if (type == UserType.POI) {
            User user = appStages.getUser(completedStage); // get User of completed stage (previous stage)
            List<POI> poi = appStages.getPersonsOfInterest(completedStage); // get POIs of completed stage (previous stage)
            List<String> addresses = new ArrayList<>();
            poi.forEach(person -> {
                addresses.add(person.getEmail());
                person.getDelegates().forEach(delegate -> addresses.add(delegate.getEmail()));
            });
            addresses.forEach(address -> messages.add(new EmailMessage(address, subject,
                    messageTemplates(user.getFirstname(), user.getLastname(), request.getId(), type, state, date))));
        } else if (type == UserType.nextPOI) {
            User user = appStages.getUser(completedStage); // get User of completed stage (previous stage)
            List<POI> poi = appStages.getPersonsOfInterest(request.getStage()); // get POIs of next stage
            List<String> addresses = new ArrayList<>();
            poi.forEach(person -> {
                addresses.add(person.getEmail());
                person.getDelegates().forEach(delegate -> addresses.add(delegate.getEmail()));
            });
            addresses.forEach(address -> messages.add(new EmailMessage(address, subject,
                    messageTemplates(user.getFirstname(), user.getLastname(), request.getId(), type, state, date))));
        } else {

        }
        return messages;
    }

    public User getUserByEmail(final List<User> users, final String email) {
        if (users == null || email == null)
            return null;
        Optional<User> user = users.parallelStream().filter(u -> u.getEmail().equals(email)).findAny();
        return user.orElse(null);
//        return users.stream().filter(user -> user.getEmail().equals(email)).findAny().get();
    }

    private List<EmailMessage> filterOutNonImmediate(List<EmailMessage> emails) {
        List<EmailMessage> emailList = new ArrayList<>();
//        List<User> users = userServiceImpl.getUsersWithImmediateEmailPreference();
        List<User> users = userServiceImpl.getAll(new FacetFilter()).getResults(); // FIXME: sends e-mails to everyone
        for (Iterator<EmailMessage> iterator = emails.iterator(); iterator.hasNext(); ) {
            EmailMessage email = iterator.next();
            User user = getUserByEmail(users, email.getRecipient());
            if (user != null) {
                emailList.add(email);
            }
        }
        return emailList;
    }

    private String messageTemplates(String firstname, String lastname, String id, UserType type,
                                    RequestState state, String date_secs) {
        String messageText = null;
        String date = "";
        if (date_secs != null) {
            date = new SimpleDateFormat("dd/MM/yyyy").format(new Date(Long.parseLong(date_secs))).toString();
        }
        String link = createLink(id);
        if (type == UserType.USER) {
            if (state == RequestState.INITIALIZED) {
                messageText = "Το αίτημά σας, με κωδικό " + id + ", υποβλήθηκε επιτυχώς στις " + date;
            } else if (state == RequestState.ACCEPTED) {
                messageText = "Το αίτημά σας εγκρίθηκε από τον επιστημονικό υπεύθυνο: " + firstname + " " + lastname;
            } else if (state == RequestState.ACCEPTED_DIAVGEIA) {
                messageText = "Το αίτημά σας με κωδικό " + id + " εγκρίθηκε από τον υπεύθυνο της Διαύγειας, " +
                        firstname + " " + lastname + " στις " + date + ". \nΜπορείτε να προχωρήσετε με τις αγορές σας.";
            } else if (state == RequestState.COMPLETED) {
                messageText = "Το αίτημά σας με κωδικό " + id + " ελέγχθηκε κι εγκρίθηκε επιτυχώς!";
            } else if (state == RequestState.REJECTED) {
                messageText = "Το αίτημά σας με κωδικό " + id + " απορρίφθηκε.";
            }
        } else if (type == UserType.POI) {
            if (state == RequestState.INITIALIZED) {
                messageText = "Νέο αίτημα με κωδικό " + id + " από τον/την " + firstname + " " + lastname;
            } else if (state == RequestState.ACCEPTED) {
                messageText = "Εγκρίθηκε το αίτημα με κωδικό " + id + " από τον/την " + firstname + " " + lastname;
            } else if (state == RequestState.ACCEPTED_DIAVGEIA) {
                messageText = "Το αίτημά με κωδικό " + id + " εγκρίθηκε από τον υπεύθυνο της Διαύγειας, " +
                        firstname + " " + lastname + " στις " + date + ".";
            } else if (state == RequestState.COMPLETED) {
                messageText = "Ολοκληρώθηκε το αίτημα με κωδικό " + id + " από τον/την " + firstname + " " + lastname;
            } else if (state == RequestState.REJECTED) {
                messageText = "Απορρίφθηκε το αίτημα με κωδικό " + id + " από τον/την " + firstname + " " + lastname;
                // TODO @spring.Resource to load (use .xml not .properties)
//                messageText = "Απορρίφθηκε το αίτημα με κωδικό {{id}} από τον/την {{firstname}} {{lastname}}";
            } else if (state == RequestState.REVIEW) {
                messageText = "Tο αίτημα με κωδικό " + id + ", επιστράφηκε για επανέλεγχο από τον/την " + firstname +
                        " " + lastname;
            }
        } else if (type == UserType.nextPOI) {
            if (state == RequestState.INITIALIZED) {
                messageText = "Nέα αίτηση υποβλήθηκε στο σύστημα από τον/την " + firstname + " " + lastname +
                        ". Κωδικός αίτησης: " + id;
            } else if (state == RequestState.ACCEPTED) {
                messageText = "Νέο αίτημα προς έλεγχο με κωδικό " + id;
            } else if (state == RequestState.ACCEPTED_DIAVGEIA) {
                messageText = "Το αίτημά με κωδικό " + id + " εγκρίθηκε από τον υπεύθυνο της Διαύγειας, " +
                        firstname + " " + lastname + " στις " + date + ".";
            } else if (state == RequestState.REVIEW) {
                messageText = "Το αίτημα με κωδικό " + id + " βρίσκεται υπό επανέλεγχο";
            } else if (state == RequestState.COMPLETED) {
                messageText = "";
            } else if (state == RequestState.REJECTED) {
                messageText = "";
            }
        }
        return messageText + "\n\nΓια να μεταβείτε στη σελίδα του αιτήματος, πατήστε τον παρακάτω σύνδεσμο:\n"
                + createLink(id);
    }

    private String createLink(String id) {
        return url + id;
    }
}
