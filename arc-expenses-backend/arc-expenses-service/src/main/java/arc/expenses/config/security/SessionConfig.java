package arc.expenses.config.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@ComponentScan("arc.expenses.config.*")
@Order(2)
public class SessionConfig extends WebSecurityConfigurerAdapter{

    @Autowired
    SAMLBasicFilter samlBasicFilter;

    @Autowired
    CustomAccessDeniedHandler customAccessDeniedHandler;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                    .anyRequest()
                    .permitAll()
                .and()
                    .csrf()
                    .disable()
                .addFilterBefore(samlBasicFilter, BasicAuthenticationFilter.class)
                .exceptionHandling().accessDeniedHandler(customAccessDeniedHandler);
    }

    @Autowired
    public void configure(AuthenticationManagerBuilder builder)
            throws Exception {
        builder.inMemoryAuthentication();
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.debug(true);
    }

}
