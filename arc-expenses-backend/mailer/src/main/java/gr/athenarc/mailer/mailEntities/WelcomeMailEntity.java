package gr.athenarc.mailer.mailEntities;

public class WelcomeMailEntity {

    private String name;

    public WelcomeMailEntity(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
