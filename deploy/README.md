
## Local Deployment (Minikube)

Preparação minikube (1 vez apenas)

    # Criar diretório do volume local
	minikube ssh -- sudo mkdir -p /data/pg-data-volume

    # Habilitar addon metrics server
    minikube addons enable metrics-server

Deploy:

    kubectl apply -f ./k8s/db/db-configs.yml
    kubectl apply -f ./k8s/db/data-volume.yml
    kubectl apply -f ./k8s/db/data-volume-claim.yml
    kubectl apply -f ./k8s/db/db-deployment.yml
    kubectl apply -f ./k8s/db/db-service.yml
    kubectl apply -f ./k8s/app/pagamento-configs.yml
    kubectl apply -f ./k8s/app/app-deployment.yml
    kubectl apply -f ./k8s/app/app-service-localcluster.yml
    kubectl apply -f ./k8s/app/app-hpa.yml

## Cloud Deployment

- Pré-requisito: Infra contida nos repositórios infra-base (VPC, Banco de Dados) + infra-k8s (EKS, API-GW, Cognito) 

Iniciar os elementos no cluster Kubernetes:

    ./deploy-aws.sh

