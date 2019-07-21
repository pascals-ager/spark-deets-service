#!/bin/bash

: ${HADOOP_VERSION:=2.8.4}
: ${HADOOP_PREFIX:=/opt/hadoop-$HADOOP_VERSION}
: ${HADOOP_CONF_DIR:=$HADOOP_PREFIX/etc/hadoop}

#$HADOOP_PREFIX/etc/hadoop/hadoop-env.sh


function wait_for_it()
{
    local serviceport=$1
    local service=${serviceport%%:*}
    local port=${serviceport#*:}
    local retry_seconds=5
    local max_try=100
    let i=1

    nc -z $service $port
    result=$?

    until [ $result -eq 0 ]; do
      echo "[$i/$max_try] check for ${service}:${port}..."
      echo "[$i/$max_try] ${service}:${port} is not available yet"
      if (( $i == $max_try )); then
        echo "[$i/$max_try] ${service}:${port} is still not available; giving up after ${max_try} tries. :/"
        exit 1
      fi

      echo "[$i/$max_try] try in ${retry_seconds}s once again ..."
      let "i++"
      sleep $retry_seconds

      nc -z $service $port
      result=$?
    done
    echo "[$i/$max_try] $service:${port} is available."
}

for i in ${SERVICE_PRECONDITION[@]}
do
    wait_for_it ${i}
done

# installing libraries if any - (resource urls added comma separated to the ACP system variable)
cd $HADOOP_PREFIX/share/hadoop/common ; for cp in ${ACP//,/ }; do  echo == $cp; curl -LO $cp ; done; cd -

service ssh start

$HADOOP_CONF_DIR/hadoop-env.sh

if [[ $1 = "-node-master" || $2 = "-node-master" ]]; then
  echo 'Y' | $HADOOP_PREFIX/bin/hdfs --config $HADOOP_CONF_DIR namenode -format
  $HADOOP_PREFIX/sbin/start-dfs.sh
  $HADOOP_PREFIX/sbin/start-yarn.sh
  $HADOOP_PREFIX/bin/hdfs dfs -mkdir /spark-logs
  $HADOOP_PREFIX/bin/hdfs dfs -copyFromLocal /tmp/exercise /tmp
  $SPARK_HOME/sbin/start-history-server.sh
fi

if [[ $1 = "-node-one" || $2 = "-node-one" ]]; then
  echo "starting datanode-one"
fi

if [[ $1 = "-node-two" || $2 = "-node-two" ]]; then
  echo "starting datanode-two"
fi

if [[ $1 = "-d" || $2 = "-d" ]]; then
  while true; do sleep 1000; done
fi

if [[ $1 = "-bash" || $2 = "-bash" ]]; then
  /bin/bash
fi

exec $@