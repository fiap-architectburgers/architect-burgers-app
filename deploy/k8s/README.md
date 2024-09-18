
## Local Deployment (Minikube)

Preparação minikube (1 vez apenas)

    # Criar diretório do volume local
	minikube ssh -- sudo mkdir -p /data/pg-data-volume

    # Habilitar addon metrics server
    minikube addons enable metrics-server

Deploy:

    kubectl apply -f ./db/db-configs.yml
    kubectl apply -f ./db/data-volume.yml
    kubectl apply -f ./db/data-volume-claim.yml
    kubectl apply -f ./db/db-deployment.yml
    kubectl apply -f ./db/db-service.yml
    kubectl apply -f ./app/pagamento-configs.yml
    kubectl apply -f ./app/app-deployment.yml
    kubectl apply -f ./app/app-service-localcluster.yml
    kubectl apply -f ./app/app-hpa.yml

## Cloud Deployment

Criar a infraestrutura com os dois passos abaixo:
- Pré-requisito: Infra contida no projeto infra-base (VPC + Banco de Dados)
- Executar o setup na pasta terraform

Iniciar os elementos no cluster Kubernetes:

    ./deploy-aws.sh

