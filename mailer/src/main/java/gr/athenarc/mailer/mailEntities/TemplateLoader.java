package gr.athenarc.mailer.mailEntities;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

public class TemplateLoader {

public static <ENTITY> String loadFilledTemplate(ENTITY entity, String templatePath) throws IOException {
    final Properties p = new Properties();
    p.setProperty("resource.loader", "class");
    p.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
    Velocity.init(p);

    final VelocityContext context = new VelocityContext();
    context.put("entity", entity);
    final Template template = Velocity.getTemplate(templatePath,"UTF-8");
    try (StringWriter writer = new StringWriter()) {
        template.merge(context, writer);
        return writer.toString();
    }
}}