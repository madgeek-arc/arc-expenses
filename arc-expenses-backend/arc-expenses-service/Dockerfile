FROM rossbachp/apache-tomcat8
MAINTAINER "pkanakakis@di.uoa.gr"
RUN ["rm", "-fr", "/usr/local/tomcat/webapps/ROOT"]
COPY ./target/arc-expenses-service.war /usr/local/tomcat/webapps/arc-expenses-service.war
COPY ./src/main/resources/application.properties /usr/local/tomcat/lib/registry.properties
VOLUME /var/log/arc-expenses-service:/var/log/arc-expenses-service/
CMD ["catalina.sh", "run"]

