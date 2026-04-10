# ─────────────────────────────────────────────────────────────
# Stage 1 – build
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /workspace

# Copy Maven wrapper + pom first so dependency layer is cached
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q

# Now copy source and build (skip tests – run them in CI, not in image build)
COPY src/ src/
RUN ./mvnw package -DskipTests -q

# ─────────────────────────────────────────────────────────────
# Stage 2 – runtime
# Uses JRE-only image to minimise attack surface
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine

# Non-root user – required for OpenShift's default restricted SCC
# (OpenShift drops any image that runs as uid 0)
RUN addgroup -S sds && adduser -S sds -G sds

# Create upload directory and own it by the app user.
# In production this is typically a mounted PVC, but the directory
# must exist and be writable by uid sds.
RUN mkdir -p /var/sds/uploads && chown sds:sds /var/sds/uploads

USER sds

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

# Prefer IPv4; disable default attach agent (not needed, reduces surface)
ENTRYPOINT ["java", \
  "-Djava.net.preferIPv4Stack=true", \
  "-Dspring.profiles.active=prod", \
  "-jar", "app.jar"]

# Document the ports – actual exposure is defined in the OpenShift Service
EXPOSE 8080
