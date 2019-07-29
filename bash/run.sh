#!/bin/bash

PROJECT_NAME="gateway"
DESC="aha-${PROJECT_NAME}-server"
MAIN_CLASS="aha-${PROJECT_NAME}-server.jar"
PROFILE="spring.profiles.active="$2
SKYWALKING_NAME=$2_gatewayserver


d_start() {
    echo "profile ======> ${PROFILE}"
    nohup ${JAVA_HOME}/bin/java -D${PROFILE} -Dspring.profiles.active=dev -javaagent:/Users/luweihong/Downloads/apache-skywalking-apm-bin/agent/skywalking-agent.jar=agent.service_name=${SKYWALKING_NAME} -jar -Xms256m -Xmx512m ${MAIN_CLASS}  >/dev/null 2>&1 &
}

d_stop() {
    pkill -f ${MAIN_CLASS}
    sleep 2
    if test $(pgrep -f $MAIN_CLASS | wc -l) -ne 0
    then
      pkill -9 -f $MAIN_CLASS
    fi
}

d_status(){
    if test $(pgrep -f $MAIN_CLASS | wc -l) -ne 0
    then
      printf "$DESC is running.\n"
    else
      printf "$DESC is NOT running.\n"
    fi
}

case $1 in
 start)
    printf  "Starting $DESC ..."
    d_start
    sleep 2
    printf "  ok!\n"
    ;;
 stop)
    printf  "Stopping $DESC ..."
    d_stop
    printf "  ok!\n"
    ;;
 restart)
    printf  "Restarting $DESC ...\n"
    d_stop
    sleep 1
    d_start
    sleep 1
    d_status
    ;;
  status)
    d_status
    ;;
 *)
    printf "usage:{start|stop|restart|status}\n"
    exit 1
    ;;
esac