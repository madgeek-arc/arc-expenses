package arc.expenses.config;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class SAMLAuthentication extends AbstractAuthenticationToken {

    private final String firstname;
    private final String lastname;
    private final String email;
    private final String uid;


    public SAMLAuthentication(String firstname, String lastname, String email, String uid,
                              Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.uid = uid;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return null;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public String getEmail() {
        return email;
    }

    public String getUid() {
        return uid;
    }
}
