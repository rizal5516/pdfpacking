# ============================================================
# Multi-platform build: linux/amd64
# Target: distroless nonroot, Java 24, Spring Boot 3.5
#
# Build command (single platform):
#   docker buildx build \
#     --platform linux/amd64 \
#     --tag packing-pdf:1.0.0 \
#     --load \
#     .
#
# Multi-arch push (amd64 + arm64):
#   docker buildx build \
#     --platform linux/amd64,linux/arm64 \
#     --tag registry.example.com/packing-pdf:1.0.0 \
#     --push \
#     .
# ============================================================


# ============================================================
# Stage 1: Dependency cache
# --platform=$BUILDPLATFORM → gunakan platform HOST untuk build
# agar tidak perlu emulasi QEMU saat compile Java.
# Stage ini hanya rebuild jika pom.xml berubah.
# ============================================================
FROM --platform=$BUILDPLATFORM eclipse-temurin:24-jdk-alpine AS deps

WORKDIR /build

COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

RUN chmod +x mvnw && \
    ./mvnw dependency:go-offline -B --no-transfer-progress


# ============================================================
# Stage 2: Build
# ============================================================
FROM --platform=$BUILDPLATFORM eclipse-temurin:24-jdk-alpine AS builder

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
# Target platform: linux/amd64
# ============================================================
FROM --platform=linux/amd64 gcr.io/distroless/java24-debian12:nonroot

ARG VERSION=1.0.0
ARG BUILD_DATE
ARG GIT_COMMIT

WORKDIR /app

# Copy layer JAR — urutan dari paling jarang ke paling sering berubah
# agar Docker layer cache maksimal saat hanya kode yang berubah
COPY --from=builder /build/extracted/dependencies/           ./
COPY --from=builder /build/extracted/spring-boot-loader/     ./
COPY --from=builder /build/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/extracted/application/           ./

EXPOSE 8080

# Label OCI standar — berguna untuk audit, governance, dan container registry
LABEL org.opencontainers.image.title="packing-pdf" \
      org.opencontainers.image.description="PDF Packing & Signing Service — BSSN" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.revision="${GIT_COMMIT}" \
      org.opencontainers.image.vendor="BSSN" \
      org.opencontainers.image.base.name="gcr.io/distroless/java24-debian12:nonroot" \
      org.opencontainers.image.architecture="amd64"

# JVM flags production:
# UseContainerSupport        → baca cgroup memory limit (bukan RAM host)
# MaxRAMPercentage=75.0      → heap max 75% dari limit container
# InitialRAMPercentage=50.0  → alokasi heap awal 50%, kurangi GC di awal startup
# ExitOnOutOfMemoryError     → crash cepat saat OOM, biarkan K8s restart
# java.security.egd          → non-blocking RNG untuk kriptografi iText7 PDF signing
# os.name / os.arch          → paksa JVM kenali platform linux/amd64 secara eksplisit
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:InitialRAMPercentage=50.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dos.name=Linux", \
  "-Dos.arch=amd64", \
  "-Dspring.profiles.active=production", \
  "org.springframework.boot.loader.launch.JarLauncher"]