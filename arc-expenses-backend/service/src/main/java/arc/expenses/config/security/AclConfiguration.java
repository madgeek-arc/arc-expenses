package arc.expenses.config.security;

import arc.expenses.acl.ArcPermissionFactory;
import arc.expenses.service.AclService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.ehcache.EhCacheFactoryBean;
import org.springframework.cache.ehcache.EhCacheManagerFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.acls.domain.*;
import org.springframework.security.acls.jdbc.BasicLookupStrategy;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.PermissionGrantingStrategy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.sql.DataSource;

@Configuration
public class AclConfiguration {


    @Autowired
    DataSource dataSource;

    @Bean
    public MethodSecurityExpressionHandler
    defaultMethodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler expressionHandler
                = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setPermissionEvaluator(permissionEvaluator());
        return expressionHandler;
    }

    @Bean
    public AclPermissionEvaluator permissionEvaluator(){
        AclPermissionEvaluator aclPermissionEvaluator = new AclPermissionEvaluator(aclService());
        aclPermissionEvaluator.setPermissionFactory(permissionFactory());
        return aclPermissionEvaluator;
    }

    @Bean
    public ArcPermissionFactory permissionFactory(){
        return new ArcPermissionFactory();
    }

    @Bean
    public AclAuthorizationStrategy aclAuthorizationStrategy() {
        return new AclAuthorizationStrategyImpl(
                new SimpleGrantedAuthority("ROLE_EXECUTIVE"));
    }

    @Bean
    public PermissionGrantingStrategy permissionGrantingStrategy() {
        return new DefaultPermissionGrantingStrategy(
                new ConsoleAuditLogger());
    }

    @Bean
    public EhCacheBasedAclCache aclCache() {
        return new EhCacheBasedAclCache(
                aclEhCacheFactoryBean().getObject(),
                permissionGrantingStrategy(),
                aclAuthorizationStrategy()
        );
    }

    @Bean
    public EhCacheFactoryBean aclEhCacheFactoryBean() {
        EhCacheFactoryBean ehCacheFactoryBean = new EhCacheFactoryBean();
        ehCacheFactoryBean.setCacheManager(aclCacheManager().getObject());
        ehCacheFactoryBean.setCacheName("aclCache");
        return ehCacheFactoryBean;
    }

    @Bean
    public EhCacheManagerFactoryBean aclCacheManager() {
        return new EhCacheManagerFactoryBean();
    }

    @Bean
    public LookupStrategy lookupStrategy() {
        BasicLookupStrategy basicLookupStrategy =  new BasicLookupStrategy(
                dataSource,
                aclCache(),
                aclAuthorizationStrategy(),
                new ConsoleAuditLogger()
        );
        basicLookupStrategy.setPermissionFactory(permissionFactory());
        basicLookupStrategy.setAclClassIdSupported(true);
        return basicLookupStrategy;
    }

    @Bean
    public AclService aclService() {
        AclService aclService = new arc.expenses.service.AclService(
                dataSource, lookupStrategy(), aclCache());
        aclService.setClassIdentityQuery("SELECT lastval();");
        aclService.setSidIdentityQuery("SELECT lastval();");
        aclService.setAclClassIdSupported(true);
        return aclService;
    }
}
