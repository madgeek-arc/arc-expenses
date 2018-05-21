package messages;

import gr.athenarc.domain.Request;
import mail.EmailMessage;

import java.util.ArrayList;
import java.util.List;

public class StageMessages {

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

        userEmail.setSubject(subject);
        userEmail.setRecipient(request.getRequester().getEmail());

        // Stage 1 -> 2 // TODO ok
        if (prevStage == null && nextStage.equals("2")) {
            userEmail.setText("Το αίτημά σας, με κωδικό " + request.getId() + ", υποβλήθηκε επιτυχώς στις "
                    + request.getStage1().getRequestDate());

            nextPOIEmail.setText("Nέα αίτηση υποβλήθηκε στο σύστημα. Κωδικός αίτησης: " + request.getId());
            nextPOIEmail.setRecipient(request.getProject().getScientificCoordinator().getEmail());
            request.getProject().getScientificCoordinator().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            "Νέο αίτημα προς έλεγχο με κωδικό " + request.getId())));
        }
        // Stage 2 -> 3 // TODO ok
        else if (prevStage.equals("2") && nextStage.equals("3")) {
            firstname = request.getStage2().getScientificCoordinator().getFirstname();
            lastname = request.getStage2().getScientificCoordinator().getLastname();
            userEmail.setText("Το αίτημά σας ελέγχθηκε από τον υπεύθυνο: " + firstname + " " + lastname);

            POIEmail.setText("Εγκρίθηκε το αίτημα με κωδικό " + request.getId() + " από τον/την " +
                    firstname + " " + lastname);
            POIEmail.setRecipient(request.getProject().getScientificCoordinator().getEmail());
            request.getProject().getScientificCoordinator().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            "Εγκρίθηκε το αίτημα με κωδικό " + request.getId() + " από τον/την " +
                            firstname + " " + lastname)));
            
            // there is no nextPOIEmail because getOperator() returns a list of POIs
            nextPOIEmail = null;
            request.getProject().getOperator()
                    .forEach(operator -> operator.getDelegates()
                            .forEach(delegate -> emails.add(createMessage(
                                    delegate.getEmail(), subject,
                                    "Νέο αίτημα προς έλεγχο με κωδικό " + request.getId()))));
        }
        // Stage 3 -> 4 // TODO ok
        else if (prevStage.equals("3") && nextStage.equals("4")) {
            firstname = request.getStage3().getOperator().getFirstname();
            lastname = request.getStage3().getOperator().getLastname();

            userEmail.setText("Το αίτημά σας ελέγχθηκε από τον υπεύθυνο: " + firstname + " " + lastname);

            // there is no POIEmail because getOperator() returns a list of POIs
            POIEmail = null;
            request.getProject().getOperator()
                    .forEach(operator -> operator.getDelegates()
                            .forEach(delegate -> emails.add(createMessage(
                                    delegate.getEmail(), subject,
                                    "Εγκρίθηκε το αίτημα με κωδικό " + request.getId() + " από τον/την " +
                                            firstname + " " + lastname))));

            nextPOIEmail.setText("Νέο αίτημα προς έλεγχο με κωδικό " + request.getId());
            nextPOIEmail.setRecipient(request.getProject().getInstitute().getOrganization().getPOY().getEmail());
            request.getProject().getInstitute().getOrganization().getPOY().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            "Νέο αίτημα προς έλεγχο με κωδικό " + request.getId())));
        }
        // Stage 4 -> 5 // TODO ok
        else if (prevStage.equals("4") && nextStage.equals("5")) {
            firstname = request.getStage4().getPOY().getFirstname();
            lastname = request.getStage4().getPOY().getLastname();

            userEmail.setText("Το αίτημά σας ελέγχθηκε από τον υπεύθυνο: " + firstname + " " + lastname);

            POIEmail.setText("Εγκρίθηκε το αίτημα με κωδικό " + request.getId() + " από τον/την " +
                    firstname + " " + lastname);
            POIEmail.setRecipient(request.getStage4().getPOY().getEmail()); // FIXME
            request.getProject().getInstitute().getOrganization().getPOY().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(delegate.getEmail(), subject,
                            "Εγκρίθηκε το αίτημα με κωδικό " + request.getId() + " από τον/την " +
                                    firstname + " " + lastname)));

            nextPOIEmail.setText("Νέο αίτημα προς έλεγχο με κωδικό " + request.getId());
            nextPOIEmail.setRecipient(request.getProject().getInstitute().getDirector().getEmail());
            request.getProject().getInstitute().getDirector().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(delegate.getEmail(), subject,
                            "Νέο αίτημα προς έλεγχο με κωδικό " + request.getId())));
        }
        // Stage 5 -> 5a // TODO check again
        else if (prevStage.equals("5") && nextStage.equals("5a")) {
            firstname = request.getStage5().getInstituteDirector().getFirstname();
            lastname = request.getStage5().getInstituteDirector().getLastname();

            userEmail.setText("Το αίτημά σας ελέγχθηκε από τον υπεύθυνο: " + firstname + " " + lastname);

            POIEmail.setText("Εγκρίθηκε το αίτημα με κωδικό " + request.getId() + " από τον/την " +
                    firstname + " " + lastname);
            POIEmail.setRecipient(request.getStage5().getInstituteDirector().getEmail()); // FIXME
            request.getProject().getInstitute().getDirector().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(delegate.getEmail(), subject,
                            "Εγκρίθηκε το αίτημα με κωδικό " + request.getId() + " από τον/την " +
                                    firstname + " " + lastname)));

            nextPOIEmail.setText("Νέο αίτημα προς έλεγχο με κωδικό " + request.getId());
            nextPOIEmail.setRecipient(
                    request.getProject().getInstitute().getOrganization().getDirector().getEmail());

            request.getProject().getInstitute().getOrganization().getDirector().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(delegate.getEmail(), subject,
                            "Νέο αίτημα προς έλεγχο με κωδικό " + request.getId())));
        }
        // Stage 5 -> 5b // TODO check again
        else if (prevStage.equals("5") && nextStage.equals("5b")) {
            firstname = request.getStage5().getInstituteDirector().getFirstname();
            lastname = request.getStage5().getInstituteDirector().getLastname();

            userEmail.setText("Το αίτημά σας ελέγχθηκε από τον υπεύθυνο: " + firstname + " " + lastname);

            POIEmail.setText("Εγκρίθηκε το αίτημα με κωδικό " + request.getId() + " από τον/την " +
                    firstname + " " + lastname);
            POIEmail.setRecipient(request.getStage5().getInstituteDirector().getEmail()); // FIXME
            request.getProject().getInstitute().getDirector().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(delegate.getEmail(), subject,
                            "Εγκρίθηκε το αίτημα με κωδικό " + request.getId() + " από τον/την " +
                                    firstname + " " + lastname)));

            nextPOIEmail.setText("Νέο αίτημα προς έλεγχο με κωδικό " + request.getId());
            nextPOIEmail.setRecipient(
                    request.getProject().getInstitute().getOrganization().getDioikitikoSumvoulio().getEmail());
            request.getProject().getInstitute().getOrganization().getDioikitikoSumvoulio().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(delegate.getEmail(), subject,
                            "Νέο αίτημα προς έλεγχο με κωδικό " + request.getId())));
        }
        // Stage 5/5a/5b -> 6
        else if (nextStage.equals("6")) {
            // Stage 5 -> 6 TODO check again
            if (prevStage.equals("5")) {
                firstname = request.getStage5().getInstituteDirector().getFirstname();
                lastname = request.getStage5().getInstituteDirector().getLastname();

                userEmail.setText("Το αίτημά σας ελέγχθηκε από τον υπεύθυνο: " + firstname + " " + lastname);

                POIEmail.setText("Εγκρίθηκε το αίτημα με κωδικό " + request.getId() + " από τον/την " +
                        firstname + " " + lastname);
                POIEmail.setRecipient(request.getStage5().getInstituteDirector().getEmail()); // FIXME
                request.getProject().getInstitute().getDirector().getDelegates()
                        .forEach(delegate -> emails.add(createMessage(delegate.getEmail(), subject,
                                "Εγκρίθηκε το αίτημα με κωδικό " + request.getId() + " από τον/την " +
                                        firstname + " " + lastname)));
            }
            // Stage 5a -> 6 TODO check again
            else if (prevStage.equals("5a")) {
                firstname = request.getStage5a().getOrganizationDirector().getFirstname();
                lastname = request.getStage5a().getOrganizationDirector().getLastname();

                userEmail.setText("Το αίτημά σας ελέγχθηκε από τον υπεύθυνο: " + firstname + " " + lastname);

                POIEmail.setText("Εγκρίθηκε το αίτημα με κωδικό " + request.getId() + " από τον/την " +
                        firstname + " " + lastname);
                POIEmail.setRecipient(request.getStage5a().getOrganizationDirector().getEmail()); // FIXME
                request.getProject().getInstitute().getOrganization().getDirector().getDelegates()
                        .forEach(delegate -> emails.add(createMessage(delegate.getEmail(), subject,
                                "Εγκρίθηκε το αίτημα με κωδικό " + request.getId() + " από τον/την " +
                                        firstname + " " + lastname)));
            }
            // Stage 5b -> 6 TODO check again
            else if (prevStage.equals("5b")) {
                firstname = request.getStage5b().getDioikitikoSumvoulio().getFirstname();
                lastname = request.getStage5b().getDioikitikoSumvoulio().getLastname();

                userEmail.setText("Το αίτημά σας ελέγχθηκε από τον υπεύθυνο: " + firstname + " " + lastname);

                POIEmail.setText("Εγκρίθηκε το αίτημα με κωδικό " + request.getId() + " από τον/την " +
                        firstname + " " + lastname);
                POIEmail.setRecipient(request.getStage5b().getDioikitikoSumvoulio().getEmail()); // FIXME
                request.getProject().getInstitute().getOrganization().getDioikitikoSumvoulio().getDelegates()
                        .forEach(delegate -> emails.add(createMessage(delegate.getEmail(), subject,
                                "Εγκρίθηκε το αίτημα με κωδικό " + request.getId() + " από τον/την " +
                                        firstname + " " + lastname)));
            }
            nextPOIEmail.setText("Νέο αίτημα προς έλεγχο με κωδικό " + request.getId());
            nextPOIEmail.setRecipient(request.getProject().getInstitute().getDiaugeia().getEmail());
            request.getProject().getInstitute().getDiaugeia().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(delegate.getEmail(), subject,
                            "Νέο αίτημα προς έλεγχο με κωδικό " + request.getId())));
        }
        // Stage 6 -> 7 // TODO ok
        else if (prevStage.equals("6") && nextStage.equals("7")) {
            firstname = request.getStage6().getOrganizationDiaugeia().getFirstname();
            lastname = request.getStage6().getOrganizationDiaugeia().getLastname();

            userEmail.setText("Το αίτημά σας ελέγχθηκε από τον υπεύθυνο: " + firstname + " " + lastname);

            POIEmail.setText("Εγκρίθηκε το αίτημα με κωδικό " + request.getId() + " από τον/την " +
                    firstname + " " + lastname);
            POIEmail.setRecipient(request.getProject().getInstitute().getDiaugeia().getEmail());
            request.getProject().getInstitute().getDiaugeia().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(delegate.getEmail(), subject,
                            "Εγκρίθηκε το αίτημα με κωδικό " + request.getId() + " από τον/την " +
                                    firstname + " " + lastname)));

            // there is no nextPOIEmail because getOperator() returns a list of POIs
            nextPOIEmail = null;
            request.getProject().getOperator()
                    .forEach(operator -> operator.getDelegates()
                            .forEach(delegate -> emails.add(createMessage(
                                    delegate.getEmail(), subject,
                                    "Νέο αίτημα προς έλεγχο με κωδικό " + request.getId()))));

        }
        // Stage 7 -> 8 // TODO ok
        else if (prevStage.equals("7") && nextStage.equals("8")) {
            firstname = request.getStage7().getOperator().getFirstname();
            lastname = request.getStage7().getOperator().getLastname();

            userEmail.setText("Το αίτημά σας ελέγχθηκε από τον υπεύθυνο: " + firstname + " " + lastname);

            // there is no POIEmail because getOperator() returns a list of POIs
            POIEmail = null;
            request.getProject().getOperator()
                    .forEach(operator -> operator.getDelegates()
                            .forEach(delegate -> emails.add(createMessage(
                                    delegate.getEmail(), subject,
                                    "Εγκρίθηκε το αίτημα με κωδικό " + request.getId() + " από τον/την " +
                                            firstname + " " + lastname))));

            nextPOIEmail.setText("Νέο αίτημα προς έλεγχο με κωδικό " + request.getId());
            nextPOIEmail.setRecipient(request.getProject().getInstitute().getAccountingDirector().getEmail());
            request.getProject().getInstitute().getAccountingDirector().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(delegate.getEmail(), subject,
                            "Νέο αίτημα προς έλεγχο με κωδικό " + request.getId())));
        }
        // Stage 8 -> 9 // TODO ok
        else if (prevStage.equals("8") && nextStage.equals("9")) {
            firstname = request.getStage8().getAccountingDirector().getFirstname();
            lastname = request.getStage8().getAccountingDirector().getLastname();

            userEmail.setText("Το αίτημά σας ελέγχθηκε από τον υπεύθυνο: " + firstname + " " + lastname);

            POIEmail.setText("Εγκρίθηκε το αίτημα με κωδικό " + request.getId() + " από τον/την " +
                    firstname + " " + lastname);
            POIEmail.setRecipient(request.getProject().getInstitute().getAccountingDirector().getEmail());
            request.getProject().getInstitute().getAccountingDirector().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(delegate.getEmail(), subject,
                            "Εγκρίθηκε το αίτημα με κωδικό " + request.getId() + " από τον/την " +
                                    firstname + " " + lastname)));

            nextPOIEmail.setText("Νέο αίτημα προς έλεγχο με κωδικό " + request.getId());
            nextPOIEmail.setRecipient(request.getProject().getInstitute().getOrganization().getPOY().getEmail());
            request.getProject().getInstitute().getOrganization().getPOY().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            "Νέο αίτημα προς έλεγχο με κωδικό " + request.getId())));
        }
        // Stage 9 -> 10 // TODO ok
        else if (prevStage.equals("9") && nextStage.equals("10")) {
            firstname = request.getStage9().getPOY().getFirstname();
            lastname = request.getStage9().getPOY().getLastname();

            userEmail.setText("Το αίτημά σας ελέγχθηκε από τον υπεύθυνο: " + firstname + " " + lastname);

            POIEmail.setText("Εγκρίθηκε το αίτημα με κωδικό " + request.getId() + " από τον/την " +
                    firstname + " " + lastname);
            POIEmail.setRecipient(request.getProject().getInstitute().getOrganization().getPOY().getEmail());
            request.getProject().getInstitute().getOrganization().getPOY().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(delegate.getEmail(), subject,
                            "Εγκρίθηκε το αίτημα με κωδικό " + request.getId() + " από τον/την " +
                                    firstname + " " + lastname)));

            nextPOIEmail.setText("Νέο αίτημα προς έλεγχο με κωδικό " + request.getId());
            nextPOIEmail.setRecipient(request.getProject().getInstitute().getAccountingRegistration().getEmail());
            request.getProject().getInstitute().getAccountingRegistration().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            "Νέο αίτημα προς έλεγχο με κωδικό " + request.getId())));
        }
        // Stage 10 -> 11 // TODO ok
        else if (prevStage.equals("10") && nextStage.equals("11")) {
            firstname = request.getStage10().getAccountingRegistration().getFirstname();
            lastname = request.getStage10().getAccountingRegistration().getLastname();

            userEmail.setText("Το αίτημά σας ελέγχθηκε από τον υπεύθυνο: " + firstname + " " + lastname);

            POIEmail.setText("Εγκρίθηκε το αίτημα με κωδικό " + request.getId() + " από τον/την " +
                    firstname + " " + lastname);
            POIEmail.setRecipient(request.getProject().getInstitute().getAccountingRegistration().getEmail());
            request.getProject().getInstitute().getAccountingRegistration().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(delegate.getEmail(), subject,
                            "Εγκρίθηκε το αίτημα με κωδικό " + request.getId() + " από τον/την " +
                                    firstname + " " + lastname)));

            nextPOIEmail.setText("Νέο αίτημα προς έλεγχο με κωδικό " + request.getId());
            nextPOIEmail.setRecipient(request.getProject().getInstitute().getAccountingPayment().getEmail());
            request.getProject().getInstitute().getAccountingPayment().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            "Νέο αίτημα προς έλεγχο με κωδικό " + request.getId())));
        }
        // Stage 11 -> 12 // TODO ok
        else if (prevStage.equals("11") && nextStage.equals("12") && request.getStatus().equals("accepted")) {
            firstname = request.getStage11().getAccountingPayment().getFirstname();
            lastname = request.getStage11().getAccountingPayment().getLastname();

            userEmail.setText("Το αίτημά σας ελέγθηκε και εγκρίθηκε επιτυχώς!");
            POIEmail.setText("Εγκρίθηκε το αίτημα με κωδικό " + request.getId() + " από τον/την " +
                    firstname + " " + lastname);
            POIEmail.setRecipient(request.getProject().getInstitute().getAccountingPayment().getEmail());
            request.getProject().getInstitute().getAccountingPayment().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(delegate.getEmail(), subject,
                            "Εγκρίθηκε το αίτημα με κωδικό " + request.getId() + " από τον/την " +
                                    firstname + " " + lastname)));
        }
//        // Stage 12 // TODO ti ginetai me ta stages 11/12? prepei na ftiaksoume to domain
//        else if (prevStage.equals("12") && nextStage.equals("12") && request.getStatus().equals("accepted")) {
//            userEmail.setText("Το αίτημά σας ελέγθηκε και εγκρίθηκε επιτυχώς!");
//            POIEmail.setText("Εγκρίνατε το αίτημα με κωδικό " + request.getId());
//            POIEmail.setRecipient(request.getProject());
//        }

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


}
