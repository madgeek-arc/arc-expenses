package arc.expenses.domain;

import gr.athenarc.domain.*;

public class RequestSummary {

    private BaseInfo baseInfo;
    private String requestId;
    private String requestType;
    private String projectAcronym;
    private String instituteName;
    private String requestFullName;
    private boolean canEdit;

    public RequestSummary() { }

    public BaseInfo getBaseInfo() {
        return baseInfo;
    }

    public void setBaseInfo(BaseInfo baseInfo) {
        this.baseInfo = baseInfo;
    }


    public boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getProjectAcronym() {
        return projectAcronym;
    }

    public void setProjectAcronym(String projectAcronym) {
        this.projectAcronym = projectAcronym;
    }

    public String getInstituteName() {
        return instituteName;
    }

    public void setInstituteName(String instituteName) {
        this.instituteName = instituteName;
    }

    public String getRequestFullName() {
        return requestFullName;
    }

    public void setRequestFullName(String requestFullName) {
        this.requestFullName = requestFullName;
    }
}
