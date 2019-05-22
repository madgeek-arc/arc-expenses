package arc.expenses.config;

import gr.athenarc.domain.*;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass=true)
@EnableWebMvc
@EnableAsync
@ComponentScan(basePackages = {"eu.openminted.registry.core","arc.expenses"})
@PropertySource(value = {"classpath:application.properties", "classpath:registry.properties"})
public class ARCServiceConfiguration implements WebMvcConfigurer {

    private static Logger logger = LogManager.getLogger(ARCServiceConfiguration.class);

    @Autowired
    DataSource dataSource;


    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        registry.addResourceHandler("swagger-ui.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    @Bean
    JAXBContext jaxbContext() {
        try {
            return JAXBContext.newInstance(
                    Request.class,
                    RequestPayment.class,
                    RequestApproval.class,
                    Organization.class,
                    Institute.class,
                    Project.class,
                    User.class);
        } catch (JAXBException e) {
            logger.fatal("Could not instantiate JAXB context");
            return null;
        }
    }

    @Bean
    public CommonsMultipartResolver multipartResolver(){
        return new CommonsMultipartResolver();
    }

    @Bean
    PropertyPlaceholderConfigurer propertyPlaceholderConfigurer(){
        PropertyPlaceholderConfigurer propertyPlaceholderConfigurer = new PropertyPlaceholderConfigurer();
        propertyPlaceholderConfigurer.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
        return propertyPlaceholderConfigurer;
    }

    @PostConstruct
    public void flywayMigration(){
        Flyway flyway = Flyway.configure().dataSource(dataSource).locations("classpath:db/migrations").load();
        try {
            flyway.baseline();
        }catch (FlywayException ex){
            logger.warn("Flyway exception on baseline");
        }
        flyway.migrate();
    }

}