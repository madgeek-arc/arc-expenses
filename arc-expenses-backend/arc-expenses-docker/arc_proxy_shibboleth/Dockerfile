FROM unicon/shibboleth-sp

MAINTAINER "pkanakakis@di.uoa.gr"

RUN yum -y update \
    && yum -y install php mod_ssl mod_proxy mod_proxy_ajp mod_proxy_http mod_proxy_connect


COPY  arc-shibboleth/ /etc/shibboleth/
COPY  apache2/conf.d /etc/httpd/conf.d/
COPY  apache2/ssl /etc/httpd/ssl