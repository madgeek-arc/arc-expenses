package arc.expenses.service;

import arc.expenses.domain.RequestFatClass;
import arc.expenses.mail.EmailMessage;
import arc.expenses.messages.StageMessages;
import gr.athenarc.domain.Delegate;
import gr.athenarc.domain.PersonOfInterest;
import gr.athenarc.domain.Request;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static arc.expenses.service.EmailService.RequestState.*;
import static arc.expenses.service.EmailService.RequestType.*;
import static arc.expenses.service.EmailService.UserType.*;


@Component
public class EmailService {

    private final static org.apache.logging.log4j.Logger logger = LogManager.getLogger(StageMessages.class);

    @Autowired
    RequestServiceImpl requestService;

    public enum UserType {USER, previousPersonOfInterest, nextPersonOfInterest}

    public enum RequestType {TRAVEL, OTHER}

    public enum RequestState {INITIALIZED, ACCEPTED, INVOICE, ACCEPTED_DIAVGEIA, REVIEW, REJECTED, COMPLETED,CANCELLED}

    @Value("${request.approval.url}")
    String approval_url;

    @Value("${request.payment.url}")
    String payment_url;


    private EmailMessage createEmail(String email,
                                     String id,
                                     RequestType requestType,
                                     UserType type,
                                     RequestState state,
                                     RequestFatClass requestFatClass) {

        String subject = "[ARC-REQUEST] Αίτημα " + id;


        StringBuilder stringBuilder = new StringBuilder();

        if (type == UserType.USER) {

            if(state == RequestState.CANCELLED){
                if(requestType == OTHER){
                    subject = "[ARC-ν.4485] Ακύρωση αιτήματος " + id;
                    stringBuilder
                            .append("Αγαπητέ χρήστη,\n\n")
                            .append("το ακόλουθο αίτημα έχει ακυρωθεί.");
                }else{
                    //travel
                    subject = "[ARC-ν.4485] Ακύρωση αιτήματος " + id;
                    stringBuilder
                            .append("Αγαπητέ χρήστη,\n\n")
                            .append("Το ακόλουθο αίτημα σχετικά με το επικείμενο ταξίδι σας έχει ακυρωθεί.");
                }
            }else if (state == RequestState.INITIALIZED) {

                if(requestType == OTHER){
                    subject = "[ARC-ν.4485] Δημιουργία νέου αιτήματος " + id;
                    stringBuilder
                            .append("Αγαπητέ χρήστη,\n\n")
                            .append("το ακόλουθο αίτημά σας έχει υποβληθεί επιτυχώς.");
                }else{
                    //travel
                    subject = "[ARC-ν.4485] Δημιουργία νέου αιτήματος " + id;
                    stringBuilder
                            .append("Αγαπητέ χρήστη,\n\n")
                            .append("Το ακόλουθο αίτημα σχετικά με το επικείμενο ταξίδι σας έχει υποβληθεί στο σύστημα επιτυχώς.");
                }

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

                if(requestType == OTHER){
                    subject = "[ARC-ν.4485] Ολοκλήρωση σταδίου αιτήματος " + id;
                    stringBuilder
                            .append("Αγαπητέ χρήστη,\n\n")
                            .append("το ακόλουθο αίτημά σας έχει εγκριθεί από τις διοικητικές υπηρεσίες του κέντρου\n")
                            .append("και θα αναρτηθεί στη διαύγεια.");

                }else{
                    //travel
                    subject = "[ARC-ν.4485] Ολοκλήρωση σταδίου αιτήματος " + id;
                    stringBuilder
                            .append("Αγαπητέ χρήστη,\n\n")
                            .append("το ακόλουθο αίτημά σας έχει εγκριθεί από τις διοικητικές υπηρεσίες του κέντρου\n")
                            .append("και θα αναρτηθεί στη διαύγεια.");

                }

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
        } else if (type == UserType.previousPersonOfInterest) {
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
                        .append("το ακόλουθο αίτημα  έχει επιστραφεί \n")
                        .append("για να προβείτε στις απαραίτητες διορθώσεις.");
            }
        } else if (type == UserType.nextPersonOfInterest) {
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

        return new EmailMessage(email, subject, stringBuilder.toString() + getRequestInfo(requestFatClass));
    }

    public List<EmailMessage> prepareMessages(String oldStage, String newStage, String status, RequestFatClass requestFatClass) {


        List<EmailMessage> messages = new ArrayList<>();
        List<Delegate> delegates  = null;
        List<PersonOfInterest> PersonOfInterests  = new ArrayList<>();
        String transition ;
        RequestState state = ACCEPTED;
        String email;

        if("cancelled".equals(status)){
            state = CANCELLED;
            transition = "-";
            messages.addAll(sendCancelRequestEmails(requestFatClass,newStage,state,USER));
        }else if ("rejected".equals(status)) {
            state = REJECTED;
            transition = "rejected";
        }else if ("under_review".equals(status)) {
            transition =  "<-" ;
            state = REVIEW;
        }else
            transition = "->";
        /*1->2*/
        if(newStage.equals("2") && oldStage.equals("1")){
            if (state == ACCEPTED)
                state = INITIALIZED;

            messages.add(prepareMessageForRequester(requestFatClass,state));
            /*email to PersonOfInterests/delegates of stage 2*/
            messages.addAll(prepareMessages(requestFatClass,getPersonOfInterest(requestFatClass,newStage), state, nextPersonOfInterest));
            messages.forEach(logger::info);
            return messages;
        }else if(newStage.equals("6") && oldStage.equals("6") && state == ACCEPTED){
            state = ACCEPTED_DIAVGEIA;
            /*email to requester*/
            messages.add(prepareMessageForRequester(requestFatClass,state));
            messages.addAll(prepareMessages(requestFatClass,getPersonOfInterest(requestFatClass,oldStage), state, previousPersonOfInterest));
            if(!requestFatClass.getType().equals("contract"))
                messages.addAll(prepareMessages(requestFatClass,getPersonOfInterest(requestFatClass,"7"), state, nextPersonOfInterest));
            messages.forEach(logger::info);
            return messages;
        /*13->13*/
        }else if(oldStage.equals("13") && newStage.equals("13") && state == ACCEPTED){
            state = COMPLETED;
            messages.add(prepareMessageForRequester(requestFatClass,state));
            messages.addAll(prepareMessages(requestFatClass,getPersonOfInterest(requestFatClass,newStage), state, previousPersonOfInterest));
            messages.forEach(logger::info);
            return messages;
        /*1<-2*/
        }else if(oldStage.equals("2") && newStage.equals("1")){
            /*email to requester*/
            email = requestFatClass.getUser().getEmail();
            messages.add(prepareMessageForRequester(requestFatClass,state));
            /*2*/
            messages.addAll(prepareMessages(requestFatClass,getPersonOfInterest(requestFatClass,oldStage), state, previousPersonOfInterest));
            messages.forEach(logger::info);
            return messages;
            /*7<-8*/
        }else if(oldStage.equals("8") && newStage.equals("7")){
            /*email to requester*/
            email = requestFatClass.getUser().getEmail();
            messages.add(prepareMessageForRequester(requestFatClass,state));
            /*2*/
            messages.addAll(prepareMessages(requestFatClass,getPersonOfInterest(requestFatClass,newStage), state, nextPersonOfInterest));
            messages.addAll(prepareMessages(requestFatClass,getPersonOfInterest(requestFatClass,oldStage), state, previousPersonOfInterest));
            messages.forEach(logger::info);
            return messages;
        }else{
            switch (transition) {
                case "->":
                    /*for example:  12->13 new stage = 13 , old stage = 12*/
                    messages.addAll(prepareMessages(requestFatClass,getPersonOfInterest(requestFatClass,oldStage), state, previousPersonOfInterest));
                    messages.addAll(prepareMessages(requestFatClass,getPersonOfInterest(requestFatClass,newStage), state, nextPersonOfInterest));
                    break;
                case "<-":
                    /*for example:  3<-4 new stage = 3 , old stage = 4*/
                    messages.addAll(prepareMessages(requestFatClass,getPersonOfInterest(requestFatClass,oldStage), state, nextPersonOfInterest));
                    messages.addAll(prepareMessages(requestFatClass,getPersonOfInterest(requestFatClass,newStage), state, previousPersonOfInterest));
                    break;
                case "rejected":
                    /*email to requester*/
                    messages.add(prepareMessageForRequester(requestFatClass,state));
                    messages.addAll(prepareMessages(requestFatClass,getPersonOfInterest(requestFatClass,newStage), state, previousPersonOfInterest));
                    break;
            }
        }
        messages.forEach(logger::info);
        return messages;
    }

    private List<EmailMessage> sendCancelRequestEmails(RequestFatClass requestFatClass,String stage, RequestState state,UserType userType) {
        List<EmailMessage> emails = new ArrayList<>();
        List<PersonOfInterest> pois;
        String[] stages = new String[]{"2","3","4","5a","5b","6"};

        if(requestFatClass.getStage1()!=null)
            emails.add(prepareMessageForRequester(requestFatClass,state));

        for(String s:stages){
            if(s.compareTo(stage) < 0 ){
                pois = getPersonOfInterest(requestFatClass,s);
                emails.addAll(prepareMessages(requestFatClass,pois,state,userType));
            }
        }
        return emails;
    }

    private EmailMessage prepareMessageForRequester(RequestFatClass requestFatClass, RequestState state) {

        List<EmailMessage> messages = new ArrayList<>();
        String email;

        if(requestFatClass.getType().equals("trip")){
            Request request = requestService.get(requestFatClass.getRequest_id());
            if(!request.getUser().getEmail().equals(request.getTrip().getEmail())) {
                /*traveller is not requester*/
                email = request.getTrip().getEmail();
                return createEmail(email, requestFatClass.getRequest_id(), TRAVEL, USER, state, requestFatClass);
            }

        }else{
            /*email to requester or taveller = requester*/
            email = requestFatClass.getUser().getEmail();
            return createEmail(email, requestFatClass.getRequest_id(),OTHER, USER, state,requestFatClass);
        }
        return null;
    }

    private List<PersonOfInterest> getPersonOfInterest(RequestFatClass request,String stage) {

        List<PersonOfInterest> personsOfInterest = new ArrayList();
        switch (stage) {
            case "1":
                personsOfInterest = null;
                break;
            case "2":
                personsOfInterest.add(request.getProject().getScientificCoordinator());
                break;
            case "3":
                personsOfInterest.addAll(request.getProject().getOperator());
                break;
            case "4":
                personsOfInterest.add(request.getProject().getInstitute().getOrganization().getPoy());
                break;
            case "5a":
                personsOfInterest.add(request.getProject().getInstitute().getDiataktis());
                break;
            case "5b":
                personsOfInterest.add(request.getProject().getInstitute().getOrganization().getDioikitikoSumvoulio());
                break;
            case "6":
                personsOfInterest.add(request.getProject().getInstitute().getDiaugeia());
                break;
            case "7":
                if(request.getType().equals("trip"))
                    personsOfInterest.add(request.getProject().getInstitute().getTravelManager());
                else
                    personsOfInterest.add(request.getProject().getInstitute().getSuppliesOffice());
                break;
            case "8":
                personsOfInterest.addAll(request.getProject().getInstitute().getOrganization().getInspectionTeam());
                break;
            case "9":
                personsOfInterest.add(request.getProject().getInstitute().getOrganization().getPoy());
                break;
            case "10":
                personsOfInterest.add(request.getProject().getInstitute().getDiataktis());
                break;
            case "11":
                personsOfInterest.add(request.getProject().getInstitute().getDiaugeia());
                break;
            case "12":
                personsOfInterest.add(request.getProject().getInstitute().getAccountingRegistration());
                break;
            case "13":
                personsOfInterest.add(request.getProject().getInstitute().getAccountingPayment());
                break;
            default:
                return null;
        }
        return personsOfInterest;
    }

    private List<EmailMessage> prepareMessages(RequestFatClass requestFatClass, List<PersonOfInterest> personOfInterests,
                                               RequestState state, UserType userType) {

        List<Delegate> delegates;
        List<EmailMessage> emails = new ArrayList<>();

        if(personOfInterests==null)
            return null;

        for (PersonOfInterest personOfInterest : personOfInterests) {
            emails.add(createEmail(personOfInterest.getEmail(), requestFatClass.getRequest_id(), OTHER, userType, state,requestFatClass));
            delegates = personOfInterest.getDelegates();
            for (Delegate delegate : delegates)
                emails.add(createEmail(delegate.getEmail(), requestFatClass.getRequest_id(), OTHER, userType, state,requestFatClass));
        }
        return emails;
    }

    public String getRequestInfo(RequestFatClass request) {
        final String euro = "\u20ac";
        StringBuilder requestInfo = new StringBuilder();
        String date = null;

        if (request.getStage1().getRequestDate() != null) {
            date = new SimpleDateFormat("dd/MM/yyyy").format(new Date(Long.parseLong(request.getStage1().getRequestDate())));
        }


        requestInfo
                .append("\n\nΑριθμός πρωτοκόλου: ")
                .append(request.getRequest_id())
                .append("\nΈργο: ")
                .append(request.getProject().getAcronym())
                .append("\nΗμερομηνία: ")
                .append(date)
                .append("\nΠοσό: " + euro)
                .append(request.getStage1().getAmountInEuros())
                .append("\nΘέμα: ")
                .append(request.getStage1().getSubject())
                .append("\n\nΜπορείτε να παρακολουθήσετε την εξέλιξή του ακολουθώντας τον παρακάτω σύνδεσμο: \n");

        if(request.getId().contains("a"))
            requestInfo.append(approval_url).append(request.getId());
        else
            requestInfo.append(payment_url).append(request.getId());


        return requestInfo.toString();
    }
}
