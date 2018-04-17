FROM tomcat:8.5-jre8-alpine
MAINTAINER "pkanakakis@di.uoa.gr"
RUN ["rm", "-fr", "/usr/local/tomcat/webapps/ROOT"]
COPY ./target/arc-expenses-service.war /usr/local/tomcat/webapps/arc-expenses-service.war
COPY ./src/main/resources/application.properties /usr/local/tomcat/lib/registry.properties
CMD ["catalina.sh", "run"]

