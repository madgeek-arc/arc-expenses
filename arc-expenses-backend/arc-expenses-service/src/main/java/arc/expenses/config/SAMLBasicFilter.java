package arc.expenses.config;

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

@Component
public class SAMLBasicFilter extends GenericFilterBean{



    @Override
    public void doFilter(
            ServletRequest req,
            ServletResponse res,
            FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if(SecurityContextHolder.getContext().getAuthentication() == null){
            SAMLAuthentication samlAuthentication = new SAMLAuthentication(request.getHeader("AJP_firstname"),
                    request.getHeader("AJP_lastname"),request.getHeader("AJP_email"),request.getHeader("AJP_uid"),null);

            SecurityContextHolder.getContext().setAuthentication(samlAuthentication);
        }

//        Cookie sessionCookie = new Cookie("arc_currentUser", request.getHeader("AJP_eppn"));
        Cookie sessionCookie = new Cookie("arc_currentUser", request.getHeader("AJP_uid"));

        int expireSec = -1;
        sessionCookie.setMaxAge(expireSec);
        sessionCookie.setPath("/");
        response.addCookie(sessionCookie);
        chain.doFilter(req, res);
    }
}
