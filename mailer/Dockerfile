FROM java:alpine
MAINTAINER "jdiplas@gmail.com"

COPY ./target/mailer-*.jar /application.jar

ENTRYPOINT [ "java" , "-jar" , "/application.jar" ]
