FROM centos:centos7

MAINTAINER Unicon, Inc.

RUN rpm -Uvh https://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm \
    && rpm -Uvh https://mirror.webtatic.com/yum/el7/webtatic-release.rpm \
    && yum -y install epel-release \
    && yum -y update \
    && yum -y install httpd mod_ssl php55w php55w-mbstring php55w-mcrypt php55w-pdo php55w-pear php55w-xml wget \
    && yum -y clean all

RUN ssp_version=1.15.4; \
    ssp_hash=349cf5d9f9ecbbced0e6f6794d26d5242fc9dafbd34268aeeb200182c24f88a5; \
    wget https://github.com/simplesamlphp/simplesamlphp/releases/download/v$ssp_version/simplesamlphp-$ssp_version.tar.gz \
    && echo "$ssp_hash  simplesamlphp-$ssp_version.tar.gz" | sha256sum -c - \
	&& cd /var \
	&& tar xzf /simplesamlphp-$ssp_version.tar.gz \
    && mv simplesamlphp-$ssp_version simplesamlphp \
    && rm /simplesamlphp-$ssp_version.tar.gz

#COPY simplesamlphp/config.php /var/www/simplesamlphp/config
#COPY simplesamlphp/authsources.php /var/www/simplesamlphp/config
#COPY simplesamlphp/saml20-sp-remote.php /var/www/simplesamlphp/metadata
#COPY simplesamlphp/server.crt /var/www/simplesamlphp/cert/
#COPY simplesamlphp/server.pem /var/www/simplesamlphp/cert/

RUN yum -y update \
    && yum -y install php55w-ldap \
    && yum -y clean all

COPY simplesamlphp/var-simplesamlphp /var/simplesamlphp/
COPY simplesamlphp/var-www-html /var/www/html/

RUN chown apache:apache /var/simplesamlphp/log/ \
    && chown -R apache:apache /var/simplesamlphp/cert/


# Apache
COPY apache/ports.conf /etc/httpd
COPY apache/ssl.conf /etc/httpd/conf.d/
#COPY apache/simplesamlphp.conf /etc/httpd/conf.d/
COPY apache/cert.crt /etc/ssl/cert/cert.crt
COPY apache/private.key /etc/ssl/private/private.key
RUN a2enmod ssl && \
    #a2dissite 000-default.conf default-ssl.conf && \
    a2ensite ssl.conf


COPY httpd-foreground /usr/local/bin/

CMD ["httpd-foreground"]