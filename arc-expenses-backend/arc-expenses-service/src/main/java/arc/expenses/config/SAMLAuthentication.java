package arc.expenses.config;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component("samlAuthentication")
public class SAMLAuthentication extends AbstractAuthenticationToken {

    private  String firstname;
    private  String lastname;
    private  String email;
    private  String uid;

    public SAMLAuthentication(){
        super(null);
    }

    public SAMLAuthentication(String firstname, String lastname, String email, String uid,
                              Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.uid = uid;
        setAuthenticated(true);
    }

    public SAMLAuthentication(Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
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
