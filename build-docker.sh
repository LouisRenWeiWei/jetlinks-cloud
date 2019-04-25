#!/usr/bin/env bash

servers="$1"
repo="$2"

if [ ! -n "$servers" ];then
echo "未指定构建的服务,可执行 ./build-docker.sh all 构建全部镜像"
exit 0;
fi
if [ ! -n "repo" ];then
repo=jetlinks
fi
if [ "$servers" = "all" ];then
servers=device-gateway-service\
,rule-engine-service\
,log-service\
,dashboard-gateway-service
fi

echo "构建服务:$servers"
echo "docker仓库:$repo"

./mvnw -pl common-components\
,common-components/influx-component\
,common-components/logging-component\
,common-components/redis-component\
,common-components/service-dependencies\
,common-components/rule-engine-component \
-am clean install -DskipTests

./mvnw -Dgit-commit-id=$(git rev-parse HEAD) -pl "$servers" clean package docker:removeImage docker:build -DskipTests -Ddocker.image.baseName="$repo"
