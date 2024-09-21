#!/bin/bash

# Realiza o deploy da aplicação no cluster EKS
# Este script simula os passos realizados no workflow de CD

EKS_CLUSTER_NAME=app-cluster
DB_INSTANCE_IDENTIFIER=burgers-db
API_GATEWAY_NAME=http-api
COGNITO_USER_POOL=customer-logins
DATABASE_NAME=postgres
DATABASE_USER=burger
DB_PW=Burgers2024

APP_IMAGE=gomesrodris/architect-burgers:0.0.8

MERCADOPAGO_POS_ID=Dummy  # Set if needed
MERCADOPAGO_USER_ID=Dummy
MERCADOPAGO_ACCESS_TOKEN=DummyDummy

TMP_OUTPUTS=/tmp/deploy/tmp_outputs.vars

baseDir=$(dirname $0)

cd $baseDir || exit 1
mkdir /tmp/deploy


./utils/check-cluster-status.sh ${EKS_CLUSTER_NAME}
if [ $? -ne 0 ]
then
  echo "Error checking AWS resource. See error messages"
  exit 1
fi

./utils/get-database-url.sh ${DB_INSTANCE_IDENTIFIER} > $TMP_OUTPUTS
if [ $? -ne 0 ]
then
  echo "Error checking AWS resource. See error messages"
  exit 1
fi
source $TMP_OUTPUTS

./utils/get-external-api-info.sh ${API_GATEWAY_NAME} > $TMP_OUTPUTS
if [ $? -ne 0 ]
then
  echo "Error checking AWS resource. See error messages"
  exit 1
fi
source $TMP_OUTPUTS

./utils/get-user-pool-info.sh ${COGNITO_USER_POOL} > $TMP_OUTPUTS
if [ $? -ne 0 ]
then
  echo "Error checking AWS resource. See error messages"
  exit 1
fi
source $TMP_OUTPUTS

cat ./k8s/db/db-configs-cloud-template.yml \
   | sed "s/{DB_URL}/${DB_URL}/" \
   | sed "s/{DB_NAME}/${DATABASE_NAME}/" \
   | sed "s/{DB_USER}/${DATABASE_USER}/" \
   | sed "s/{DB_PASS}/${DB_PW}/" > /tmp/deploy/db-configs.yml

if [ $? -ne 0 ]
then
  echo "Error preparing configmap. See error messages"
  exit 1
fi

cat ./k8s/app/pagamento-configs-template.yml \
   | sed "s|{APP_URL}|${API_URL}|" \
   | sed "s/{POS_ID}/${MERCADOPAGO_POS_ID}/" \
   | sed "s/{USER_ID}/${MERCADOPAGO_USER_ID}/" \
   | sed "s/{ACCESS_TOKEN}/${MERCADOPAGO_ACCESS_TOKEN}/" > /tmp/deploy/pagamento-configs.yml

if [ $? -ne 0 ]
then
  echo "Error preparing configmap. See error messages"
  exit 1
fi

cat ./k8s/app/aws-configs-template.yml \
   | sed "s/{ACCESS_KEY_ID}/$(echo -n ${aws_access_key_id} | tr -d '\n')/" \
   | sed "s|{SECRET_ACCESS_KEY}|$(echo -n ${aws_secret_access_key} | tr -d '\n')|" \
   | sed "s|{SESSION_TOKEN}|$(echo -n ${aws_session_token} | tr -d '\n')|" \
   | sed "s/{REGION}/us-east-1/" \
   | sed "s/{USER_POOL_ID}/${USER_POOL_ID}/" \
   | sed "s/{USER_POOL_CLIENT_ID}/${USER_POOL_CLIENT_ID}/" \
   | sed "s/{USER_POOL_CLIENT_SECRET}/${USER_POOL_CLIENT_SECRET}/" > /tmp/deploy/aws-configs.yml

if [ $? -ne 0 ]
then
  echo "Error preparing configmap. See error messages"
  exit 1
fi

cat ./k8s/app/app-deployment.yml \
  | sed "s|image: .*architect-burgers.*$|image: $APP_IMAGE|" > /tmp/deploy/app-deployment.yml

aws eks update-kubeconfig --name ${EKS_CLUSTER_NAME}

kubectl apply -f /tmp/deploy/db-configs.yml
if [ $? -ne 0 ]
then
  echo "Error running kubectl step. See error messages"
  exit 1
fi

kubectl apply -f /tmp/deploy/pagamento-configs.yml
if [ $? -ne 0 ]
then
  echo "Error running kubectl step. See error messages"
  exit 1
fi

kubectl apply -f /tmp/deploy/aws-configs.yml
if [ $? -ne 0 ]
then
  echo "Error running kubectl step. See error messages"
  exit 1
fi

kubectl apply -f ./k8s/app/app-service-loadbalancer.yml
if [ $? -ne 0 ]
then
  echo "Error running kubectl step. See error messages"
  exit 1
fi

kubectl apply -f /tmp/deploy/app-deployment.yml
if [ $? -ne 0 ]
then
  echo "Error running kubectl step. See error messages"
  exit 1
fi

kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
if [ $? -ne 0 ]
then
  echo "Error running kubectl step. See error messages"
  exit 1
fi

kubectl apply -f ./k8s/app/app-hpa.yml
if [ $? -ne 0 ]
then
  echo "Error running kubectl step. See error messages"
  exit 1
fi
