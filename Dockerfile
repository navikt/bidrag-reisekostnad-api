FROM ghcr.io/navikt/baseimages/temurin:21-appdynamics
LABEL maintainer="Team Bidrag" \
      email="nav.ikt.prosjekt.og.forvaltning.bidrag@nav.no"

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
COPY ./target/bidrag-reisekostnad-api.jar "app.jar"

EXPOSE 8080