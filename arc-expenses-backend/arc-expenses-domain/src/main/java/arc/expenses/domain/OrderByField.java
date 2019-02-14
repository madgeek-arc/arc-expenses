package arc.expenses.domain;

public enum OrderByField {

    REQUEST_PROJECT_ACRONYM("request_project_acronym"),
    REQUEST_INSTITUTE("request_institute"),
    CREATION_DATE("creation_date");

    private final String text;

    OrderByField(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
