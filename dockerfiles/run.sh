#!/bin/bash



# Options:
# -n network_name
# -t toop_dir
# -c container_name
# -p external_port
# -s smp_container_name_to_link
# -m smp_network_name (the network that the smp container runs in)
# ./run.sh -h prints help
# both arguments are optional

args=`getopt s:m:c:n:t:p:h $*`
set -- $args


# extract options and their arguments into variables.
for i ; do
    case "$i"
    in
        -n)
            network=$2 ; echo "set network"; shift 2;;
        -t)
            toop_dir=$2 ; shift 2;;
        -s)
            smp_container_name_to_link=$2; shift 2;;
        -m)
            smp_network_name=$2; shift 2;;
        -c) 
            container_name=$2 ; shift 2;;
        -p) 
            PORT_OPT="-p $2:8080" ; shift 2;;
        -h) 
            echo "Usage: ./run.sh [-t toop_dir] [-n network] [-c container_name] [-s smp_container] [-m smp_network_name] [-p externalport] [-h (for help)]"; exit 0;;
        --) shift; break;;
    esac
done



if [[ $network != "" ]]
then
   NETWORK_OPT="--network=$network"
   docker network create $network
fi

if [[ $toop_dir != "" ]]
then
   toop_dir="$(pwd)/$toop_dir"
fi

if [[ $container_name != "" ]]
then
   CONTAINER_NAME_OPT="--name=$container_name"
   NET_ALIAS_OPT="--net-alias $container_name"
fi


if [[ $smp_container_name_to_link != "" ]]
then
   SMP_CONTAINER_NAME_OPT="--link $smp_container_name_to_link"
fi



echo "toop_dir                : $toop_dir"
echo "NETWORK_OPT             : $NETWORK_OPT"
echo "CONTAINER_NAME_OPT      : $CONTAINER_NAME_OPT"
echo "SMP_CONTAINER_NAME_OPT  : $SMP_CONTAINER_NAME_OPT"
echo "NET_ALIAS_OPT           : $NET_ALIAS_OPT"
echo "PORT_OPT                : $PORT_OPT"

echo "   ./run.sh $jdk_volume $toop_dir $external_port"
    
    
containerId=`docker run -d \
           $CONTAINER_NAME_OPT \
           $SMP_CONTAINER_NAME_OPT \
           $NETWORK_OPT \
           $NET_ALIAS_OPT \
           $PORT_OPT \
           -v $toop_dir:/toop-dir \
           toop/toop-connector-webapp:0.10.4`


echo "Container ID is $containerId"

if [[ $containerId != "" && $smp_network_name != "" ]]
then
   echo "Connect to SMP $smp_network_name "
   docker network connect $smp_network_name $containerId
fi
