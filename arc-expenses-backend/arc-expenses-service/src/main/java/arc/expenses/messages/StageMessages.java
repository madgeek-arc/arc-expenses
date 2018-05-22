package arc.expenses.messages;

import gr.athenarc.domain.Request;
import arc.expenses.mail.EmailMessage;

import java.util.ArrayList;
import java.util.List;

public class StageMessages {
    public enum UserType {USER, POI, nextPOI}
    public enum RequestState {INITIALIZED, ACCEPTED, REJECTED, FINISHED}

    public List<EmailMessage> createMessages(String prevStage, String nextStage, Request request) {
        String firstname;
        String lastname;

        EmailMessage userEmail = new EmailMessage();
        EmailMessage POIEmail = new EmailMessage();
        EmailMessage nextPOIEmail = new EmailMessage();

        List<EmailMessage> emails = new ArrayList<>();

        String subject = "[ARC-REQUEST] Αίτηση " + request.getId();

        POIEmail.setSubject(subject);
        nextPOIEmail.setSubject(subject);


        // Stage 1 -> 2
        if (prevStage == null && nextStage.equals("2")) {
            // email to user
            emails.add(createMessage(request.getRequester().getEmail(), subject, messageTemplates(
                    null, null, request.getId(), UserType.USER, RequestState.INITIALIZED,
                    request.getStage1().getRequestDate())));

            // email to next POI for review
            emails.add(createMessage(request.getProject().getScientificCoordinator().getEmail(), subject,
                    messageTemplates(null, null, request.getId(), UserType.nextPOI,
                            RequestState.INITIALIZED, request.getStage1().getRequestDate())));
            // email to all next POI delegates
            request.getProject().getScientificCoordinator().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            messageTemplates(null, null, request.getId(), UserType.nextPOI,
                                    RequestState.INITIALIZED, request.getStage1().getRequestDate()))));
        }
        // Stage 2 -> 3 // TODO ok
        else if (prevStage.equals("2") && nextStage.equals("3")) {
            firstname = request.getStage2().getUser().getFirstname();
            lastname = request.getStage2().getUser().getLastname();

            // email report to POI
            emails.add(createMessage(request.getProject().getScientificCoordinator().getEmail(), subject,
                    messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                            RequestState.ACCEPTED, null)));

            // email report to all POI delegates
            request.getProject().getScientificCoordinator().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                                    RequestState.ACCEPTED, null))));

            // emails to all next POIs and their delegates - getOperator() returns a list of POIs
            request.getProject().getOperator()
                    .forEach(operator -> operator.getDelegates()
                            .forEach(delegate -> emails.add(createMessage(
                                    delegate.getEmail(), subject,
                                    messageTemplates(null, null,
                                            request.getId(), UserType.nextPOI, RequestState.ACCEPTED, null)))));
        }
        // Stage 3 -> 4 // TODO ok
        else if (prevStage.equals("3") && nextStage.equals("4")) {
            firstname = request.getStage3().getUser().getFirstname();
            lastname = request.getStage3().getUser().getLastname();

            // email report to all POIs and their delegates - getOperator() returns a list of POIs
            request.getProject().getOperator()
                    .forEach(operator -> operator.getDelegates()
                            .forEach(delegate -> emails.add(createMessage(
                                    delegate.getEmail(), subject,
                                    messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                                            RequestState.ACCEPTED, null)))));

            // emails to next POI and delegates
            emails.add(createMessage(request.getProject().getInstitute().getOrganization().getPOI().getEmail(),
                    subject, messageTemplates(null, null, request.getId(), UserType.nextPOI,
                            RequestState.ACCEPTED, null)));
            request.getProject().getInstitute().getOrganization().getPOI().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            messageTemplates(null, null, request.getId(), UserType.nextPOI,
                                    RequestState.ACCEPTED, null))));
        }
        // Stage 4 -> 5 // TODO ok
        else if (prevStage.equals("4") && nextStage.equals("5")) {
            firstname = request.getStage4().getUser().getFirstname();
            lastname = request.getStage4().getUser().getLastname();

            // email report to POI
            emails.add(createMessage(request.getProject().getInstitute().getOrganization().getPOI().getEmail(), subject,
                    messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                            RequestState.ACCEPTED, null)));

            // email report to all POI delegates
            request.getProject().getInstitute().getOrganization().getPOI().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                                    RequestState.ACCEPTED, null))));

            // email to next POI
            emails.add(createMessage(request.getProject().getInstitute().getDirector().getEmail(), subject,
                    messageTemplates(null, null, request.getId(), UserType.nextPOI,
                            RequestState.ACCEPTED, null)));

            // email to next POI delegates
            request.getProject().getInstitute().getDirector().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            messageTemplates(null, null, request.getId(), UserType.nextPOI,
                                    RequestState.ACCEPTED, null))));
        }
        // Stage 5 -> 5a // TODO check again
        else if (prevStage.equals("5") && nextStage.equals("5a")) {
            firstname = request.getStage5().getUser().getFirstname();
            lastname = request.getStage5().getUser().getLastname();

            // email report to POI
            emails.add(createMessage(request.getProject().getInstitute().getDirector().getEmail(), subject,
                    messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                            RequestState.ACCEPTED, null)));

            // email report to all POI delegates
            request.getProject().getInstitute().getDirector().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                                    RequestState.ACCEPTED, null))));

            // email to next POI
            emails.add(createMessage(request.getProject().getInstitute().getOrganization().getDirector().getEmail(),
                    subject, messageTemplates(null, null, request.getId(), UserType.nextPOI,
                            RequestState.ACCEPTED, null)));

            // email to next POI delegates
            request.getProject().getInstitute().getOrganization().getDirector().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            messageTemplates(null, null, request.getId(), UserType.nextPOI,
                                    RequestState.ACCEPTED, null))));
        }
        // Stage 5 -> 5b // TODO check again
        else if (prevStage.equals("5") && nextStage.equals("5b")) {
            firstname = request.getStage5().getUser().getFirstname();
            lastname = request.getStage5().getUser().getLastname();

            // email report to POI
            emails.add(createMessage(request.getProject().getInstitute().getDirector().getEmail(), subject,
                    messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                            RequestState.ACCEPTED, null)));

            // email report to all POI delegates
            request.getProject().getInstitute().getDirector().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                                    RequestState.ACCEPTED, null))));

            // email to next POI
            emails.add(createMessage(
                    request.getProject().getInstitute().getOrganization().getDioikitikoSumvoulio().getEmail(),
                    subject, messageTemplates(null, null, request.getId(), UserType.nextPOI,
                            RequestState.ACCEPTED, null)));

            // email to next POI delegates
            request.getProject().getInstitute().getOrganization().getDioikitikoSumvoulio().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            messageTemplates(null, null, request.getId(), UserType.nextPOI,
                                    RequestState.ACCEPTED, null))));
        }
        // Stage 5/5a/5b -> 6
        else if (nextStage.equals("6")) {
            // Stage 5 -> 6 TODO check again
            if (prevStage.equals("5")) {
                firstname = request.getStage5().getUser().getFirstname();
                lastname = request.getStage5().getUser().getLastname();

                // email report to POI
                emails.add(createMessage(request.getProject().getInstitute().getDirector().getEmail(), subject,
                        messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                                RequestState.ACCEPTED, null)));

                // email report to all POI delegates
                request.getProject().getInstitute().getDirector().getDelegates()
                        .forEach(delegate -> emails.add(createMessage(
                                delegate.getEmail(), subject,
                                messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                                        RequestState.ACCEPTED, null))));
            }
            // Stage 5a -> 6 TODO check again
            else if (prevStage.equals("5a")) {
                firstname = request.getStage5a().getUser().getFirstname();
                lastname = request.getStage5a().getUser().getLastname();

                // email report to POI
                emails.add(createMessage(
                        request.getProject().getInstitute().getOrganization().getDirector().getEmail(), subject,
                        messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                                RequestState.ACCEPTED, null)));

                // email report to all POI delegates
                request.getProject().getInstitute().getOrganization().getDirector().getDelegates()
                        .forEach(delegate -> emails.add(createMessage(
                                delegate.getEmail(), subject,
                                messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                                        RequestState.ACCEPTED, null))));
            }
            // Stage 5b -> 6 TODO check again
            else if (prevStage.equals("5b")) {
                firstname = request.getStage5b().getUser().getFirstname();
                lastname = request.getStage5b().getUser().getLastname();

                // email report to POI
                emails.add(createMessage(
                        request.getProject().getInstitute().getOrganization().getDioikitikoSumvoulio().getEmail(),
                        subject, messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                                RequestState.ACCEPTED, null)));

                // email report to all POI delegates
                request.getProject().getInstitute().getOrganization().getDioikitikoSumvoulio().getDelegates()
                        .forEach(delegate -> emails.add(createMessage(
                                delegate.getEmail(), subject,
                                messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                                        RequestState.ACCEPTED, null))));
            }
            // email to next POI
            emails.add(createMessage(
                    request.getProject().getInstitute().getDiaugeia().getEmail(),
                    subject, messageTemplates(null, null, request.getId(), UserType.nextPOI,
                            RequestState.ACCEPTED, null)));

            // email to next POI delegates
            request.getProject().getInstitute().getDiaugeia().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            messageTemplates(null, null, request.getId(), UserType.nextPOI,
                                    RequestState.ACCEPTED, null))));
        }
//        // Stage 6 -> 7 // TODO ok
//        else if (prevStage.equals("6") && nextStage.equals("7")) {
//            firstname = request.getStage6().getOrganizationDiaugeia().getFirstname();
//            lastname = request.getStage6().getOrganizationDiaugeia().getLastname();
//
////            userEmail.setText("Το αίτημά σας ελέγχθηκε από τον υπεύθυνο: " + firstname + " " + lastname);
//            userEmail = null;
//
//            POIEmail.setText(messageTemplates(firstname, lastname, request.getId(), UserType.POI));
//            POIEmail.setRecipient(request.getProject().getInstitute().getDiaugeia().getEmail());
//            request.getProject().getInstitute().getDiaugeia().getDelegates()
//                    .forEach(delegate -> emails.add(createMessage(delegate.getEmail(), subject,
//                            messageTemplates(firstname, lastname, request.getId(), UserType.POI))));
//
//            // there is no nextPOIEmail because getOperator() returns a list of POIs
//            nextPOIEmail = null;
//            request.getProject().getOperator()
//                    .forEach(operator -> operator.getDelegates()
//                            .forEach(delegate -> emails.add(createMessage(
//                                    delegate.getEmail(), subject,
//                                    messageTemplates(null, null,
//                                            request.getId(), UserType.nextPOI)))));
//
//        }
//        // Stage 7 -> 8 // TODO ok
//        else if (prevStage.equals("7") && nextStage.equals("8")) {
//            firstname = request.getStage7().getOperator().getFirstname();
//            lastname = request.getStage7().getOperator().getLastname();
//
////            userEmail.setText("Το αίτημά σας ελέγχθηκε από τον υπεύθυνο: " + firstname + " " + lastname);
//            userEmail = null;
//
//            // there is no POIEmail because getOperator() returns a list of POIs
//            POIEmail = null;
//            request.getProject().getOperator()
//                    .forEach(operator -> operator.getDelegates()
//                            .forEach(delegate -> emails.add(createMessage(
//                                    delegate.getEmail(), subject,
//                                    messageTemplates(firstname, lastname, request.getId(), UserType.POI)))));
//
//            nextPOIEmail.setText(messageTemplates(null, null, request.getId(), UserType.nextPOI));
//            nextPOIEmail.setRecipient(request.getProject().getInstitute().getAccountingDirector().getEmail());
//            request.getProject().getInstitute().getAccountingDirector().getDelegates()
//                    .forEach(delegate -> emails.add(createMessage(
//                            delegate.getEmail(), subject, messageTemplates(null, null,
//                            request.getId(), UserType.nextPOI))));
//        }
//        // Stage 8 -> 9 // TODO ok
//        else if (prevStage.equals("8") && nextStage.equals("9")) {
//            firstname = request.getStage8().getAccountingDirector().getFirstname();
//            lastname = request.getStage8().getAccountingDirector().getLastname();
//
////            userEmail.setText("Το αίτημά σας ελέγχθηκε από τον υπεύθυνο: " + firstname + " " + lastname);
//            userEmail = null;
//
//            POIEmail.setText(messageTemplates(firstname, lastname, request.getId(), UserType.POI));
//            POIEmail.setRecipient(request.getProject().getInstitute().getAccountingDirector().getEmail());
//            request.getProject().getInstitute().getAccountingDirector().getDelegates()
//                    .forEach(delegate -> emails.add(createMessage(delegate.getEmail(), subject,
//                            messageTemplates(firstname, lastname, request.getId(), UserType.POI))));
//
//            nextPOIEmail.setText(messageTemplates(null, null, request.getId(), UserType.nextPOI));
//            nextPOIEmail.setRecipient(request.getProject().getInstitute().getOrganization().getPOI().getEmail());
//            request.getProject().getInstitute().getOrganization().getPOI().getDelegates()
//                    .forEach(delegate -> emails.add(createMessage(
//                            delegate.getEmail(), subject, messageTemplates(null, null,
//                                    request.getId(), UserType.nextPOI))));
//        }
//        // Stage 9 -> 10 // TODO ok
//        else if (prevStage.equals("9") && nextStage.equals("10")) {
//            firstname = request.getStage9().getPOI().getFirstname();
//            lastname = request.getStage9().getPOI().getLastname();
//
////            userEmail.setText("Το αίτημά σας ελέγχθηκε από τον υπεύθυνο: " + firstname + " " + lastname);
//            userEmail = null;
//
//            POIEmail.setText(messageTemplates(firstname, lastname, request.getId(), UserType.POI));
//            POIEmail.setRecipient(request.getProject().getInstitute().getOrganization().getPOI().getEmail());
//            request.getProject().getInstitute().getOrganization().getPOI().getDelegates()
//                    .forEach(delegate -> emails.add(createMessage(delegate.getEmail(), subject,
//                            messageTemplates(firstname, lastname, request.getId(), UserType.POI))));
//
//            nextPOIEmail.setText(messageTemplates(null, null, request.getId(), UserType.nextPOI));
//            nextPOIEmail.setRecipient(request.getProject().getInstitute().getAccountingRegistration().getEmail());
//            request.getProject().getInstitute().getAccountingRegistration().getDelegates()
//                    .forEach(delegate -> emails.add(createMessage(
//                            delegate.getEmail(), subject, messageTemplates(null, null,
//                                    request.getId(), UserType.nextPOI))));
//        }
//        // Stage 10 -> 11 // TODO ok
//        else if (prevStage.equals("10") && nextStage.equals("11")) {
//            firstname = request.getStage10().getAccountingRegistration().getFirstname();
//            lastname = request.getStage10().getAccountingRegistration().getLastname();
//
////            userEmail.setText("Το αίτημά σας ελέγχθηκε από τον υπεύθυνο: " + firstname + " " + lastname);
//            userEmail = null;
//
//            POIEmail.setText(messageTemplates(firstname, lastname, request.getId(), UserType.POI));
//            POIEmail.setRecipient(request.getProject().getInstitute().getAccountingRegistration().getEmail());
//            request.getProject().getInstitute().getAccountingRegistration().getDelegates()
//                    .forEach(delegate -> emails.add(createMessage(delegate.getEmail(), subject,
//                            messageTemplates(firstname, lastname, request.getId(), UserType.POI))));
//
//            nextPOIEmail.setText(messageTemplates(null, null, request.getId(), UserType.nextPOI));
//            nextPOIEmail.setRecipient(request.getProject().getInstitute().getAccountingPayment().getEmail());
//            request.getProject().getInstitute().getAccountingPayment().getDelegates()
//                    .forEach(delegate -> emails.add(createMessage(
//                            delegate.getEmail(), subject, messageTemplates(null, null,
//                                    request.getId(), UserType.nextPOI))));
//        }
//        // Stage 11 -> 12 // TODO ok
//        else if (prevStage.equals("11") && nextStage.equals("12") && request.getStatus().equals("accepted")) {
//            firstname = request.getStage11().getAccountingPayment().getFirstname();
//            lastname = request.getStage11().getAccountingPayment().getLastname();
//
//            userEmail.setText("Το αίτημά σας ελέγθηκε και εγκρίθηκε επιτυχώς!");
//            POIEmail.setText(messageTemplates(firstname, lastname, request.getId(), UserType.POI));
//            POIEmail.setRecipient(request.getProject().getInstitute().getAccountingPayment().getEmail());
//            request.getProject().getInstitute().getAccountingPayment().getDelegates()
//                    .forEach(delegate -> emails.add(createMessage(delegate.getEmail(), subject,
//                            messageTemplates(firstname, lastname, request.getId(), UserType.POI))));
//        }
//        // Stage 12 // TODO ti ginetai me ta stages 11/12? prepei na ftiaksoume to domain
//        else if (prevStage.equals("12") && nextStage.equals("12") && request.getStatus().equals("accepted")) {
//            userEmail.setText("Το αίτημά σας ελέγθηκε και εγκρίθηκε επιτυχώς!");
//            POIEmail.setText("Εγκρίνατε το αίτημα με κωδικό " + request.getId());
//            POIEmail.setRecipient(request.getProject());
//        }

        if (userEmail != null) {
            emails.add(userEmail);
        }
        if (POIEmail != null) {
            emails.add(POIEmail);
        }
        if (nextPOIEmail != null) {
            emails.add(nextPOIEmail);
        }
        emails.add(userEmail);
        return emails;
    }

    private EmailMessage createMessage(String to, String subject, String text) {
        EmailMessage mail = new EmailMessage();
        mail.setRecipient(to);
        mail.setSubject(subject);
        mail.setText(text);
        return mail;
    }


    private String messageTemplates(String firstname, String lastname, String id, UserType type,
                                    RequestState state, String date) {
        String messageText = null;
        if (type == UserType.USER) {
            if (state == RequestState.INITIALIZED) {
                messageText = "Το αίτημά σας, με κωδικό " + id + ", υποβλήθηκε επιτυχώς στις " + date;
            } else if (state == RequestState.ACCEPTED) {
                messageText = "Το αίτημά σας ελέγχθηκε από τον υπεύθυνο: " + firstname + " " + lastname;
            } else if (state == RequestState.FINISHED) {
                messageText = "Το αίτημά σας με κωδικό " + id + " ελέγχθηκε κι εγκρίθηκε επιτυχώς!";
            } else if (state == RequestState.REJECTED) {
                messageText = "Το αίτημά σας με κωδικό " + id + " απορρίφθηκε...";
            }
        } else if (type == UserType.POI) {
            if (state == RequestState.INITIALIZED) {
                messageText = "Νέο αίτημα με κωδικό " + id + " από τον/την " + firstname + " " + lastname;
            } else if (state == RequestState.ACCEPTED) {
                messageText = "Εγκρίθηκε το αίτημα με κωδικό " + id + " από τον/την " + firstname + " " + lastname;
            } else if (state == RequestState.FINISHED) {
                messageText = "Ολοκληρώθηκε το αίτημα με κωδικό " + id + " από τον/την " + firstname + " " + lastname;
            } else if (state == RequestState.REJECTED) {
                messageText = "Απορρίφθηκε το αίτημα με κωδικό " + id + " από τον/την " + firstname + " " + lastname;
            }
        } else if (type == UserType.nextPOI) {
            if (state == RequestState.INITIALIZED) {
                messageText = "Nέα αίτηση υποβλήθηκε στο σύστημα. Κωδικός αίτησης: " + id;
            } else if (state == RequestState.ACCEPTED) {
                messageText = "Νέο αίτημα προς έλεγχο με κωδικό " + id;
            } else if (state == RequestState.FINISHED) {
                messageText = "";
            } else if (state == RequestState.REJECTED) {
                messageText = "";
            }
        }
        return messageText;
    }
}
