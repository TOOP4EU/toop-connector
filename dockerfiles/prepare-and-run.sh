#!/bin/sh
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


TOOP_DIR=/toop-dir

echo "************************************************************************"
echo "************************************************************************"
echo "************************************************************************"
echo "***************                                          ***************"
echo "***************      RUNNING TOOP Connector ${TOOP_CONNECTOR_VERSION}        ***************" 
echo "***************                                          ***************"            
echo "************************************************************************"            
echo "************************************************************************"            
echo "************************************************************************"

echo "Create Directories"

#create connector directories
mkdir -p $TOOP_DIR/tc/dumps/from-dc
mkdir -p $TOOP_DIR/tc/dumps/to-dc
mkdir -p $TOOP_DIR/tc/dumps/from-dp
mkdir -p $TOOP_DIR/tc/dumps/to-dp
mkdir -p $TOOP_DIR/tc/config


echo "Check TC Config"
if [ -f $TOOP_DIR/tc/config/toop-connector.properties ]
then
    echo "A configuration already exists in the $TOOP_DIR/tc/config/toop-connector.properties"
else
    echo "No configuration file found at $TOOP_DIR/tc/config/ Create a default one"
    cp /supplementaryFiles/default-toop-connector.properties $TOOP_DIR/tc/config/toop-connector.properties
    cp /supplementaryFiles/playground-keystore.jks           $TOOP_DIR/tc/config/playground-keystore.jks
fi

echo "RUN TOMCAT"
catalina.sh run
