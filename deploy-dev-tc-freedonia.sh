#/bin/sh

DATE=`date +%Y%m%d`
BASEDIR=/opt/tomcat/webapps
FILENAME=toop-connector-webapp-0.10.6-SNAPSHOT.war

if [ -f ~/$FILENAME ]
  then
    echo stopping Tomcat
    sudo service tomcat stop

    echo Cleaning exisiting dirs
    cd $BASEDIR
    sudo mv ROOT/ ~/tc.$DATE
    sudo chmod 777 ~/tc.$DATE

    echo ROOT
    APPDIR=$BASEDIR/ROOT
    [ ! -d $APPDIR ] && sudo mkdir $APPDIR
    sudo cp ~/$FILENAME $APPDIR
    cd $APPDIR
    sudo unzip -q $FILENAME && sudo rm $FILENAME
    sudo rm $APPDIR/WEB-INF/classes/private-*.properties
    sudo rm $APPDIR/WEB-INF/classes/toop-connector.properties
    sudo mv $APPDIR/WEB-INF/classes/toop-connector.freedonia-dev.properties $APPDIR/WEB-INF/classes/toop-connector.properties

    echo Done!
  else
    echo "Source file ~/$FILENAME not existing!"
fi
