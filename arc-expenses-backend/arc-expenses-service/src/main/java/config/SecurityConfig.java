package config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.stereotype.Component;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	
	@Autowired
    CustomizedLogoutSuccessHandler customizedLogoutSuccessHandler;

    @Autowired
    CustomizedAuthenticationSuccessHandler customizedAuthenticationSuccessHandler;
	
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.addFilterAfter(
                new SAMLFilter(), BasicAuthenticationFilter.class);

       /* http
            .authorizeRequests()
                .antMatchers("/","/logoutsuccessful").permitAll()
                .antMatchers("/admin").hasRole("ADMIN")
                .anyRequest().authenticated()
                .and()
            .formLogin()
                .successHandler(customizedAuthenticationSuccessHandler)
//                .loginPage("/user/logout")
                .permitAll()
                .and()
            .logout().logoutSuccessHandler(customizedLogoutSuccessHandler)
                .permitAll();
        http.exceptionHandling().accessDeniedPage("/403");*/
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth
            .inMemoryAuthentication()
                .withUser("user").password("user").roles("USER")
                .and()
        		.withUser("admin").password("admin").roles("ADMIN");
    }
}
