package arc.expenses.service;

import arc.expenses.domain.RequestFatClass;
import arc.expenses.mail.EmailMessage;
import arc.expenses.messages.StageMessages;
import gr.athenarc.domain.Delegate;
import gr.athenarc.domain.POI;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static arc.expenses.service.EmailService.RequestState.*;
import static arc.expenses.service.EmailService.UserType.*;


@Component
public class EmailService {

    private final static org.apache.logging.log4j.Logger logger = LogManager.getLogger(StageMessages.class);


    public enum UserType {USER, previousPOI, nextPOI}

    public enum RequestState {INITIALIZED, ACCEPTED, INVOICE, ACCEPTED_DIAVGEIA, REVIEW, REJECTED, COMPLETED}

    @Value("${request.url}")
    String url;


    private EmailMessage createEmail(String email, String id,
                                     UserType type,
                                     RequestState state) {

        String subject = "[ARC-REQUEST] Αίτημα " + id;


        StringBuilder stringBuilder = new StringBuilder();

        if (type == UserType.USER) {
            if (state == RequestState.INITIALIZED) {
                subject = "[ARC-ν.4485] Δημιουργία νέου αιτήματος " + id;
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημά σας έχει υποβληθεί επιτυχώς.");
            } else if (state == RequestState.ACCEPTED) {
                subject = "[ARC-ν.4485] Ολοκλήρωση σταδίου αιτήματος " + id;
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημά σας έχει εγκριθεί από τον επιστημονικό υπεύθυνο κι\n")
                        .append("έχει προωθηθεί στις διοικητικές υπηρεσίες του κέντρου για επεξεργασία.");
            } else if (state == RequestState.INVOICE) {
                subject = "[ARC-ν.4485] Ολοκλήρωση σταδίου αιτήματος " + id;
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημά σας έχει εγκριθεί από τις διοικητικές υπηρεσίες του κέντρου.\n")
                        .append("Μπορείτε να προχωρήσετε στην πραγματοποίηση της δαπάνης και παρακαλείστε\n")
                        .append("να μεταφορτώσετε το τιμολόγιό σας στο σύστημα.");
            } else if (state == RequestState.ACCEPTED_DIAVGEIA) {
                subject = "[ARC-ν.4485] Ολοκλήρωση σταδίου αιτήματος " + id;
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημά σας έχει εγκριθεί από τις διοικητικές υπηρεσίες του κέντρου\n")
                        .append("και θα αναρτηθεί στη διαύγεια.");

            } else if (state == RequestState.COMPLETED) {
                subject = "[ARC-ν.4485] Διεκπεραίωση του αιτήματος " + id;
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημά σας έχει ολοκληρωθεί.\n");

            } else if (state == RequestState.REJECTED) {
                subject = "[ARC-ν.4485] Απόρριψη του αιτήματος " + id;
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημά σας έχει απορριφθεί.");

            } else if (state == RequestState.REVIEW) {
                subject = "[ARC-ν.4485] Ολοκλήρωση σταδίου αιτήματος " + id;
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημά σας έχει επιστραφεί από τον επιστημονικό υπεύθυνο\n")
                        .append("για να προβείτε στις απαραίτητες διορθώσεις.");

            }
        } else if (type == UserType.previousPOI) {
            subject = "[ARC-ν.4485] Ολοκλήρωση σταδίου αιτήματος " + id;
            if (state == RequestState.ACCEPTED) {
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημα προχώρησε στο επόμενο στάδιο.");

            } else if (state == RequestState.ACCEPTED_DIAVGEIA) {
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημα προχώρησε στο επόμενο στάδιο.");

            } else if (state == RequestState.COMPLETED) {
                subject = "[ARC-ν.4485] Διεκπεραίωση του αιτήματος " + id;
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημα έχει ολοκληρωθεί.");
            } else if (state == RequestState.REJECTED) {
                subject = "[ARC-ν.4485] Απόρριψη του αιτήματος " + id;
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημα έχει απορριφθεί.");

            } else if (state == RequestState.REVIEW) {
                subject = "[ARC-ν.4485] Επανέλεγχος του αιτήματος " + id;
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημα με έχει επιστραφεί \n")
                        .append("για να προβείτε στις απαραίτητες διορθώσεις.");
            }
        } else if (type == UserType.nextPOI) {
            subject = "[ARC-ν.4485] Αναμονή ενεργειών για το αιτήμα " + id;
            if (state == RequestState.INITIALIZED) {
                subject = "[ARC-ν.4485] Υποβολή αιτήματος " + id;
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("έχει υποβληθεί ένα νέο αίτημα στην πλατφόρμα και αναμένει τις ενέργειές σας.");
            } else if (state == RequestState.ACCEPTED) {
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημα βρίσκεται σε αναμονή για τις ενέργειές σας.");

            } else if (state == RequestState.ACCEPTED_DIAVGEIA) {
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημα με βρίσκεται σε αναμονή για τις ενέργειές σας.");
            } else if (state == RequestState.REVIEW) {
                subject = "[ARC-ν.4485] Επανέλεγχος αιτήματος " + id;
                stringBuilder
                        .append("Αγαπητέ χρήστη,\n\n")
                        .append("το ακόλουθο αίτημα βρίσκεται υπό επανέλεγχο.");
            } else if (state == RequestState.COMPLETED) {
                stringBuilder.append("");
            } else if (state == RequestState.REJECTED) {
                stringBuilder.append("");
            }
        }

        return new EmailMessage(email, subject, stringBuilder.toString() /*+ getRequestInfo(request)*/);
    }

    public List<EmailMessage> prepareMessages(String oldStage, String newStage, String status, RequestFatClass requestFatClass) {


        List<EmailMessage> messages = new ArrayList<>();
        List<Delegate> delegates  = null;
        List<POI> pois  = new ArrayList<>();
        String transition;
        RequestState state = ACCEPTED;
        String email;

        if ("rejected".equals(status)) {
            state = REJECTED;
            transition = "rejected";

        } else if ("under_review".equals(status)) {
            // TODO: add explicitly cases requiring special handling
            switch (newStage) {
                case "1":
                    transition = newStage + "<-";
                    break;
                default:
                    transition = newStage + "<-" + oldStage;
            }
            state = REVIEW;
        } else {
            transition = oldStage + "->" + newStage;
        }

        switch (transition) {
            // TODO: add explicitly cases requiring special handling
            case "1->2":
                if (state == ACCEPTED) {
                    state = INITIALIZED;
                }
                /*email to requester*/
                email = requestFatClass.getUser().getEmail();
                messages.add(createEmail(email, requestFatClass.getRequest_id(), USER, state));
                /*email to pois/delegates of stage 2*/
                email = requestFatClass.getProject().getScientificCoordinator().getEmail();
                messages.add(createEmail(email, requestFatClass.getRequest_id(), nextPOI, state));

                delegates = requestFatClass.getProject().getScientificCoordinator().getDelegates();
                for (Delegate delegate : delegates)
                    messages.add(createEmail(delegate.getEmail(), requestFatClass.getRequest_id(), nextPOI, state));

                break;

            case "6->7":
                if (state == ACCEPTED) {
                    state = ACCEPTED_DIAVGEIA;
                }
                /*email to requester*/
                email = requestFatClass.getUser().getEmail();
                messages.add(createEmail(email, requestFatClass.getRequest_id(), USER, state));


                /* email to pois/delegates of stage 6  */
                email = requestFatClass.getProject().getInstitute().getDiaugeia().getEmail();
                messages.add(createEmail(email, requestFatClass.getRequest_id(), previousPOI, state));

                delegates = requestFatClass.getProject().getInstitute().getDiaugeia().getDelegates();
                for (Delegate delegate : delegates)
                    messages.add(createEmail(delegate.getEmail(), requestFatClass.getRequest_id(), previousPOI, state));

                /*email to pois/delegates of stage 7*/
                List<POI> operators = requestFatClass.getProject().getOperator();
                for (POI operator : operators)
                    messages.add(createEmail(operator.getEmail(), requestFatClass.getRequest_id(), nextPOI, state));

                break;

//
//            case "13->13":
//                if (state == ACCEPTED) {
//                    state = COMPLETED;
//                }
//                if (state != REVIEW) { // TODO: probably remove, REVIEW is only for '<-' cases
//                    // email to USER
//                    emails.addAll(getEmailMessages(baseInfo, USER, state));
//                }
//                // email to previousPOI and delegates
//                emails.addAll(getEmailMessages(baseInfo, StageMessages.UserType.previousPOI, state));
//                break;
//
            case "1<-":
                /*email to requester*/
                email = requestFatClass.getUser().getEmail();
                messages.add(createEmail(email, requestFatClass.getRequest_id(), USER, state));
                // email to previousPOI and delegates
//                emails.addAll(getEmailMessages(baseInfo, StageMessages.UserType.previousPOI, state));
                break;
            case "12<-13":
                /*13*/
                email = requestFatClass.getProject().getInstitute().getAccountingPayment().getEmail();
                messages.add(createEmail(email, requestFatClass.getRequest_id(), nextPOI, state));

                delegates = requestFatClass.getProject().getInstitute().getAccountingPayment().getDelegates();
                for (Delegate delegate : delegates)
                    messages.add(createEmail(delegate.getEmail(), requestFatClass.getRequest_id(), nextPOI, state));
                /*12*/
                email = requestFatClass.getProject().getInstitute().getAccountingRegistration().getEmail();
                messages.add(createEmail(email, requestFatClass.getRequest_id(), previousPOI, state));

                delegates = requestFatClass.getProject().getInstitute().getAccountingRegistration().getDelegates();
                for (Delegate delegate : delegates)
                    messages.add(createEmail(delegate.getEmail(), requestFatClass.getRequest_id(), previousPOI, state));

                break;
            case "11<-12":
                /*12*/
                email = requestFatClass.getProject().getInstitute().getAccountingRegistration().getEmail();
                messages.add(createEmail(email, requestFatClass.getRequest_id(), nextPOI, state));

                delegates = requestFatClass.getProject().getInstitute().getAccountingRegistration().getDelegates();
                for (Delegate delegate : delegates)
                    messages.add(createEmail(delegate.getEmail(), requestFatClass.getRequest_id(), nextPOI, state));

                /*11*/
                email = requestFatClass.getProject().getInstitute().getDiaugeia().getEmail();
                messages.add(createEmail(email, requestFatClass.getRequest_id(), previousPOI, state));

                delegates = requestFatClass.getProject().getInstitute().getDiaugeia().getDelegates();
                for (Delegate delegate : delegates)
                    messages.add(createEmail(delegate.getEmail(), requestFatClass.getRequest_id(), previousPOI, state));

                break;
            case "10<-11":
                /*11*/
                email = requestFatClass.getProject().getInstitute().getDiaugeia().getEmail();
                messages.add(createEmail(email, requestFatClass.getRequest_id(), nextPOI, state));

                delegates = requestFatClass.getProject().getInstitute().getDiaugeia().getDelegates();
                for (Delegate delegate : delegates)
                    messages.add(createEmail(delegate.getEmail(), requestFatClass.getRequest_id(), nextPOI, state));
                /*10*/
                email = requestFatClass.getProject().getInstitute().getOrganization().getDirector().getEmail();
                messages.add(createEmail(email, requestFatClass.getRequest_id(), previousPOI, state));

                delegates = requestFatClass.getProject().getInstitute().getOrganization().getDirector().getDelegates();
                for (Delegate delegate : delegates)
                    messages.add(createEmail(delegate.getEmail(), requestFatClass.getRequest_id(), previousPOI, state));
                break;
            case "9<-10":
                /*10*/
                email = requestFatClass.getProject().getInstitute().getOrganization().getDirector().getEmail();
                messages.add(createEmail(email, requestFatClass.getRequest_id(), nextPOI, state));

                delegates = requestFatClass.getProject().getInstitute().getOrganization().getDirector().getDelegates();
                for (Delegate delegate : delegates)
                    messages.add(createEmail(delegate.getEmail(), requestFatClass.getRequest_id(), nextPOI, state));
                /*9*/
                email = requestFatClass.getProject().getInstitute().getOrganization().getPOI().getEmail();
                messages.add(createEmail(email, requestFatClass.getRequest_id(), previousPOI, state));

                delegates = requestFatClass.getProject().getInstitute().getOrganization().getPOI().getDelegates();
                for (Delegate delegate : delegates)
                    messages.add(createEmail(delegate.getEmail(), requestFatClass.getRequest_id(), previousPOI, state));
                break;
            case "8<-9":
                /*9*/
                email = requestFatClass.getProject().getInstitute().getOrganization().getPOI().getEmail();
                messages.add(createEmail(email, requestFatClass.getRequest_id(), nextPOI, state));

                delegates = requestFatClass.getProject().getInstitute().getOrganization().getPOI().getDelegates();
                for (Delegate delegate : delegates)
                    messages.add(createEmail(delegate.getEmail(), requestFatClass.getRequest_id(), nextPOI, state));
                /*8*/
                pois = requestFatClass.getProject().getInstitute().getOrganization().getInspectionTeam();
                messages.addAll(prepareMessages(requestFatClass, pois, state, previousPOI));
                break;
            case "7<-8":
                /*8*/
                pois = requestFatClass.getProject().getInstitute().getOrganization().getInspectionTeam();
                messages.addAll(prepareMessages(requestFatClass,pois, state, nextPOI));
                pois.clear();
                /*7*/
                pois = requestFatClass.getProject().getOperator();
                messages.addAll(prepareMessages(requestFatClass,pois, state, previousPOI));
                pois.clear();
                break;
            case "6<-7":
                /*7*/
                pois = requestFatClass.getProject().getOperator();
                messages.addAll(prepareMessages(requestFatClass,pois, state, nextPOI));
                pois.clear();
                /*6*/
                pois.add(requestFatClass.getProject().getInstitute().getDiaugeia());
                messages.addAll(prepareMessages(requestFatClass,pois, state, previousPOI));
                pois.clear();
                break;
            case "5b<-6":
                /*6*/
                pois.add(requestFatClass.getProject().getInstitute().getDiaugeia());
                messages.addAll(prepareMessages(requestFatClass,pois, state, nextPOI));
                pois.clear();
                /*5b*/
                pois.add(requestFatClass.getProject().getInstitute().getOrganization().getDioikitikoSumvoulio());
                messages.addAll(prepareMessages(requestFatClass,pois, state, previousPOI));
                pois.clear();
                break;
            case "5a<-6":
                /*6*/
                pois.add(requestFatClass.getProject().getInstitute().getDiaugeia());
                messages.addAll(prepareMessages(requestFatClass,pois, state, nextPOI));
                pois.clear();
                /*5a*/
                pois.add(requestFatClass.getProject().getInstitute().getOrganization().getDirector());
                messages.addAll(prepareMessages(requestFatClass,pois, state, previousPOI));
                pois.clear();
                break;
            case "5a<-5b":
                /*5b*/
                pois.add(requestFatClass.getProject().getInstitute().getOrganization().getDioikitikoSumvoulio());
                messages.addAll(prepareMessages(requestFatClass,pois, state, previousPOI));
                pois.clear();
                /*5a*/
                pois.add(requestFatClass.getProject().getInstitute().getOrganization().getDirector());
                messages.addAll(prepareMessages(requestFatClass,pois, state, nextPOI));
                pois.clear();
                break;
            case "5<-5b":
                /*5b*/
                pois.add(requestFatClass.getProject().getInstitute().getOrganization().getDioikitikoSumvoulio());
                messages.addAll(prepareMessages(requestFatClass,pois, state, previousPOI));
                pois.clear();
                /*5*/
                pois.add(requestFatClass.getProject().getInstitute().getDirector());
                messages.addAll(prepareMessages(requestFatClass,pois, state, nextPOI));
                pois.clear();
                break;
            case "4<-5a":
                /*5a*/
                pois.add(requestFatClass.getProject().getInstitute().getOrganization().getDirector());
                messages.addAll(prepareMessages(requestFatClass,pois, state, previousPOI));
                pois.clear();
                /*4*/
                pois.add(requestFatClass.getProject().getInstitute().getOrganization().getPOI());
                messages.addAll(prepareMessages(requestFatClass,pois, state, nextPOI));
                pois.clear();
                break;
            case "4<-5":
                /*5*/
                pois.add(requestFatClass.getProject().getInstitute().getDirector());
                messages.addAll(prepareMessages(requestFatClass,pois, state, previousPOI));
                pois.clear();
                /*4*/
                pois.add(requestFatClass.getProject().getInstitute().getOrganization().getPOI());
                messages.addAll(prepareMessages(requestFatClass,pois, state, nextPOI));
                pois.clear();
                break;
            case "3<-4":
                /*4*/
                pois.add(requestFatClass.getProject().getInstitute().getOrganization().getPOI());
                messages.addAll(prepareMessages(requestFatClass,pois, state, previousPOI));
                pois.clear();
                /*3*/
                pois = requestFatClass.getProject().getOperator();
                messages.addAll(prepareMessages(requestFatClass,pois, state, nextPOI));
                pois.clear();
                break;
            case "2<-3":
                /*3*/
                pois = requestFatClass.getProject().getOperator();
                messages.addAll(prepareMessages(requestFatClass,pois, state, previousPOI));
                pois.clear();
                /*2*/
                pois.add(requestFatClass.getProject().getScientificCoordinator());
                messages.addAll(prepareMessages(requestFatClass,pois, state, nextPOI));
                pois.clear();
                break;
            case "rejected":
                /*email to requester*/
                email = requestFatClass.getUser().getEmail();
                messages.add(createEmail(email, requestFatClass.getRequest_id(), USER, state));
                // email to POIs and delegates
                //emails.addAll(getEmailMessages(baseInfo, StageMessages.UserType.previousPOI, state));
                break;
        }
        messages.forEach(logger::info);
        return messages;
    }

    private List<EmailMessage> prepareMessages(RequestFatClass requestFatClass, List<POI> pois, RequestState state, UserType nextPOI) {

        List<Delegate> delegates;
        List<EmailMessage> emails = new ArrayList<>();

        for (POI poi : pois) {
            emails.add(createEmail(poi.getEmail(), requestFatClass.getRequest_id(), nextPOI, state));
            delegates = poi.getDelegates();
            for (Delegate delegate : delegates)
                emails.add(createEmail(delegate.getEmail(), requestFatClass.getRequest_id(), nextPOI, state));
        }
        return emails;
    }
}
