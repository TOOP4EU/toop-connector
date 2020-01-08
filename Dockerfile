#
# Copyright (C) 2018-2020 toop.eu
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

FROM tomcat:9-jre11

ARG TOOP_CONNECTOR_VERSION
ARG TC_WAR_NAME=toop-connector-webapp-${TOOP_CONNECTOR_VERSION}.war


#COPY SCRIPTS and the default config TO THE ROOT
RUN  mkdir /supplementaryFiles

COPY dockerfiles/default-toop-connector.properties dockerfiles/playground-keystore.jks /supplementaryFiles/
COPY dockerfiles/prepare-and-run.sh /

#create tc webapp folder
WORKDIR $CATALINA_HOME/webapps

ENV TC_PORT=$TC_PORT \
    TC_WAR_NAME=$TC_WAR_NAME \
    TOOP_CONNECTOR_VERSION=${TOOP_CONNECTOR_VERSION} \
    JAVA_OPTS="$JAVA_OPTS -Dtoop.connector.server.properties.path=/toop-dir/tc/config/toop-connector.properties" \
    CATALINA_OPTS="$CATALINA_OPTS -Dorg.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH=true -Djava.security.egd=file:/dev/urandom"

ADD toop-connector-webapp/target/$TC_WAR_NAME ./

RUN rm -fr manager host-manager ROOT && \
    unzip $TC_WAR_NAME -d ROOT  && \
    rm -fr $TC_WAR_NAME  && \
    chmod +x /prepare-and-run.sh 

WORKDIR /
#run connector setup
CMD ["/prepare-and-run.sh"]
