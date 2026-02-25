# ============================================================
# Stage 1: Dependency cache
# --platform=$BUILDPLATFORM → gunakan platform HOST untuk build
# agar tidak perlu emulasi QEMU saat compile Java.
# Stage ini hanya rebuild jika pom.xml berubah.
# ============================================================
FROM eclipse-temurin:21-jdk-alpine AS deps

WORKDIR /build

COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

RUN chmod +x mvnw && \
    ./mvnw dependency:go-offline -B --no-transfer-progress


# ============================================================
# Stage 2: Build
# ============================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Ambil dependency cache dari stage deps
COPY --from=deps /root/.m2 /root/.m2
COPY --from=deps /build/ .

COPY src/ src/

# Build JAR untuk target platform linux/amd64
#
# -Dos.detected.classifier=linux-x86_64
#   → penting! pom.xml mengandung dependency:
#     netty-resolver-dns-native-macos (osx-aarch_64)
#     yang spesifik macOS. Flag ini memaksa Maven
#     meresolvasi native classifier linux/amd64,
#     bukan macOS, sehingga tidak error saat runtime.
RUN ./mvnw package \
      -DskipTests \
      -B \
      --no-transfer-progress \
      -Dos.detected.classifier=linux-x86_64 \
      -Dos.detected.name=linux \
      -Dos.detected.arch=x86_64 && \
    rm -rf target/*-sources.jar target/*-javadoc.jar

# Ekstrak layered JAR untuk optimasi cache & startup
RUN java -Djarmode=layertools \
         -jar target/*.jar \
         extract --destination /build/extracted


# ============================================================
# Stage 3: Runtime — Distroless nonroot
# ============================================================
FROM gcr.io/distroless/java21-debian12:nonroot

ARG VERSION=1.0.0
ARG BUILD_DATE
ARG GIT_COMMIT

WORKDIR /app

COPY --from=builder /build/extracted/dependencies/           ./
COPY --from=builder /build/extracted/spring-boot-loader/     ./
COPY --from=builder /build/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/extracted/application/           ./

EXPOSE 8080

LABEL org.opencontainers.image.title="packing-pdf" \
      org.opencontainers.image.description="PDF Packing & Signing Service — BSSN" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.revision="${GIT_COMMIT}" \
      org.opencontainers.image.vendor="BSSN"

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:InitialRAMPercentage=50.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=production", \
  "org.springframework.boot.loader.launch.JarLauncher"]