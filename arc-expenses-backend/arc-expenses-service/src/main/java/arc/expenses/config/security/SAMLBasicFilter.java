package arc.expenses.config.security;

import arc.expenses.config.SAMLAuthenticationToken;
import arc.expenses.service.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

@Component
public class SAMLBasicFilter extends GenericFilterBean{

    @Autowired
    UserServiceImpl userService;

    @Value("${redirect.error.url}")
    String redirect_error_url;

    @Value("${debug.idp}")
    Boolean debug_idp;

    @Override
    public void doFilter(
            ServletRequest req,
            ServletResponse res,
            FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        Cookie sessionCookie = null;


        if(SecurityContextHolder.getContext().getAuthentication() == null){

            Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();
            grantedAuthorities.addAll(userService.getRole(request.getHeader("AJP_email")));

            SAMLAuthenticationToken samlAuthentication = new SAMLAuthenticationToken(request.getHeader("AJP_firstname"),
                    request.getHeader("AJP_lastname"),request.getHeader("AJP_email"),
                    request.getHeader("AJP_uid"), grantedAuthorities);

            SecurityContextHolder.getContext().setAuthentication(samlAuthentication);

        }

        if(!debug_idp && !request.getHeader("AJP_eppn").equals(""))
            sessionCookie = new Cookie("arc_currentUser", request.getHeader("AJP_eppn"));
        else
            response.sendRedirect(redirect_error_url);

        if(debug_idp && !request.getHeader("AJP_uid").equals(""))
            sessionCookie = new Cookie("arc_currentUser", request.getHeader("AJP_uid"));
        else
            response.sendRedirect(redirect_error_url);

        int expireSec = 14400;
        sessionCookie.setMaxAge(expireSec);
        sessionCookie.setPath("/");
        response.addCookie(sessionCookie);
        chain.doFilter(req, res);
    }
}
