FROM navikt/common:0.1 AS navikt-common
FROM navikt/java:11

COPY --from=navikt-common /init-scripts /init-scripts
COPY --from=navikt-common /entrypoint.sh /entrypoint.sh
COPY --from=navikt-common /dumb-init /dumb-init
COPY build/libs/*.jar app.jar
COPY /scripts/init/service-user.sh /init-scripts/service-user.sh
RUN chmod +x /init-scripts/service-user.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/dumb-init", "--", "/entrypoint.sh"]