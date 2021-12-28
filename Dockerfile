FROM openjdk:11

WORKDIR /app
COPY target/echoserver-0.1.0-SNAPSHOT-standalone.jar echoserver.jar

# Default startup command line
CMD ["java", "-jar", "/app/echoserver.jar", "--host", "0.0.0.0", "--port", "8080"]