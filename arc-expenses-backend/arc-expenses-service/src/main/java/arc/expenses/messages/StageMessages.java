package arc.expenses.messages;

import arc.expenses.RequestWrapper;
import arc.expenses.mail.EmailMessage;
import arc.expenses.service.RequestServiceImpl;
import gr.athenarc.domain.BaseInfo;
import gr.athenarc.domain.PersonOfInterest;
import gr.athenarc.domain.Request;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static arc.expenses.messages.StageMessages.RequestState.*;
import static arc.expenses.messages.StageMessages.UserType.USER;

@Component
public class StageMessages {

    private final static org.apache.logging.log4j.Logger logger = LogManager.getLogger(StageMessages.class);

    /*private UserServiceImpl userServiceImpl;*/

    public enum UserType {USER, previousPersonOfInterest, nextPersonOfInterest}

    public enum RequestState {INITIALIZED, ACCEPTED, INVOICE, ACCEPTED_DIAVGEIA, REVIEW, REJECTED, COMPLETED}

    /*@Autowired
    private StageMessages(GenericService<User> userServiceImpl) {
        this.userServiceImpl = (UserServiceImpl) userServiceImpl;
    }*/

    @Value("${request.url}")
    String url;

    @Autowired
    RequestServiceImpl requestService;

    public List<EmailMessage> createStageMessages(String prevStage, String nextStage, BaseInfo baseInfo) {
        List<EmailMessage> emails = new ArrayList<>();
        String transition;
        RequestState state = ACCEPTED;

        logger.debug("Request Link: " + url + baseInfo.getId());

        if ("rejected".equals(baseInfo.getStatus())) {
            state = RequestState.REJECTED;
            transition = "rejected";

        } else if ("under_review".equals(baseInfo.getStatus())) {
            // TODO: add explicitly cases requiring special handling
            switch (nextStage) {
                case "1":
                    transition = nextStage + "<-";
                    break;
                default:
                    transition = "<-";
            }
            state = REVIEW;
        } else {
            transition = prevStage + "->" + nextStage;
        }

        switch (transition) {
            // TODO: add explicitly cases requiring special handling
            case "1->2":
                if (state == ACCEPTED) {
                    state = INITIALIZED;
                }
                // email to USER
                emails.addAll(getEmailMessages(baseInfo, USER, state));
                // email to nextPersonOfInterests and delegates
                emails.addAll(getEmailMessages(baseInfo, UserType.nextPersonOfInterest, state));
                break;

            case "5a->UplodInvoice":
                // email to USER
                emails.addAll(getEmailMessages(baseInfo, USER, INVOICE));
                // email to previousPersonOfInterest and delegates
                emails.addAll(getEmailMessages(baseInfo, UserType.previousPersonOfInterest, state));
//                // email to nextPersonOfInterest and delegates
//                emails.addAll(getEmailMessages(request, UserType.nextPersonOfInterest, state, subject));
                break;

            case "UploadInvoice<-8":
                // email to USER
                emails.addAll(getEmailMessages(baseInfo, USER, INVOICE));
                // email to previousPersonOfInterest and delegates
                emails.addAll(getEmailMessages(baseInfo, UserType.previousPersonOfInterest, state));
//                // email to nextPersonOfInterest and delegates
//                emails.addAll(getEmailMessages(request, UserType.nextPersonOfInterest, state, subject));
                break;

            case "6->7":
                if (state == ACCEPTED) {
                    state = ACCEPTED_DIAVGEIA;
                }
                // email to USER
                emails.addAll(getEmailMessages(baseInfo, USER, state));
                // email to previousPersonOfInterest and delegates
                emails.addAll(getEmailMessages(baseInfo, UserType.previousPersonOfInterest, state));
                // email to nextPersonOfInterest and delegates
                emails.addAll(getEmailMessages(baseInfo, UserType.nextPersonOfInterest, state));
                break;

            case "13->13":
                if (state == ACCEPTED) {
                    state = COMPLETED;
                }
                if (state != REVIEW) { // TODO: probably remove, REVIEW is only for '<-' cases
                    // email to USER
                    emails.addAll(getEmailMessages(baseInfo, USER, state));
                }
                // email to previousPersonOfInterest and delegates
                emails.addAll(getEmailMessages(baseInfo, UserType.previousPersonOfInterest, state));
                break;

            case "1<-":
                // email to USER
                emails.addAll(getEmailMessages(baseInfo, USER, state));
                // email to previousPersonOfInterest and delegates
                emails.addAll(getEmailMessages(baseInfo, UserType.previousPersonOfInterest, state));
                break;

//            case "12<-13":
//
//                break;

            case "<-":
                // email to previousPersonOfInterest and delegates
                emails.addAll(getEmailMessages(baseInfo, UserType.previousPersonOfInterest, state));
                // email to nextPersonOfInterest and delegates
                emails.addAll(getEmailMessages(baseInfo, UserType.nextPersonOfInterest, state));
                break;

            case "rejected":
                // email to USER
                emails.addAll(getEmailMessages(baseInfo, USER, state));
                // email to PersonOfInterests and delegates
                emails.addAll(getEmailMessages(baseInfo, UserType.previousPersonOfInterest, state));
                break;

            case "UploadInvoice->8":
                // probably default case works // TODO check this !!!
            default:
                // email to PersonOfInterests and delegates
                emails.addAll(getEmailMessages(baseInfo, UserType.previousPersonOfInterest, state));
                // email to nextPersonOfInterest and delegates
                emails.addAll(getEmailMessages(baseInfo, UserType.nextPersonOfInterest, state));
        }

        emails.forEach(logger::info);
        return emails;
    }

//    public User getUserByEmail(final List<User> users, final String email) {
//        if (users == null || email == null)
//            return null;
//        Optional<User> user = users.parallelStream().filter(u -> u.getEmail().equals(email)).findAny();
//
//        return user.orElse(null);
////        return users.stream().filter(user -> user.getEmail().equals(email)).findAny().get();
//    }

//    private List<EmailMessage> filterOutNonImmediate(List<EmailMessage> emails) {
//        List<EmailMessage> emailList = new ArrayList<>();
////        List<User> users = userServiceImpl.getUsersWithImmediateEmailPreference();
//        List<User> users = userServiceImpl.getAll(new FacetFilter()).getResults(); // FIXME: sends e-mails to everyone
//        for (Iterator<EmailMessage> iterator = emails.iterator(); iterator.hasNext(); ) {
//            EmailMessage email = iterator.next();
//            User user = getUserByEmail(users, email.getRecipient());
//            if (user != null) {
//                if ("true".equals(user.getReceiveEmails()) && "true".equals(user.getImmediateEmails())) {
//                    emailList.add(email);
//                }
//            } else {
//                emailList.add(email);
//            }
//        }
//        return emailList;
//    }

    private List<EmailMessage> getEmailMessages(BaseInfo baseInfo, UserType type, RequestState state) {
        List<EmailMessage> messages = new ArrayList<>();

        Request request = requestService.get(baseInfo.getRequestId());

        RequestWrapper appStages = new RequestWrapper(request,baseInfo);
        String completedStage = null;
        try {
            if (state == REVIEW) { // if stage is under review
                // the completed stage is the stage after the request.getStage()
                completedStage = RequestWrapper.getNextStage(baseInfo);
            } else { // if stage is not under review
                // the completed stage is always one stage behind request.getStage()
                completedStage = RequestWrapper.getPreviousStage(baseInfo);
            }

        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }

        /*String date = appStages.getDate(completedStage);
        User user = appStages.getUser(completedStage); // get User of completed stage (previous stage)*/

        if (type == USER) {
            messages.add(
                    createEmail(request.getUser().getEmail(), /*user.getFirstname(), user.getLastname(),*/ baseInfo, type, state/*, date*/));
        } else if (type == UserType.previousPersonOfInterest) {
            List<PersonOfInterest> PersonOfInterest = appStages.getPersonsOfInterest(completedStage); // get PersonOfInterests of completed stage (previous stage)
            List<String> addresses = new ArrayList<>();
            PersonOfInterest.forEach(person -> {
                addresses.add(person.getEmail());
                person.getDelegates().forEach(delegate -> addresses.add(delegate.getEmail()));
            });
            addresses.forEach(address -> messages.add(
                    createEmail(address, /*user.getFirstname(), user.getLastname(),*/ baseInfo, type, state/*, date*/)));
        } else if (type == UserType.nextPersonOfInterest) {
            List<PersonOfInterest> PersonOfInterests;
            PersonOfInterests = appStages.getPersonsOfInterest(baseInfo.getStage()); // get PersonOfInterests of next stage
            List<String> addresses = new ArrayList<>();
            PersonOfInterests.forEach(person -> {
                addresses.add(person.getEmail());
                person.getDelegates().forEach(delegate -> addresses.add(delegate.getEmail()));
            });
            addresses.forEach(address -> messages.add(
                    createEmail(address, /*user.getFirstname(), user.getLastname(),*/ baseInfo, type, state/*, date*/)));
        }
        return messages;
    }

    private EmailMessage createEmail(String address, /*String firstname, String lastname,*/ BaseInfo baseInfo, UserType type,
                                     RequestState state/*, String date_secs*/) {
        String subject = "[ARC-REQUEST] Αίτημα " + baseInfo.getId();

        Request request = requestService.get(baseInfo.getRequestId());

        RequestWrapper appStages = new RequestWrapper(request,baseInfo);
        StringBuilder stringBuilder = new StringBuilder();
        /*String date = "";
        if (date_secs != null) {
            date = new SimpleDateFormat("dd/MM/yyyy").format(new Date(Long.parseLong(date_secs)));
        }*/
        if (type == USER) {
            if (state == INITIALIZED) {
                subject = "[ARC-ν.4485] Δημιουργία νέου αιτήματος " + baseInfo.getId();
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημά σας έχει υποβληθεί επιτυχώς.");
            } else if (state == ACCEPTED) {
                subject = "[ARC-ν.4485] Ολοκλήρωση σταδίου αιτήματος " + baseInfo.getId();
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημά σας έχει εγκριθεί από τον επιστημονικό υπεύθυνο κι\n")
                        .append("έχει προωθηθεί στις διοικητικές υπηρεσίες του κέντρου για επεξεργασία.");
            } else if (state == INVOICE) {
                subject = "[ARC-ν.4485] Ολοκλήρωση σταδίου αιτήματος " + baseInfo.getId();
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημά σας έχει εγκριθεί από τις διοικητικές υπηρεσίες του κέντρου.\n")
                        .append("Μπορείτε να προχωρήσετε στην πραγματοποίηση της δαπάνης και παρακαλείστε\n")
                        .append("να μεταφορτώσετε το τιμολόγιό σας στο σύστημα.");
            } else if (state == ACCEPTED_DIAVGEIA) {
                subject = "[ARC-ν.4485] Ολοκλήρωση σταδίου αιτήματος " + baseInfo.getId();
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημά σας έχει εγκριθεί από τις διοικητικές υπηρεσίες του κέντρου\n")
                        .append("και θα αναρτηθεί στη διαύγεια.");

            } else if (state == COMPLETED) {
                subject = "[ARC-ν.4485] Διεκπεραίωση του αιτήματος " + baseInfo.getId();
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημά σας έχει ολοκληρωθεί.\n");

            } else if (state == RequestState.REJECTED) {
                subject = "[ARC-ν.4485] Απόρριψη του αιτήματος " + baseInfo.getId();
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημά σας έχει απορριφθεί.");
//                if (appStages.getComment(request.getStage()) != null) {
//                    stringBuilder
//                            .append("\n\nΑιτία: ")
//                            .append(appStages.getComment(request.getStage()));
//                }

            } else if (state == REVIEW) {
                subject = "[ARC-ν.4485] Ολοκλήρωση σταδίου αιτήματος " + baseInfo.getId();
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημά σας έχει επιστραφεί από τον επιστημονικό υπεύθυνο\n")
                        .append("για να προβείτε στις απαραίτητες διορθώσεις.");

//                if (appStages.getComment(request.getStage()) != null) {
//                    stringBuilder
//                            .append("\n\nΣχόλια: ")
//                            .append(appStages.getComment(request.getStage()));
//                }
            }
        } else if (type == UserType.previousPersonOfInterest) {
            subject = "[ARC-ν.4485] Ολοκλήρωση σταδίου αιτήματος " + baseInfo.getId();
            if (state == ACCEPTED) {
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημα προχώρησε στο επόμενο στάδιο.");

            } else if (state == ACCEPTED_DIAVGEIA) {
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημα προχώρησε στο επόμενο στάδιο.");

            } else if (state == COMPLETED) {
                subject = "[ARC-ν.4485] Διεκπεραίωση του αιτήματος " + baseInfo.getId();
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημα έχει ολοκληρωθεί.");
            } else if (state == RequestState.REJECTED) {
                subject = "[ARC-ν.4485] Απόρριψη του αιτήματος " + baseInfo.getId();
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημα έχει απορριφθεί.");
//                if (appStages.getComment(request.getStage()) != null) {
//                    stringBuilder
//                            .append("\n\nΑιτία: ")
//                            .append(appStages.getComment(request.getStage()));
//                }

            } else if (state == REVIEW) {
                subject = "[ARC-ν.4485] Επανέλεγχος του αιτήματος " + baseInfo.getId();
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημα με έχει επιστραφεί \n")
                        .append("για να προβείτε στις απαραίτητες διορθώσεις.");
//                if (appStages.getComment(request.getStage()) != null) {
//                    stringBuilder
//                            .append("\n\nΣχόλιο: ")
//                            .append(appStages.getComment(request.getStage()));
//                }
            }
        } else if (type == UserType.nextPersonOfInterest) {
            subject = "[ARC-ν.4485] Αναμονή ενεργειών για το αιτήμα " + baseInfo.getId();
            if (state == INITIALIZED) {
                subject = "[ARC-ν.4485] Υποβολή αιτήματος " + baseInfo.getId();
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("έχει υποβληθεί ένα νέο αίτημα στην πλατφόρμα και αναμένει τις ενέργειές σας.");
            } else if (state == ACCEPTED) {
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημα βρίσκεται σε αναμονή για τις ενέργειές σας.");

            } else if (state == ACCEPTED_DIAVGEIA) {
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημα με βρίσκεται σε αναμονή για τις ενέργειές σας.");
            } else if (state == REVIEW) {
                subject = "[ARC-ν.4485] Επανέλεγχος αιτήματος " + baseInfo.getId();
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημα βρίσκεται υπό επανέλεγχο.");
//                if (appStages.getComment(request.getStage()) != null) {
//                    stringBuilder
//                            .append("\n\nΣχόλιο: ")
//                            .append(appStages.getComment(request.getStage()));
//                }
            } else if (state == COMPLETED) {
                stringBuilder.append("");
            } else if (state == RequestState.REJECTED) {
                stringBuilder.append("");
            }
        }

        return new EmailMessage(address, subject, stringBuilder.toString() + getRequestInfo(request));
    }

    public String getRequestInfo(Request request) {
        final String euro = "\u20ac";
        StringBuilder requestInfo = new StringBuilder();
        String date = null;

        if (request.getStage1().getRequestDate() != null) {
            date = new SimpleDateFormat("dd/MM/yyyy").format(new Date(Long.parseLong(request.getStage1().getRequestDate())));
        }

        requestInfo
                .append("\n\nΑριθμός πρωτοκόλου: ")
                .append(request.getId())
                .append("\nΈργο: ")
                .append(request.getProject().getAcronym())
                .append("\nΗμερομηνία: ")
                .append(date)
                .append("\nΠοσό: " + euro)
                .append(request.getStage1().getAmountInEuros())
                .append("\nΘέμα: ")
                .append(request.getStage1().getSubject())
                .append("\n\nΜπορείτε να παρακολουθήσετε την εξέλιξή του ακολουθώντας τον παρακάτω σύνδεσμο: \n")
                .append(createLink(request.getId()));

        return requestInfo.toString();
    }

    private String createLink(String id) {
        return url + id;
    }
}
