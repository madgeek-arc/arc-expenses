package gr.athenarc.mailer.mailEntities;

public class ContactEmailEntity {

    private String body;

    private String fullname;

    private String email;

    public ContactEmailEntity(String body, String fullname, String email) {
        this.body = body;
        this.fullname = fullname;
        this.email = email;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
