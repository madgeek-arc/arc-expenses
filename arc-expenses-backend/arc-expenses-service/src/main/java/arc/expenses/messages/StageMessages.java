package arc.expenses.messages;

import arc.expenses.service.GenericService;
import arc.expenses.service.UserServiceImpl;
import eu.openminted.registry.core.domain.FacetFilter;
import gr.athenarc.domain.Request;
import arc.expenses.mail.EmailMessage;
import gr.athenarc.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class StageMessages {
    public enum UserType {USER, POI, nextPOI}
    public enum RequestState {INITIALIZED, ACCEPTED, ACCEPTED_DIAVGEIA, REVIEW, REJECTED, FINISHED}

    UserServiceImpl userServiceImpl;

    @Autowired
    private StageMessages(GenericService<User> userServiceImpl) {
        this.userServiceImpl = (UserServiceImpl) userServiceImpl;
    }

    public List<EmailMessage> createMessages(String prevStage, String nextStage, Request request) {
        List<EmailMessage> emails = new ArrayList<>();
//        List<User> users = userServiceImpl.getUsersWithImmediateEmailPreference();

        String firstname;
        String lastname;
        String subject = "[ARC-REQUEST] Αίτηση " + request.getId();


        if (request.getStatus().equals("rejected")) {
            emails.add(createMessage(request.getRequester().getEmail(), subject, messageTemplates(
                    null, null, request.getId(), UserType.USER, RequestState.REJECTED,
                    request.getStage1().getRequestDate())));
            return emails;
        }

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
        // Stage 2 -> 3
        else if (prevStage.equals("2") && nextStage.equals("3")) {
            firstname = request.getStage2().getUser().getFirstname();
            lastname = request.getStage2().getUser().getLastname();

            // email to user
            emails.add(createMessage(request.getRequester().getEmail(), subject, messageTemplates(
                    null, null, request.getId(), UserType.USER, RequestState.ACCEPTED,
                    request.getStage1().getRequestDate())));

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
                    .forEach(operator -> {
                        emails.add(createMessage(operator.getEmail(), subject,
                                messageTemplates(null, null, request.getId(),
                                        UserType.nextPOI, RequestState.ACCEPTED, null)));
                        operator.getDelegates()
                                .forEach(delegate -> emails.add(createMessage(
                                        delegate.getEmail(), subject,
                                        messageTemplates(null, null, request.getId(),
                                                UserType.nextPOI, RequestState.ACCEPTED, null))));
                    });
        }
        // Stage 3 -> 4
        else if (prevStage.equals("3") && nextStage.equals("4")) {
            firstname = request.getStage3().getUser().getFirstname();
            lastname = request.getStage3().getUser().getLastname();

            // email report to all POIs and their delegates - getOperator() returns a list of POIs
            request.getProject().getOperator()
                    .forEach(operator -> {
                        emails.add(createMessage(operator.getEmail(), subject,
                                messageTemplates(firstname, lastname, request.getId(),
                                        UserType.POI, RequestState.ACCEPTED, null)));
                        operator.getDelegates()
                                .forEach(delegate -> emails.add(createMessage(
                                        delegate.getEmail(), subject,
                                        messageTemplates(firstname, lastname, request.getId(),
                                                UserType.POI, RequestState.ACCEPTED, null))));
                    });

            // emails to next POI and delegates
            emails.add(createMessage(request.getProject().getInstitute().getOrganization().getPOI().getEmail(),
                    subject, messageTemplates(null, null, request.getId(), UserType.nextPOI,
                            RequestState.ACCEPTED, null)));

            // email to next POI delegates
            request.getProject().getInstitute().getOrganization().getPOI().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            messageTemplates(null, null, request.getId(), UserType.nextPOI,
                                    RequestState.ACCEPTED, null))));
        }
        // Stage 4 -> 5
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
        // Stage 5 -> 5a
        else if (prevStage.equals("4") && nextStage.equals("5a")) {
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
        // Stage 5 -> 5b
        else if (prevStage.equals("4") && nextStage.equals("5b")) {
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
            // Stage 5 -> 6
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
            // Stage 5a -> 6
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
            // Stage 5b -> 6
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
        // Stage 6 -> 7
        else if (prevStage.equals("6") && nextStage.equals("7")) {
            firstname = request.getStage6().getUser().getFirstname();
            lastname = request.getStage6().getUser().getLastname();

            // user email
            emails.add(createMessage(request.getRequester().getEmail(), subject, messageTemplates(firstname, lastname,
                    request.getId(), UserType.USER, RequestState.ACCEPTED_DIAVGEIA, request.getStage6().getDate())));

            // email report to POI
            emails.add(createMessage(
                    request.getProject().getInstitute().getDiaugeia().getEmail(),
                    subject, messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                            RequestState.ACCEPTED, null)));

            // email report to all POI delegates
            request.getProject().getInstitute().getDiaugeia().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                                    RequestState.ACCEPTED, null))));


            // emails to all next POIs and their delegates - getOperator() returns a list of POIs
            request.getProject().getOperator()
                    .forEach(operator -> {
                        emails.add(createMessage(operator.getEmail(), subject,
                                messageTemplates(null, null, request.getId(),
                                        UserType.nextPOI, RequestState.ACCEPTED, null)));
                        operator.getDelegates()
                                .forEach(delegate -> emails.add(createMessage(
                                        delegate.getEmail(), subject,
                                        messageTemplates(null, null, request.getId(),
                                                UserType.nextPOI, RequestState.ACCEPTED, null))));
                    });
        }
        // Stage 7 -> 8
        else if (prevStage.equals("7") && nextStage.equals("8")) {
            firstname = request.getStage7().getUser().getFirstname();
            lastname = request.getStage7().getUser().getLastname();

            // email report to all POIs and their delegates - getOperator() returns a list of POIs
            request.getProject().getOperator()
                    .forEach(operator -> {
                        emails.add(createMessage(operator.getEmail(), subject,
                                messageTemplates(firstname, lastname, request.getId(),
                                        UserType.POI, RequestState.ACCEPTED, null)));
                        operator.getDelegates()
                                .forEach(delegate -> emails.add(createMessage(
                                        delegate.getEmail(), subject,
                                        messageTemplates(firstname, lastname, request.getId(),
                                                UserType.POI, RequestState.ACCEPTED, null))));
                    });

            // email to next POI
            emails.add(createMessage(
                    request.getProject().getInstitute().getAccountingDirector().getEmail(),
                    subject, messageTemplates(null, null, request.getId(), UserType.nextPOI,
                            RequestState.ACCEPTED, null)));

            // email to next POI delegates
            request.getProject().getInstitute().getAccountingDirector().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            messageTemplates(null, null, request.getId(), UserType.nextPOI,
                                    RequestState.ACCEPTED, null))));
        }
        // Stage 8 -> 9
        else if (prevStage.equals("8") && nextStage.equals("9")) {
            firstname = request.getStage8().getUser().getFirstname();
            lastname = request.getStage8().getUser().getLastname();

            // email report to POI
            emails.add(createMessage(
                    request.getProject().getInstitute().getAccountingDirector().getEmail(),
                    subject, messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                            RequestState.ACCEPTED, null)));

            // email report to all POI delegates
            request.getProject().getInstitute().getAccountingDirector().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                                    RequestState.ACCEPTED, null))));

            // email to next POI
            emails.add(createMessage(
                    request.getProject().getInstitute().getOrganization().getPOI().getEmail(),
                    subject, messageTemplates(null, null, request.getId(), UserType.nextPOI,
                            RequestState.ACCEPTED, null)));

            // email to next POI delegates
            request.getProject().getInstitute().getOrganization().getPOI().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            messageTemplates(null, null, request.getId(), UserType.nextPOI,
                                    RequestState.ACCEPTED, null))));
        }
        // Stage 9 -> 10
        else if (prevStage.equals("9") && nextStage.equals("10")) {
            firstname = request.getStage9().getUser().getFirstname();
            lastname = request.getStage9().getUser().getLastname();

            // email report to POI
            emails.add(createMessage(
                    request.getProject().getInstitute().getOrganization().getPOI().getEmail(),
                    subject, messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                            RequestState.ACCEPTED, null)));

            // email report to all POI delegates
            request.getProject().getInstitute().getOrganization().getPOI().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                                    RequestState.ACCEPTED, null))));

            // email to next POI
            emails.add(createMessage(
                    request.getProject().getInstitute().getOrganization().getDirector().getEmail(),
                    subject, messageTemplates(null, null, request.getId(), UserType.nextPOI,
                            RequestState.ACCEPTED, null)));

            // email to next POI delegates
            request.getProject().getInstitute().getOrganization().getDirector().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            messageTemplates(null, null, request.getId(), UserType.nextPOI,
                                    RequestState.ACCEPTED, null))));
        }
        // Stage 10 -> 11
        else if (prevStage.equals("10") && nextStage.equals("11")) {
            firstname = request.getStage10().getUser().getFirstname();
            lastname = request.getStage10().getUser().getLastname();

            // email report to POI
            emails.add(createMessage(
                    request.getProject().getInstitute().getOrganization().getDirector().getEmail(),
                    subject, messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                            RequestState.ACCEPTED, null)));

            // email report to all POI delegates
            request.getProject().getInstitute().getOrganization().getDirector().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                                    RequestState.ACCEPTED, null))));

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
        // Stage 11 -> 12
        else if (prevStage.equals("11") && nextStage.equals("12")) {
            firstname = request.getStage11().getUser().getFirstname();
            lastname = request.getStage11().getUser().getLastname();

            // email report to POI
            emails.add(createMessage(
                    request.getProject().getInstitute().getDiaugeia().getEmail(),
                    subject, messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                            RequestState.ACCEPTED, null)));

            // email report to all POI delegates
            request.getProject().getInstitute().getDiaugeia().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                                    RequestState.ACCEPTED, null))));

            // email to next POI
            emails.add(createMessage(
                    request.getProject().getInstitute().getAccountingRegistration().getEmail(),
                    subject, messageTemplates(null, null, request.getId(), UserType.nextPOI,
                            RequestState.ACCEPTED, null)));

            // email to next POI delegates
            request.getProject().getInstitute().getAccountingRegistration().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            messageTemplates(null, null, request.getId(), UserType.nextPOI,
                                    RequestState.ACCEPTED, null))));
        }
        // Stage 12 -> 13
        else if (prevStage.equals("12") && nextStage.equals("12") && request.getStatus().equals("accepted")) {
            firstname = request.getStage11().getUser().getFirstname();
            lastname = request.getStage11().getUser().getLastname();

            // email report to POI
            emails.add(createMessage(
                    request.getProject().getInstitute().getAccountingRegistration().getEmail(),
                    subject, messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                            RequestState.ACCEPTED, null)));

            // email report to all POI delegates
            request.getProject().getInstitute().getAccountingRegistration().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                                    RequestState.ACCEPTED, null))));

            // email to next POI
            emails.add(createMessage(
                    request.getProject().getInstitute().getAccountingPayment().getEmail(),
                    subject, messageTemplates(null, null, request.getId(), UserType.nextPOI,
                            RequestState.ACCEPTED, null)));

            // email to next POI delegates
            request.getProject().getInstitute().getAccountingPayment().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            messageTemplates(null, null, request.getId(), UserType.nextPOI,
                                    RequestState.ACCEPTED, null))));
        } else if (prevStage.equals("13") && request.getStatus().equals("completed")) {
            firstname = request.getStage11().getUser().getFirstname();
            lastname = request.getStage11().getUser().getLastname();

            // email report to POI
            emails.add(createMessage(
                    request.getProject().getInstitute().getAccountingPayment().getEmail(),
                    subject, messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                            RequestState.ACCEPTED, null)));

            // email report to all POI delegates
            request.getProject().getInstitute().getAccountingPayment().getDelegates()
                    .forEach(delegate -> emails.add(createMessage(
                            delegate.getEmail(), subject,
                            messageTemplates(firstname, lastname, request.getId(), UserType.POI,
                                    RequestState.ACCEPTED, null))));
        }

//        TODO: uncomment when filterOutNonImmediate is ready
//        List<EmailMessage> mails;
//        mails = filterOutNonImmediate(emails);
//        return mails;
        return emails; // TODO remove when you uncomment this^^
    }

    private EmailMessage createMessage(String to, String subject, String text) {
        EmailMessage mail = new EmailMessage();
        mail.setRecipient(to);
        mail.setSubject(subject);
        mail.setText(text);
        return mail;
    }

    public User getUserByEmail(final List<User> users, final String email) {
        if(users == null || email == null)
            return null;
        Optional<User> user = users.parallelStream().filter(u -> u.getEmail().equals(email)).findAny();

        return user.orElse(null);
//        return users.stream().filter(user -> user.getEmail().equals(email)).findAny().get();
    }

    private List<EmailMessage> filterOutNonImmediate(List<EmailMessage> emails) {
        List<EmailMessage> emailList = new ArrayList<>();
//        List<User> users = userServiceImpl.getUsersWithImmediateEmailPreference();
        List<User> users = userServiceImpl.getAll(new FacetFilter()).getResults();
        for (Iterator<EmailMessage> iterator = emails.iterator(); iterator.hasNext();) {
            EmailMessage email = iterator.next();
            User user = getUserByEmail(users, email.getRecipient());
//            if (user.getReceiveEmails() && user.getImmediateEmails()) {
            if (user != null) {
                emailList.add(email);
            }
        }
        return emailList;
    }

    private String messageTemplates(String firstname, String lastname, String id, UserType type,
                                    RequestState state, String date_secs) {
        String messageText = null;
        String date = new SimpleDateFormat("dd/MM/yyyy").format(new Date(Long.parseLong(date_secs))).toString();
        if (type == UserType.USER) {
            if (state == RequestState.INITIALIZED) {
                messageText = "Το αίτημά σας, με κωδικό " + id + ", υποβλήθηκε επιτυχώς στις " + date;
            } else if (state == RequestState.ACCEPTED) {
                messageText = "Το αίτημά σας εγκρίθηκε από τον επιστημονικό υπεύθυνο: " + firstname + " " + lastname;
            } else if (state == RequestState.ACCEPTED_DIAVGEIA) {
                messageText = "Το αίτημά σας με κωδικό " + id + " εγκρίθηκε από τον υπεύθυνο της Διαύγειας, " +
                        firstname + " " + lastname + " στις " + date + ". \nΜπορείτε να προχωρήσετε με τις αγορές σας.";
            } else if (state == RequestState.FINISHED) {
                messageText = "Το αίτημά σας με κωδικό " + id + " ελέγχθηκε κι εγκρίθηκε επιτυχώς!";
            } else if (state == RequestState.REJECTED) {
                messageText = "Το αίτημά σας με κωδικό " + id + " απορρίφθηκε.";
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
            } else if (state == RequestState.REVIEW) {
                messageText = "Το αίτημα με κωδικό " + id + " βρίσκεται υπό επανέλεγχο";
            } else if (state == RequestState.FINISHED) {
                messageText = "";
            } else if (state == RequestState.REJECTED) {
                messageText = "";
            }
        }
        return messageText;
    }
}
