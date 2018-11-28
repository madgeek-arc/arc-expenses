package arc.expenses.domain;

public class Executive {

    private String firstname;
    private String lastname;
    private String email;

    public Executive(String email, String firstname, String lastname) {
        this.firstname = firstname;
        this.email= email;
        this.lastname = lastname;
    }


    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
