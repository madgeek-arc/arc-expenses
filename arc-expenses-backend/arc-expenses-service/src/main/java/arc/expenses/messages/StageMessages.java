package arc.expenses.messages;

import arc.expenses.RequestWrapper;
import arc.expenses.service.GenericService;
import arc.expenses.service.UserServiceImpl;
import eu.openminted.registry.core.domain.FacetFilter;
import gr.athenarc.domain.POI;
import gr.athenarc.domain.Request;
import arc.expenses.mail.EmailMessage;
import gr.athenarc.domain.User;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class StageMessages {
    public enum UserType {USER, previousPOI, nextPOI}

    public enum RequestState {INITIALIZED, ACCEPTED, INVOICE, ACCEPTED_DIAVGEIA, REVIEW, REJECTED, COMPLETED}

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
        String subject = "[ARC-REQUEST] Αίτημα " + request.getId();
        RequestState state = RequestState.ACCEPTED;

        logger.info("Request Link: " + url + request.getId());

        if ("rejected".equals(request.getStatus())) {
            state = RequestState.REJECTED;
            transition = "rejected";

        } else if ("under_review".equals(request.getStatus())) {
            // TODO: add explicitly cases requiring special handling
            switch (nextStage) {
                case "1":
                    transition = nextStage + "<-";
                    break;
                default:
                    transition = "<-";
            }
            state = RequestState.REVIEW;
        } else {
            transition = prevStage + "->" + nextStage;
        }

        switch (transition) {
            // TODO: add explicitly cases requiring special handling
            case "1->2":
                if (state == RequestState.ACCEPTED) {
                    state = RequestState.INITIALIZED;
                }
                // email to USER
                emails.addAll(getEmailMessages(request, UserType.USER, state, subject));
                // email to nextPOIs and delegates
                emails.addAll(getEmailMessages(request, UserType.nextPOI, state, subject));
                break;

            case "5a->UplodInvoice":
                // email to USER
                emails.addAll(getEmailMessages(request, UserType.USER, RequestState.INVOICE, subject));
                // email to previousPOI and delegates
                emails.addAll(getEmailMessages(request, UserType.previousPOI, state, subject));
//                // email to nextPOI and delegates
//                emails.addAll(getEmailMessages(request, UserType.nextPOI, state, subject));
                break;

            case "UploadInvoice<-8":
                // email to USER
                emails.addAll(getEmailMessages(request, UserType.USER, RequestState.INVOICE, subject));
                // email to previousPOI and delegates
                emails.addAll(getEmailMessages(request, UserType.previousPOI, state, subject));
//                // email to nextPOI and delegates
//                emails.addAll(getEmailMessages(request, UserType.nextPOI, state, subject));
                break;

            case "6->7":
                if (state == RequestState.ACCEPTED) {
                    state = RequestState.ACCEPTED_DIAVGEIA;
                }
                // email to USER
                emails.addAll(getEmailMessages(request, UserType.USER, state, subject));
                // email to previousPOI and delegates
                emails.addAll(getEmailMessages(request, UserType.previousPOI, state, subject));
                // email to nextPOI and delegates
                emails.addAll(getEmailMessages(request, UserType.nextPOI, state, subject));
                break;

            case "13->13":
                if (state == RequestState.ACCEPTED) {
                    state = RequestState.COMPLETED;
                }
                if (state != RequestState.REVIEW) { // TODO: probably remove, REVIEW is only for '<-' cases
                    // email to USER
                    emails.addAll(getEmailMessages(request, UserType.USER, state, subject));
                }
                // email to previousPOI and delegates
                emails.addAll(getEmailMessages(request, UserType.previousPOI, state, subject));
                break;

            case "1<-":
                // email to USER
                emails.addAll(getEmailMessages(request, UserType.USER, state, subject));
                // email to previousPOI and delegates
                emails.addAll(getEmailMessages(request, UserType.previousPOI, state, subject));
                break;

//            case "12<-13":
//
//                break;

            case "<-":
                // email to previousPOI and delegates
                emails.addAll(getEmailMessages(request, UserType.previousPOI, state, subject));
                // email to nextPOI and delegates
                emails.addAll(getEmailMessages(request, UserType.nextPOI, state, subject));
                break;

            case "rejected":
                // email to USER
                emails.addAll(getEmailMessages(request, UserType.USER, state, subject));
                // email to POIs and delegates
                emails.addAll(getEmailMessages(request, UserType.previousPOI, state, subject));
                break;

            case "UploadInvoice->8":
                // probably default case works // TODO check this !!!
            default:
                // email to POIs and delegates
                emails.addAll(getEmailMessages(request, UserType.previousPOI, state, subject));
                // email to nextPOI and delegates
                emails.addAll(getEmailMessages(request, UserType.nextPOI, state, subject));
        }
        return emails;
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
                if ("true".equals(user.getReceiveEmails()) && "true".equals(user.getImmediateEmails())) {
                    emailList.add(email);
                }
            } else {
                emailList.add(email);
            }
        }
        return emailList;
    }

    List<EmailMessage> getEmailMessages(Request request, UserType type, RequestState state, String subject) {
        List<EmailMessage> messages = new ArrayList<>();
        RequestWrapper appStages = new RequestWrapper(request);
        String completedStage = null;
        try {
            if (state == RequestState.REVIEW) { // if stage is under review
                // the completed stage is the stage after the request.getStage()
                completedStage = RequestWrapper.getNextStage(request);
            } else { // if stage is not under review
                // the completed stage is always one stage behind request.getStage()
                completedStage = RequestWrapper.getPreviousStage(request);
            }

        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        String date = appStages.getDate(completedStage);

        if (type == UserType.USER) {
            User user = appStages.getUser(completedStage); // get User of completed stage (previous stage)
            messages.add(new EmailMessage(request.getRequester().getEmail(), subject,
                    messageTemplates(user.getFirstname(), user.getLastname(), request, type, state, date)));
        } else if (type == UserType.previousPOI) {
            User user = appStages.getUser(completedStage); // get User of completed stage (previous stage)
            List<POI> poi = appStages.getPersonsOfInterest(completedStage); // get POIs of completed stage (previous stage)
            List<String> addresses = new ArrayList<>();
            poi.forEach(person -> {
                addresses.add(person.getEmail());
                person.getDelegates().forEach(delegate -> addresses.add(delegate.getEmail()));
            });
            addresses.forEach(address -> messages.add(new EmailMessage(address, subject,
                    messageTemplates(user.getFirstname(), user.getLastname(), request, type, state, date))));
        } else if (type == UserType.nextPOI) {
            User user = appStages.getUser(completedStage); // get User of completed stage (previous stage)
            List<POI> pois;
            pois = appStages.getPersonsOfInterest(request.getStage()); // get POIs of next stage
            List<String> addresses = new ArrayList<>();
            pois.forEach(person -> {
                addresses.add(person.getEmail());
                person.getDelegates().forEach(delegate -> addresses.add(delegate.getEmail()));
            });
            addresses.forEach(address -> messages.add(new EmailMessage(address, subject,
                    messageTemplates(user.getFirstname(), user.getLastname(), request, type, state, date))));
        } else {

        }
        return messages;
    }

    private String messageTemplates(String firstname, String lastname, Request request, UserType type,
                                    RequestState state, String date_secs) {
        String subject;
        String messageText = null;
        String date = "";
        String id = request.getId();
        RequestWrapper appStages = new RequestWrapper(request);
        StringBuilder stringBuilder = new StringBuilder();
        if (date_secs != null) {
            date = new SimpleDateFormat("dd/MM/yyyy").format(new Date(Long.parseLong(date_secs))).toString();
        }
        String link = createLink(id);
        // TODO:
//        switch (state) {
//            case INITIALIZED:
//                switch (type) {
//                    case USER:
//                        stringBuilder
//                                .append("Αγαπητέ χρήστη,\n\n")
//                                .append("το ακόλουθο αίτημά σας με αριθμό πρωτοκόλλου ")
//                                .append(id)
//                                .append(" έχει υποβληθεί επιτυχώς.");
//                        break;
//
//                    case nextPOI:
//
//                        break;
//                }
//                break;
//
//            case INVOICE:
//                stringBuilder
//                        .append("Αγαπητέ χρήστη,\n\n")
//                        .append("το ακόλουθο αίτημά σας με αριθμό πρωτοκόλλου ")
//                        .append(id)
//                        .append(" έχει εγκριθεί από τις διοικητικές υπηρεσίες του κέντρου.\n")
//                        .append("Μπορείτε να προχωρήσετε στην πραγματοποίηση της δαπάνης και παρακαλείστε να να μεταφορτώσετε το τιμολόγιό σας στο σύστημα.");
//                break;
//
//            case ACCEPTED_DIAVGEIA:
//                switch (type) {
//                    case USER:
//                        stringBuilder
//                                .append("Αγαπητέ χρήστη,\n\n")
//                                .append("το ακόλουθο αίτημά σας με αριθμό πρωτοκόλλου ")
//                                .append(id)
//                                .append(" έχει εγκριθεί από τον επιστημονικό\n")
//                                .append("υπεύθυνο και έχει προωθηθεί στις διοικητικές υπηρεσίες του κέντρου για επεξεργασία.");
//                        break;
//
//                    case previousPOI:
//
//                        break;
//
//                    case nextPOI:
//
//                        break;
//                }
//                break;
//
//            case REVIEW:
//                switch (type) {
//                    case USER:
//                        stringBuilder
//                                .append("Αγαπητέ χρήστη,\n\n")
//                                .append("το ακόλουθο αίτημά σας με αριθμό πρωτοκόλλου ")
//                                .append(id)
//                                .append(" έχει επιστραφεί από τον επιστημονικό\n")
//                                .append("υπεύθυνο για να προβείτε στις απαραίτητες διορθώσεις.");
//                        break;
//
//                    case previousPOI:
//
//                        break;
//
//                    case nextPOI:
//
//                        break;
//                }
//                break;
//
//            case REJECTED:
//                switch (type) {
//                    case USER:
//                        stringBuilder
//                                .append("Αγαπητέ χρήστη,\n\n")
//                                .append("το ακόλουθο αίτημά σας με αριθμό πρωτοκόλλου ")
//                                .append(id)
//                                .append(" έχει απορριφθεί.");
//                        break;
//
//                    case previousPOI:
//
//                        break;
//                }
//                break;
//
//            case COMPLETED:
//                switch (type) {
//                    case USER:
//                        stringBuilder
//                                .append("Αγαπητέ χρήστη,\n\n")
//                                .append("το ακόλουθο αίτημά σας με αριθμό πρωτοκόλλου ")
//                                .append(id)
//                                .append(" έχει ολοκληρωθεί.\n");
//                        break;
//
//                    case previousPOI:
//
//                        break;
//                }
//                break;
//
//            case ACCEPTED:
//
//            default:
//                if (type == UserType.previousPOI) {
//
//                } else if (type == UserType.nextPOI) {
//
//                } else if (type == UserType.USER) {
//                    stringBuilder
//                            .append("Αγαπητέ χρήστη,\n\n")
//                            .append("το ακόλουθο αίτημά σας με αριθμό πρωτοκόλλου ")
//                            .append(id)
//                            .append(" έχει εγκριθεί από τον επιστημονικό\n")
//                            .append("υπεύθυνο και έχει προωθηθεί στις διοικητικές υπηρεσίες του κέντρου για επεξεργασία.");
//                }
//                break;
//        }
        if (type == UserType.USER) {
            if (state == RequestState.INITIALIZED) {
                subject = "[ARC-ν.4485] Ολοκλήρωση σταδίου αιτήματος " + request.getId();
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημά σας με αριθμό πρωτοκόλλου ")
                        .append(id)
                        .append(" έχει υποβληθεί επιτυχώς.");
            } else if (state == RequestState.ACCEPTED) {
                subject = "[ARC-ν.4485] Ολοκλήρωση σταδίου αιτήματος " + request.getId();
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημά σας με αριθμό πρωτοκόλλου ")
                        .append(id)
                        .append(" έχει εγκριθεί από τον επιστημονικό\n")
                        .append("υπεύθυνο και έχει προωθηθεί στις διοικητικές υπηρεσίες του κέντρου για επεξεργασία.");
            } else if (state == RequestState.INVOICE) {
                subject = "[ARC-ν.4485] Ολοκλήρωση σταδίου αιτήματος " + request.getId();
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημά σας με αριθμό πρωτοκόλλου ")
                        .append(id)
                        .append(" έχει εγκριθεί από τις διοικητικές υπηρεσίες του κέντρου.\n")
                        .append("Μπορείτε να προχωρήσετε στην πραγματοποίηση της δαπάνης και παρακαλείστε να να μεταφορτώσετε το τιμολόγιό σας στο σύστημα.");
            } else if (state == RequestState.ACCEPTED_DIAVGEIA) {
                subject = "[ARC-ν.4485] Ολοκλήρωση σταδίου αιτήματος " + request.getId();
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημά σας με αριθμό πρωτοκόλλου ")
                        .append(id)
                        .append(" έχει εγκριθεί από τον επιστημονικό\n")
                        .append("υπεύθυνο και έχει προωθηθεί στις διοικητικές υπηρεσίες του κέντρου για επεξεργασία.");

            } else if (state == RequestState.COMPLETED) {
                subject = "[ARC-ν.4485] Ολοκλήρωση σταδίου αιτήματος " + request.getId();
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημά σας με αριθμό πρωτοκόλλου ")
                        .append(id)
                        .append(" έχει ολοκληρωθεί.\n");

            } else if (state == RequestState.REJECTED) {
                subject = "[ARC-ν.4485] Ολοκλήρωση σταδίου αιτήματος " + request.getId();
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημά σας με αριθμό πρωτοκόλλου ")
                        .append(id)
                        .append(" έχει απορριφθεί.");
                if (appStages.getComment(request.getStage()) != null) {
                    stringBuilder
                            .append("\n\nΑιτία: ")
                            .append(appStages.getComment(request.getStage()));
                }

            } else if (state == RequestState.REVIEW) {
                subject = "[ARC-ν.4485] Ολοκλήρωση σταδίου αιτήματος " + request.getId();
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημά σας με αριθμό πρωτοκόλλου ")
                        .append(id)
                        .append(" έχει επιστραφεί από τον επιστημονικό\n")
                        .append("υπεύθυνο για να προβείτε στις απαραίτητες διορθώσεις.");

                if (appStages.getComment(request.getStage()) != null) {
                    stringBuilder
                            .append("\n\nΣχόλια: ")
                            .append(appStages.getComment(request.getStage()));
                }
            }
        } else if (type == UserType.previousPOI) {
            subject = "[ARC-ν.4485] Ολοκλήρωση σταδίου αιτήματος " + request.getId();
            if (state == RequestState.ACCEPTED) {
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημα με αριθμό πρωτοκόλλου ")
                        .append(id)
                        .append(" προχώρησε στο επόμενο στάδιο.");

            } else if (state == RequestState.ACCEPTED_DIAVGEIA) {
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημα με αριθμό πρωτοκόλλου ")
                        .append(id)
                        .append(" προχώρησε στο επόμενο στάδιο.");

            } else if (state == RequestState.COMPLETED) {
                subject = "[ARC-ν.4485] Διεκπεραίωση του αιτήματος " + request.getId();
                stringBuilder.append("Ολοκληρώθηκε το αίτημα με κωδικό ");
                stringBuilder.append(id);
                stringBuilder.append(" από τον/την ");
                stringBuilder.append(firstname);
                stringBuilder.append(" ");
                stringBuilder.append(lastname);
            } else if (state == RequestState.REJECTED) {
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημα με αριθμό πρωτοκόλλου ")
                        .append(id)
                        .append(" έχει απορριφθεί.");
                if (appStages.getComment(request.getStage()) != null) {
                    stringBuilder
                            .append("\n\nΑιτία: ")
                            .append(appStages.getComment(request.getStage()));
                }

            } else if (state == RequestState.REVIEW) {
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημα με αριθμό πρωτοκόλλου ")
                        .append(id)
                        .append(" έχει επιστραφεί \n")
                        .append("για να προβείτε στις απαραίτητες διορθώσεις.");
                if (appStages.getComment(request.getStage()) != null) {
                    stringBuilder
                            .append("\n\nΣχόλιο: ")
                            .append(appStages.getComment(request.getStage()));
                }
            }
        } else if (type == UserType.nextPOI) {
            subject = "[ARC-ν.4485] Αναμονή ενεργειών για το αιτήμα " + request.getId();
            if (state == RequestState.INITIALIZED) {
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("έχει υποβληθεί ένα νέο αίτημα με αριθμό πρωτοκόλλου ")
                        .append(id);
            } else if (state == RequestState.ACCEPTED) {
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημα με αριθμό πρωτοκόλλου ")
                        .append(id)
                        .append(" βρίσκεται σε αναμονή για τις ενέργειές σας.");

            } else if (state == RequestState.ACCEPTED_DIAVGEIA) {
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημα με αριθμό πρωτοκόλλου ")
                        .append(id)
                        .append(" βρίσκεται σε αναμονή για τις ενέργειές σας.");
            } else if (state == RequestState.REVIEW) {
                subject = "[ARC-ν.4485] Επανέλεγχος αιτήματος " + request.getId();
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημα με αριθμό πρωτοκόλλου ")
                        .append(id)
                        .append(" βρίσκεται υπό επανέλεγχο.");
                if (appStages.getComment(request.getStage()) != null) {
                    stringBuilder
                            .append("\n\nΣχόλιο: ")
                            .append(appStages.getComment(request.getStage()));
                }
            } else if (state == RequestState.COMPLETED) {
                stringBuilder.append("");
            } else if (state == RequestState.REJECTED) {
                stringBuilder.append("");
            }
        }

        return stringBuilder.toString() + getRequestInfo(request);
    }

    private String getRequestInfo(Request request) {
        StringBuilder requestInfo = new StringBuilder();

        requestInfo
                .append("\nΑριθμός πρωτοκόλου: ")
                .append(request.getId())
                .append("\nΈργο: ")
                .append(request.getProject().getAcronym())
                .append("\nΗμερομηνία: ")
                .append(request.getStage1().getRequestDate())
                .append("\nΠοσό: ")
                .append(request.getStage1().getAmountInEuros())
                .append("\nΘέμα: ")
                .append(request.getStage1().getSubject())
                .append("\n\nΜπορείτε να παρακολουθήσετε την εξέλιξή του ακολουθώντας τον ακόλουθο σύνδεσμο: ")
                .append(createLink(request.getId()));

        return requestInfo.toString();
    }

    private String createLink(String id) {
        return url + id;
    }
}
