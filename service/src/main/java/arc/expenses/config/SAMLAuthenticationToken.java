package arc.expenses.config;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class SAMLAuthenticationToken extends AbstractAuthenticationToken {

    private  final String firstname;
    private  final String lastname;
    private  final String email;
    private  final String uid;
    private  boolean authenticated;
    private final String principal;
    private Collection<GrantedAuthority> authorities;



    public SAMLAuthenticationToken(String firstname, String lastname, String email, String uid,
                                   Collection<GrantedAuthority> authorities) {
        super(authorities);
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.uid = uid;
        this.principal = email;
        this.authorities = authorities;
        setAuthenticated(true);
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean b) throws IllegalArgumentException {
        this.authenticated = b;
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

    @Override
    public String getName() {
        return null;
    }
}
