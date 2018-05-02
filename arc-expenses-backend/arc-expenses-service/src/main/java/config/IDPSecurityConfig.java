package config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    SAMLLogoutSuccessHandler samlLogoutSuccessHandler;

    @Autowired
    SAMLBasicFilter samlBasicFilter;

    @Autowired
    SAMLRedirectFilter samlRedirectFilter;

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http
                .antMatcher("/user/*")
                .addFilterBefore(samlBasicFilter, BasicAuthenticationFilter.class)
                .addFilterAfter(samlRedirectFilter,FilterSecurityInterceptor.class)
//        http.logout().logoutSuccessHandler(samlLogoutSuccessHandler).permitAll();
    }
}
