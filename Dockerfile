
FROM navikt/java:11

COPY /init-scripts/service-user.sh /init-scripts/service-user.sh
RUN chmod +x /init-scripts/service-user.sh
COPY build/libs/*.jar app.jar
