# 基镜像由 docker/base-images.env 中 BASE_JAVA_IMAGE 指定（默认与官方一致；可改为内网仓库）
ARG BASE_JAVA_IMAGE=eclipse-temurin:17-jre
FROM ${BASE_JAVA_IMAGE}

ARG JAR_FILE
COPY ${JAR_FILE} /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
