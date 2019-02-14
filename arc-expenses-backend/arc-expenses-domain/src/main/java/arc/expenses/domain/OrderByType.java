package arc.expenses.domain;

public enum OrderByType {

    ASC("ASC"),
    DSC("DSC");

    private final String text;

    OrderByType(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
