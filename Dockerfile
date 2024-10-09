FROM registry.tools.3stripes.net/eft-wm-commerce-next/maven-3.8.3-alpine_java-17:latest AS builder
WORKDIR /var/app
COPY pom.xml .
COPY src src/
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests -Dpact.verifier.publishResults=false

FROM registry.tools.3stripes.net/base-images/alpine_java-17:latest
EXPOSE 8080
WORKDIR /var/app
ADD --chown=domainname:domainname \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.23.0/opentelemetry-javaagent.jar \
    opentelemetry-javaagent.jar
COPY --chown=domainname:domainname --from=builder /var/app/target/*.jar /var/app/app.jar
EXPOSE 8080
# This ENTRYPOINT is a placeholder for local development.
# It gets overriden in the deployment. Check `deployment/kubernetes/values.yaml` for real values.
ENTRYPOINT [ \
    "sh", \
    "-c", \
    "java -Dsun.net.inetaddr.ttl=60 -Djava.security.egd=file:/dev/./urandom \
    -XX:+IgnoreUnrecognizedVMOptions \
    -XX:+PerfDisableSharedMem \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:+ExitOnOutOfMemoryError \
    -XX:+UseG1GC \
    -XX:+UnlockExperimentalVMOptions \
    -XX:+UseCGroupMemoryLimitForHeap \
    -XX:MaxRAMFraction=2 \
    -XX:+UseStringDeduplication \
    -XX:MaxDirectMemorySize=512m \
    -DETG_ENV=${ENVIRONMENT} \
    -Dspring.profiles.active=${ENVIRONMENT} \
    ${JVM_DEBUG_PORT:+-agentlib:jdwp=transport=dt_socket,address=$JVM_DEBUG_PORT,server=y,suspend=n} \
    -javaagent:opentelemetry-javaagent.jar \
    -jar /var/app/app.jar" \
    ]