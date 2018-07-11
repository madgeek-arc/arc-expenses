FROM unicon/shibboleth-sp

MAINTAINER "pkanakakis@di.uoa.gr"

RUN yum -y update \
    && yum -y install php mod_ssl apt-get install -y libapache2-mod-shib2 a2enmod shib2

COPY  apache2/conf.d /etc/httpd/conf.d/
COPY  apache2/ssl /etc/httpd/

COPY  copy-config-files.sh /usr/local/bin/

RUN chmod 777 /usr/local/bin/copy-config-files.sh \
    && ln -s /usr/local/bin/copy-config-files.sh /

ENTRYPOINT ["copy-config-files.sh"]
