FROM toop/tomcat-jdk-image:1

ARG TOOP_CONNECTOR_VERSION
ARG TC_WAR_NAME=toop-connector-webapp-${TOOP_CONNECTOR_VERSION}.war


#COPY SCRIPTS and the default config TO THE ROOT
RUN  mkdir /apriorifiles

COPY dockerfiles/default-toop-connector.properties dockerfiles/playground-keystore.jks /apriorifiles/
COPY dockerfiles/prepare-and-run.sh /

#create tc webapp folder
WORKDIR $CATALINA_HOME/webapps

ENV TC_PORT=$TC_PORT \
    TC_WAR_NAME=$TC_WAR_NAME \
    TOOP_CONNECTOR_VERSION=${TOOP_CONNECTOR_VERSION} \
    JAVA_OPTS="-Dtoop.connector.server.properties.path=/toop-dir/tc/config/toop-connector.properties" \
    CATALINA_OPTS="$CATALINA_OPTS -Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true -Djava.security.egd=file:/dev/urandom"

ADD toop-connector-webapp/target/$TC_WAR_NAME ./

RUN rm -fr manager host-manager ROOT && \
    unzip $TC_WAR_NAME -d ROOT  && \
    rm -fr $TC_WAR_NAME  && \
    chmod +x /prepare-and-run.sh 

WORKDIR /
#run connector setup
CMD ["/prepare-and-run.sh"]
