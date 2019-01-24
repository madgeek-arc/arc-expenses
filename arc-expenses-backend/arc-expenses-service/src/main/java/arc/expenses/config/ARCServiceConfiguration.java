package arc.expenses.config;

import gr.athenarc.domain.*;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass=true)
@EnableWebMvc
@EnableAsync
@ComponentScan(basePackages = {"eu.openminted.registry.core","arc.expenses.*"})
@PropertySource(value = {"classpath:application.properties", "classpath:registry.properties"})
public class ARCServiceConfiguration extends WebMvcConfigurerAdapter {

    private static Logger logger = LogManager.getLogger(ARCServiceConfiguration.class);

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

}