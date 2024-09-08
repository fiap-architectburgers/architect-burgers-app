![Project logo](/docs/image/logo.png?raw=true)

Lanchonete Architect Burgers
============================

Tech Challenge FIAP Pós Tech - curso **Software Architecture**

Autor: Rodrigo Gomes da Silva - RM: 353421 - Turma: 6SOAT

____________________
Architect Burgers é uma lanchonete de bairro que está se expandindo devido ao seu grande sucesso. 
Neste Tech Challenge desenvolveremos o sistema de controle de pedidos que irá garantir que a lanchonete 
possa atender seus clientes de forma eficiente.

Sistema de backend da aplicação abrangendo as seguintes APIs:
- Cadastro do Cliente
- Identiﬁcação do Cliente via CPF
- Criar, editar e remover produtos
- Buscar produtos por categoria
- Fake checkout (Pagamento “em dinheiro”, sem integração com sistema)
- Listar os pedidos
- Documentação com Swagger

-----

Comandos úteis:

Execução em ambiente de desenvolvimento, a partir do código-fonte:

> Certificar-se que o runtime e compilador do Java (executáveis java e javac) apontem para uma
> distribuição do Java 21. Ou definir a variável JAVA_HOME de acordo

```shell
mvn -e spring-boot:run
```

Empacotamento da imagem docker:

```shell
$ cd docker/
$ ./build-local.sh
# executar docker push se for uma versão release
```

Execução a partir da imagem docker (não requer instalação do Java)
```shell
$ cd docker/
$ docker compose up  [-d]
# ou docker-compose up , se na distribuição utilizada o compose estiver em executável separado 
```

### Documentos:

- [Diagramas e documentação do DDD](https://miro.com/app/board/uXjVKYofLr8=/)

- [docs/DICIONARIO.md](docs/DICIONARIO.md) - Dicionário de domínio

- [docs/ARQUITETURA-HEXAGONAL.md](docs/ARQUITETURA-HEXAGONAL.md) - Descrição da arquitetura

- [docs/image/webservices-compra.png](docs/image/webservices-compra.png) - Fluxograma com os webservices utilizados no processo de pedido e entrega

**Link da documentação Swagger -** Com a aplicação rodando, acessar:

Documentação OpenAPI pura em Json: http://localhost:8090/v3/api-docs

UI de documentação e execução de chamadas: http://localhost:8090/swagger-ui.html

Exemplos de invocação dos serviços: web-app/src/test/samples/service-samples.http (compatível com HTTP client do IntelliJ IDEA)
