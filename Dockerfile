FROM navikt/java:17-appdynamics

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

COPY ./target/bidrag-reisekostnad-api.jar "app.jar"
