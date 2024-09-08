##
# This version of the Dockerfile is to be integrated with the Pipeline and it depends on
# the application being pre-packaged in the ./temp_libs directory at the repository
#
# (for reference, the commands below run at the repo root will prepare the dependencies)
# mvn install dependency:copy-dependencies
# mkdir ./docker/temp_libs && cp ./modules/web-app/target/architect-burgers-webapi-*.jar ./docker/temp_libs/ && cp ./modules/web-app/target/lib/* ./docker/temp_libs/
#
#
FROM eclipse-temurin:21-jdk-alpine

RUN adduser apprunner -D

RUN mkdir /app

COPY ../temp_libs/* /app/
COPY ./entrypoint.sh /app/

RUN chown -R apprunner /app
RUN chmod a+x /app/entrypoint.sh

USER apprunner

ENV archburgers.datasource.driverClass=org.postgresql.Driver
ENV archburgers.datasource.dbUrl=jdbc:postgresql://localhost/archburgers?autoReconnect=true
ENV archburgers.datasource.dbUser=myDbUser
ENV archburgers.datasource.dbPass=muDbPass

ENV archburgers.datasource-mongodb.uri=mongodb://localhost/architectburgers
ENV archburgers.integration.mercadopago.apiBaseUrl=https://api.mercadopago.com
ENV archburgers.integration.mercadopago.userId=123456789
ENV archburgers.integration.mercadopago.accessToken=APP_USR-0000000000000000
ENV archburgers.integration.mercadopago.posId=LOJA1POS1
ENV archburgers.integration.mercadopago.notificationUrl=https://gomesrodris-mplabs.requestcatcher.com/payment-hooks/mercadopago

ENV archburgers.integration.aws.region=us-east-1
ENV archburgers.integration.cognito.userPoolId=us-east-1_0000000
ENV archburgers.integration.cognito.clientId=123456789abcdef
ENV archburgers.integration.cognito.clientSecret=xxxxxxxxxxxxxxxxxxxxxx


EXPOSE 8090

CMD ["/app/entrypoint.sh"]
