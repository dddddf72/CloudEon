#!/bin/bash

source  /opt/edp/${service.serviceName}/conf/install-iceberg.sh

<#assign ramPercentage = conf['flink.hs.jvm.memory.percentage']?number>

export FLINK_HS_HEAP=$[ $MEM_LIMIT / 1024 / 1024  * ${ramPercentage} / 100 ]M
export FLINK_ENV_JAVA_OPTS_HS="-Xmx$FLINK_HS_HEAP -Xms$FLINK_HS_HEAP $FLINK_ENV_JAVA_OPTS_HS"

export FLINK_ENV_JAVA_OPTS_HS="$FLINK_ENV_JAVA_OPTS_HS  -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=9925 -javaagent:/opt/jmx_exporter/jmx_prometheus_javaagent-0.14.0.jar=5555:/opt/edp/${service.serviceName}/conf/jmx_prometheus.yaml"

${r"${FLINK_HOME}"}/bin/historyserver.sh start

tail -f /dev/null