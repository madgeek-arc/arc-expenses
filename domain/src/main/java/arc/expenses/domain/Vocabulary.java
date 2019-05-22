package arc.expenses.domain;



public class Vocabulary  {

    private String projectID;
    private String projectAcronym;
    private String instituteId;
    private String instituteName;

    public Vocabulary(){

    }

    public Vocabulary(String project_id, String project_acronym, String project_institute, String instituteName) {
        this.projectID=project_id;
        this.projectAcronym=project_acronym;
        this.instituteId =project_institute;
        this.instituteName=instituteName;
    }


    public String getProjectID() {
        return projectID;
    }

    public void setProjectID(String projectID) {
        this.projectID = projectID;
    }

    public String getProjectAcronym() {
        return projectAcronym;
    }

    public void setProjectAcronym(String projectAcronym) {
        this.projectAcronym = projectAcronym;
    }

    public String getInstituteId() {
        return instituteId;
    }

    public void setInstituteId(String instituteId) {
        this.instituteId = instituteId;
    }

    public String getInstituteName() {
        return instituteName;
    }

    public void setInstituteName(String instituteName) {
        this.instituteName = instituteName;
    }
}
