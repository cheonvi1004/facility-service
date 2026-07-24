FROM eclipse-temurin:21-jre
WORKDIR /app

COPY ./facility-service-0.0.1-SNAPSHOT.jar /app/service.jar

# 제공해주신 환경 변수 설정
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=60.0 -XX:InitialRAMPercentage=25.0 -XX:+ExitOnOutOfMemoryError"

# 컨테이너 실행 시 스크립트가 부드럽게 평가되도록 ENTRYPOINT 설정
# exec 형식을 유지하면서 JAVA_OPTS가 올바르게 파싱되도록 쉘을 경유하여 실행합니다.
ENTRYPOINT ["sh", "-c", "exec $JAVA_HOME/bin/java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/service.jar"]

