package arc.expenses.messages;

import arc.expenses.service.GenericService;
import arc.expenses.service.UserServiceImpl;
import com.google.inject.Stage;
import gr.athenarc.domain.POI;
import gr.athenarc.domain.Request;
import arc.expenses.mail.EmailMessage;
import gr.athenarc.domain.User;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class StageMessages {
    public enum UserType {USER, POI, nextPOI}
    public enum RequestState {INITIALIZED, ACCEPTED, ACCEPTED_DIAVGEIA, REVIEW, REJECTED, COMPLETED}

    UserServiceImpl userServiceImpl;

    @Autowired
    private StageMessages(GenericService<User> userServiceImpl) {
        this.userServiceImpl = (UserServiceImpl) userServiceImpl;
    }

    Logger logger = Logger.getLogger(StageMessages.class);

    // FIXME: change array values if "Stage Order Changes"
    final String[] stages = {null,"1","2","3","4","5a","5b","6","7","8","9","10","11","12","13"};


    // TODO: extend each stage
    public List<EmailMessage> createMessages(String prevStage, String nextStage, Request request) {
        List<EmailMessage> emails = new ArrayList<>();

        String subject = "[ARC-REQUEST] Αίτηση " + request.getId();
        RequestState state = RequestState.ACCEPTED;

        if ("rejected".equals(request.getStatus())) {
            emails.add(new EmailMessage(request.getRequester().getEmail(), subject, messageTemplates(
                    null, null, request.getId(), UserType.USER, RequestState.REJECTED,
                    request.getStage1().getRequestDate())));
            state = RequestState.REJECTED;
//            return emails;
        } else if (Arrays.asList(stages).indexOf(prevStage) > Arrays.asList(stages).indexOf(nextStage)) {
//        } else if ("review".equals(request.getStatus())) {
//            nextStage = prevStage;
            prevStage = getPreviousStage(request);
            state = RequestState.REVIEW;
        }

        // Stage 1 -> 2
        if (prevStage == null && "2".equals(nextStage)) {
            logger.info("stage " + prevStage + " -> " + nextStage);

            User user = request.getRequester();
            String date = request.getStage1().getRequestDate();

            // email to USER
            emails.addAll(getEmailMessages(request, request.getProject().getScientificCoordinator(),
                    user, UserType.USER, RequestState.INITIALIZED, date, subject));
            emails.addAll(getEmailMessages(request, request.getProject().getScientificCoordinator(),
                    request.getRequester(), UserType.nextPOI, RequestState.INITIALIZED,
                    request.getStage1().getRequestDate(), subject));

        }
        else if ("2".equals(prevStage) && "3".equals(nextStage)) {
            logger.info("stage " + prevStage + " -> " + nextStage);

            User user = request.getStage2().getUser();
            String date = request.getStage2().getDate();

            if (state != RequestState.REJECTED) {
                // email to USER
                emails.addAll(getEmailMessages(request, request.getProject().getScientificCoordinator(),
                        user, UserType.USER, state, date, subject));
            }
            // email to POI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getScientificCoordinator(),
                    user, UserType.POI, state, date, subject));
            // email to nextPOIs and delegates
            final RequestState finalState2 = state;
            request.getProject().getOperator()
                    .forEach(operator ->
                            emails.addAll(getEmailMessages(request, operator, user,
                                    UserType.nextPOI, finalState2, date, subject)));
        }
        // Stage 3 -> 4
        else if ("3".equals(prevStage) && "4".equals(nextStage)) {
            logger.info("stage " + prevStage + " -> " + nextStage);

            User user = request.getStage3().getUser();
            String date = request.getStage3().getDate();

            // email to POIs and delegates
            final RequestState finalState1 = state;
            request.getProject().getOperator()
                    .forEach(operator ->
                            emails.addAll(getEmailMessages(request, operator, user,
                                    UserType.POI, finalState1, date, subject)));
            // email to nextPOI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getOrganization().getPOI(),
                    user, UserType.nextPOI, state, date, subject));
        }
        // Stage 4 -> 5
        else if ("4".equals(prevStage) && "5".equals(nextStage)) {
            logger.info("stage " + prevStage + " -> " + nextStage);

            User user = request.getStage4().getUser();
            String date = request.getStage4().getDate();

            // email to POI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getOrganization().getPOI(),
                    user, UserType.POI, state, date, subject));
            // email to nextPOI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getDirector(),
                    user, UserType.nextPOI, state, date, subject));
        }
        // Stage 4 -> 5a
        else if ("4".equals(prevStage) && "5a".equals(nextStage)) {
            logger.info("stage " + prevStage + " -> " + nextStage);

            User user = request.getStage4().getUser();
            String date = request.getStage4().getDate();

            // email to POI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getOrganization().getPOI(),
                    user, UserType.POI, state, date, subject));
            // email to nextPOI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getOrganization().getDirector(),
                    user, UserType.nextPOI, state, date, subject));
        }
        // Stage 4 -> 5b
        else if ("4".equals(prevStage) && "5b".equals(nextStage)) {
            logger.info("stage " + prevStage + " -> " + nextStage);


            User user = request.getStage4().getUser();
            String date = request.getStage4().getDate();

            // email to POI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getOrganization().getPOI(),
                    user, UserType.POI, state, date, subject));
            // email to nextPOI and delegates
            emails.addAll(getEmailMessages(request,
                    request.getProject().getInstitute().getOrganization().getDioikitikoSumvoulio(),
                    user, UserType.nextPOI, state, date, subject));
        }
        // Stage 5 -> 5a
        else if ("5".equals(prevStage) && "5a".equals(nextStage)) {
            logger.info("stage " + prevStage + " -> " + nextStage);

            User user = request.getStage5().getUser();
            String date = request.getStage5().getDate();

            // email to POI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getDirector(),
                    user, UserType.POI, state, date, subject));
            // email to nextPOI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getOrganization().getDirector(),
                    user, UserType.nextPOI, state, date, subject));
        }
        // Stage 5 -> 5b
        else if ("5".equals(prevStage) && "5b".equals(nextStage)) {
            logger.info("stage " + prevStage + " -> " + nextStage);

            User user = request.getStage5().getUser();
            String date = request.getStage5().getDate();

            // email to POI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getDirector(),
                    user, UserType.POI, state, date, subject));
            // email to nextPOI and delegates
            emails.addAll(getEmailMessages(request,
                    request.getProject().getInstitute().getOrganization().getDioikitikoSumvoulio(),
                    user, UserType.nextPOI, state, date, subject));
        }
        // Stage 5 -> 6
        else if ("5".equals(prevStage) && "6".equals(nextStage)) {
            logger.info("stage " + prevStage + " -> " + nextStage);

            User user = request.getStage5().getUser();
            String date = request.getStage5().getDate();

            // email to POI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getDirector(),
                    user, UserType.POI, state, date, subject));
            // email to nextPOI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getDiaugeia(),
                    user, UserType.nextPOI, state, date, subject));
        }
        // Stage 5a -> 5b
        else if ("5a".equals(prevStage) && "5b".equals(nextStage)) {
            logger.info("stage " + prevStage + " -> " + nextStage);

            User user = request.getStage5a().getUser();
            String date = request.getStage5a().getDate();

            // email to POI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getOrganization().getDirector(),
                    user, UserType.POI, state, date, subject));
            // email to nextPOI and delegates
            emails.addAll(getEmailMessages(request,
                    request.getProject().getInstitute().getOrganization().getDioikitikoSumvoulio(),
                    user, UserType.nextPOI, state, date, subject));
        }
        // Stage 5a -> 6
        else if ("5a".equals(prevStage) && "6".equals(nextStage)) {
            logger.info("stage " + prevStage + " -> " + nextStage);

            User user = request.getStage5a().getUser();
            String date = request.getStage5a().getDate();

            // email to POI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getOrganization().getDirector(),
                    user, UserType.POI, state, date, subject));
            // email to nextPOI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getDiaugeia(),
                    user, UserType.nextPOI, state, date, subject));
        }
        // Stage 5b -> 6
        else if ("5b".equals(prevStage) && "6".equals(nextStage)) {
            logger.info("stage " + prevStage + " -> " + nextStage);

            User user = request.getStage5b().getUser();
            String date = request.getStage5b().getDate();

            // email to POI and delegates
            emails.addAll(getEmailMessages(request,
                    request.getProject().getInstitute().getOrganization().getDioikitikoSumvoulio(),
                    user, UserType.POI, state, date, subject));
            // email to nextPOI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getDiaugeia(),
                    user, UserType.nextPOI, state, date, subject));
        }
        // Stage 6 -> 7
        else if ("6".equals(prevStage) && "7".equals(nextStage)) {
            logger.info("stage " + prevStage + " -> " + nextStage);

            User user = request.getStage6().getUser();
            String date = request.getStage6().getDate();

            if (state == RequestState.ACCEPTED) {
                final RequestState finalState = RequestState.ACCEPTED_DIAVGEIA;

                // email to USER
                emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getDiaugeia(),
                        user, UserType.USER, finalState, date, subject));
                // email to POI and delegates
                emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getDiaugeia(),
                        user, UserType.POI, finalState, date, subject));
                // email to nextPOI and delegates
                request.getProject().getOperator()
                        .forEach(operator -> emails.addAll(getEmailMessages(request, operator,
                                user, UserType.nextPOI, finalState, date, subject)));
            } else {
                final RequestState finalState = state;
                // email to POI and delegates
                emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getDiaugeia(),
                        user, UserType.POI, state, date, subject));
                // email to nextPOI and delegates
                request.getProject().getOperator()
                        .forEach(operator -> emails.addAll(getEmailMessages(request, operator,
                                user, UserType.nextPOI, finalState, date, subject)));
            }
        }
        // Stage 7 -> 8
        else if ("7".equals(prevStage) && "8".equals(nextStage)) {
            logger.info("stage " + prevStage + " -> " + nextStage);

            User user = request.getStage7().getUser();
            String date = request.getStage7().getDate();

            // email to POIs and delegates
            final RequestState finalState = state;
            request.getProject().getOperator()
                    .forEach(operator -> emails.addAll(getEmailMessages(request, operator,
                            user, UserType.POI, finalState, date, subject)));
            // email to nextPOI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getAccountingDirector(),
                    user, UserType.nextPOI, state, date, subject));
        }
        // Stage 8 -> 9
        else if ("8".equals(prevStage) && "9".equals(nextStage)) {
            logger.info("stage " + prevStage + " -> " + nextStage);

            User user = request.getStage8().getUser();
            String date = request.getStage8().getDate();

            // email to POI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getAccountingDirector(),
                    user, UserType.POI, state, date, subject));
            // email to nextPOI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getOrganization().getPOI(),
                    user, UserType.nextPOI, state, date, subject));
        }
        // Stage 9 -> 10
        else if ("9".equals(prevStage) && "10".equals(nextStage)) {
            logger.info("stage " + prevStage + " -> " + nextStage);

            User user = request.getStage9().getUser();
            String date = request.getStage9().getDate();

            // email to POI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getOrganization().getPOI(),
                    user, UserType.POI, state, date, subject));
            // email to nextPOI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getOrganization().getDirector(),
                    user, UserType.nextPOI, state, date, subject));
        }
        // Stage 10 -> 11
        else if ("10".equals(prevStage) && "11".equals(nextStage)) {
            logger.info("stage " + prevStage + " -> " + nextStage);

            User user = request.getStage10().getUser();
            String date = request.getStage10().getDate();

            // email to POI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getOrganization().getDirector(),
                    user, UserType.POI, state, date, subject));
            // email to nextPOI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getDiaugeia(),
                    user, UserType.nextPOI, state, date, subject));
        }
        // Stage 11 -> 12
        else if ("11".equals(prevStage) && "12".equals(nextStage)) {
            logger.info("stage " + prevStage + " -> " + nextStage);

            User user = request.getStage11().getUser();
            String date = request.getStage11().getDate();

            // email to POI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getDiaugeia(),
                    user, UserType.POI, state, date, subject));
            // email to nextPOI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getAccountingRegistration(),
                    user, UserType.nextPOI, state, date, subject));
        }
        // Stage 12 -> 13
        else if ("12".equals(prevStage) && "13".equals(nextStage)) {
            logger.info("stage " + prevStage + " -> " + nextStage);

            User user = request.getStage12().getUser();
            String date = request.getStage12().getDate();

            // email to POI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getAccountingRegistration(),
                    user, UserType.POI, state, date, subject));
            // email to nextPOI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getAccountingPayment(),
                    user, UserType.nextPOI, state, date, subject));
        }
        // Stage 13 -> 13 Completed
        else if ("13".equals(prevStage) && "13".equals(nextStage)) {
            logger.info("stage " + prevStage + " -> " + nextStage);

            User user = request.getStage13().getUser();
            String date = request.getStage13().getDate();

            if (state == RequestState.ACCEPTED) {
                state = RequestState.COMPLETED;
            }
            if (state != RequestState.REVIEW) {
                // email to USER
                emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getDiaugeia(),
                        user, UserType.USER, state, date, subject));
            }

            // email to POI and delegates
            emails.addAll(getEmailMessages(request, request.getProject().getInstitute().getAccountingRegistration(),
                    user, UserType.POI, state, date, subject));

        }

        List<EmailMessage> mails;
        mails = filterOutNonImmediate(emails);
        return mails;
    }

    private List<EmailMessage> getEmailMessages(Request req, POI poi, User user, UserType type, RequestState state,
                                                String date, String subject) {
        List<EmailMessage> messages = new ArrayList<>();
        if (type == UserType.USER) {
            messages.add(new EmailMessage(req.getRequester().getEmail(), subject,
                    messageTemplates(user.getFirstname(), user.getLastname(), req.getId(), type, state, date)));
        }
        else if (type == UserType.POI) {
            List<String> addresses = new ArrayList<>();
            addresses.add(poi.getEmail());
            poi.getDelegates().forEach(delegate -> addresses.add(delegate.getEmail()));
            addresses.forEach(address -> messages.add(new EmailMessage(address, subject,
                    messageTemplates(user.getFirstname(), user.getLastname(), req.getId(), type, state, date))));
        } else if (type == UserType.nextPOI) {
            List<String> addresses = new ArrayList<>();
            addresses.add(poi.getEmail());
            poi.getDelegates().forEach(delegate -> addresses.add(delegate.getEmail()));
            addresses.forEach(address -> messages.add(new EmailMessage(address, subject,
                    messageTemplates(user.getFirstname(), user.getLastname(), req.getId(), type, state, date))));
        } else {

        }
        return messages;
    }

    public User getUserByEmail(final List<User> users, final String email) {
        if(users == null || email == null)
            return null;
        Optional<User> user = users.parallelStream().filter(u -> u.getEmail().equals(email)).findAny();
        return user.orElse(null);
//        return users.stream().filter(user -> user.getEmail().equals(email)).findAny().get();
    }


    public String getPreviousStage(Request request) {
        try {
            Object stage = null;
            int index = Arrays.asList(stages).indexOf(request.getStage());
            do {
                index --;
                stage = PropertyUtils.getProperty(request,"stage" + stages[index]);
            } while (stage == null);

            return stages[index];
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    private List<EmailMessage> filterOutNonImmediate(List<EmailMessage> emails) {
        List<EmailMessage> emailList = new ArrayList<>();
        List<User> users = userServiceImpl.getUsersWithImmediateEmailPreference();
        for (Iterator<EmailMessage> iterator = emails.iterator(); iterator.hasNext();) {
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
//                messageText = "Απορρίφθηκε το αίτημα με κωδικό {{id}} από τον/την {{firstname}} {{lastname}}"; //TODO @spring.Resource to load (use .xml not .properties)
            } else if (state == RequestState.REVIEW) {
                messageText = "Tο αίτημα με κωδικό " + id + ", επιστράφηκε για επανέλεγχο από τον/την " + firstname +
                        " " + lastname;
            }
        } else if (type == UserType.nextPOI) {
            if (state == RequestState.INITIALIZED) {
                messageText = "Nέα αίτηση υποβλήθηκε στο σύστημα. Κωδικός αίτησης: " + id;
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
        return messageText;
    }
}
