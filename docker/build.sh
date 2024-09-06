#!/bin/bash

cd $(dirname $0) || exit 1

cd ..

# For now doing the build in a builder container. Review when integrating with CI/CD
#
#./mvnw -DskipTests install dependency:copy-dependencies
#
#cd -
#
##rm -rf temp_libs
##mkdir temp_libs
##cp ../web-app/target/architect-burgers-webapi-*.jar ./temp_libs/ || exit 1
##cp ../web-app/target/lib/* ./temp_libs/ || exit 1
#
#if [ $? -ne 0 ]
#then
#	echo "Package failed"
#	exit 1
#fi

docker build . -f docker/Dockerfile -t gomesrodris/architect-burgers:0.0.7

docker push docker.io/gomesrodris/architect-burgers:0.0.7

