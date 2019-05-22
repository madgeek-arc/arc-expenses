package gr.athenarc.mailer.mailEntities;

public class InitialEmailEntity {

    private String request_id;
    private String project_acronym;
    private String creation_date;
    private String final_amount;
    private String subject;
    private String url;

    public InitialEmailEntity(String request_id, String project_acronym, String creation_date, String final_amount, String subject, String url) {
        this.request_id = request_id;
        this.project_acronym = project_acronym;
        this.creation_date = creation_date;
        this.final_amount = final_amount;
        this.subject = subject;
        this.url = url;
    }

    public String getRequest_id() {
        return request_id;
    }

    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }

    public String getProject_acronym() {
        return project_acronym;
    }

    public void setProject_acronym(String project_acronym) {
        this.project_acronym = project_acronym;
    }

    public String getCreation_date() {
        return creation_date;
    }

    public void setCreation_date(String creation_date) {
        this.creation_date = creation_date;
    }

    public String getFinal_amount() {
        return final_amount;
    }

    public void setFinal_amount(String final_amount) {
        this.final_amount = final_amount;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
