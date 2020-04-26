FROM scala-sbt-rauthprovider

COPY scala /root/app

WORKDIR /root/app

RUN cd /root/app \
    && sbt clean assembly

ENV SERVER_PORT 8080    
ENV KEYCLOAK_BASE_URL https://your.domain.ext
ENV KEYCLOAK_REALM master
ENV KEYCLOAK_CLIENT_ID xxx
ENV KEYCLOAK_CLIENT_SECRET xxx
ENV MATRIX_DOMAIN mx.domain.org

EXPOSE ${SERVER_PORT}

CMD java -jar target/scala-2.13/restauthprovider-v0.0.1.jar

