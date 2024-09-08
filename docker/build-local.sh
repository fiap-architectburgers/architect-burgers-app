#!/bin/bash
#
# Cria a imagem docker localmente usando build container

version=0.0.6

cd $(dirname $0) || exit 1

cd ..

docker build . -f docker/Dockerfile.fullBuild -t gomesrodris/architect-burgers:$version

docker push docker.io/gomesrodris/architect-burgers:$version

