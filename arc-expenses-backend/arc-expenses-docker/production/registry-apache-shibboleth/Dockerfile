FROM unicon/shibboleth-sp

MAINTAINER "pkanakakis@di.uoa.gr"

RUN yum -y update \
    && yum -y install php mod_ssl   

COPY  arc-shibboleth /etc/shibboleth/
COPY  apache2 /etc/httpd/conf.d/