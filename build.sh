#!/bin/bash

#发包命令 ./build.sh.sh dev|test
#106.75.110.211 dev

PROJECT_NAME="gateway"
FOLDER_PATH="aha-gateway-server"
tar_file="aha-${PROJECT_NAME}-server.jar"

gradle clean build

echo $"copy ${tar_file} to [$1] environment ..."
	scp ./build/libs/${tar_file} hjmrunning@$1:/opt/hjm/${FOLDER_PATH}
	scp ./bash/deploy.sh ./bash/run.sh hjmrunning@$1:/opt/hjm/${FOLDER_PATH}
	ssh -t -p 22 hjmrunning@$1 "/opt/hjm/${FOLDER_PATH}/deploy.sh "$1
