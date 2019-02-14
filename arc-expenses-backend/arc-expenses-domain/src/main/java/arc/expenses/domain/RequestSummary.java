package arc.expenses.domain;

import gr.athenarc.domain.*;

public class RequestSummary {

    private BaseInfo baseInfo;
    private String request_id;
    private String request_type;
    private String project_acronym;
    private String institute_name;
    private String request_full_name;
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

    public String getRequest_id() {
        return request_id;
    }

    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }

    public String getRequest_type() {
        return request_type;
    }

    public void setRequest_type(String request_type) {
        this.request_type = request_type;
    }

    public String getProject_acronym() {
        return project_acronym;
    }

    public void setProject_acronym(String project_acronym) {
        this.project_acronym = project_acronym;
    }

    public String getInstitute_name() {
        return institute_name;
    }

    public void setInstitute_name(String institute_name) {
        this.institute_name = institute_name;
    }

    public String getRequest_full_name() {
        return request_full_name;
    }

    public void setRequest_full_name(String request_full_name) {
        this.request_full_name = request_full_name;
    }
}
