#!/bin/bash

baseDir=$(dirname $0)

dbUrl="$(aws rds describe-db-instances --db-instance-identifier burgers-db | jq -r '.DBInstances[0].Endpoint.Address')"

if [ "$dbUrl" == "" ]
then
  echo "The application must be deployed after the database"
  exit 1
fi

aws eks update-kubeconfig --name app-cluster
if [ $? -ne 0 ]
then
  echo "Failed to get kubeconfig configuration. Aborting"
  exit 1
fi

cat ${baseDir}/db/db-configs-cloud.yml | sed "s/__DB_URL__/$dbUrl/" | kubectl apply -f -
if [ $? -ne 0 ]
then
  echo "Failed to create db-configs-cloud"
  exit 1
fi

kubectl apply -f ${baseDir}/app/app-service-loadbalancer.yml
if [ $? -ne 0 ]
then
  echo "Failed to create app-service-loadbalancer"
  exit 1
fi

# TODO - Resolver esse problema de sincronização entre os passos...
# Antes do comando abaixo: Preencher nosso endpoint do API Gateway (kubectl get svc -> External IP)
# Webhook de Pagamento nao funcionará no estado atual!
kubectl apply -f ${baseDir}/app/pagamento-configs.yml
if [ $? -ne 0 ]
then
  echo "Failed to create pagamento-configs"
  exit 1
fi

# TODO Automatizar conteudo do arquivo aws-configs
kubectl apply -f ${baseDir}/app/aws-configs.yml
if [ $? -ne 0 ]
then
  echo "Failed to create aws-configs"
  exit 1
fi

kubectl apply -f ${baseDir}/app/app-deployment.yml
if [ $? -ne 0 ]
then
  echo "Failed to create app-deployment"
  exit 1
fi

kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
if [ $? -ne 0 ]
then
  echo "Failed to create Metrics Server"
  exit 1
fi

kubectl apply -f ${baseDir}/app/app-hpa.yml
if [ $? -ne 0 ]
then
  echo "Failed to create HPA"
  exit 1
fi

