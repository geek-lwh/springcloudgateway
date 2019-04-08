#!/bin/bash
export JAVA_HOME=/opt/app/jdk1.8.0_65
PROJECT_NAME="gateway"
FOLDER_PATH="aha-gateway-server"

base_dir="/opt/hjm/${FOLDER_PATH}"
tar_file="aha-${PROJECT_NAME}-server.jar"

cd ${base_dir}

ENV=$1

./run.sh stop

sleep 1

./run.sh start ${ENV}
