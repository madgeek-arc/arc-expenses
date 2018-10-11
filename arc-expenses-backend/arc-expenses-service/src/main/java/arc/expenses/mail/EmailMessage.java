package arc.expenses.mail;

public class EmailMessage {
    private String recipient;
    private String subject;
    private String text;

    public EmailMessage() {
    }

    public EmailMessage(String email, String subject, String text) {
        this.recipient = email;
        this.subject = subject;
        this.text = text;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {

        return "\n#########################################################################################\n" +
               String.format("\nTo:%s\nSubject:%s\n%s\n",recipient,subject,text) +
               "#########################################################################################\n";
    }
}

