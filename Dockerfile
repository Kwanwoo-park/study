# Java builds do not generate PDFs or require Node/Chromium.
FROM bellsoft/liberica-openjdk-alpine:17 AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew bootJar

# Install the renderer once; a browser starts only on a PDF cache miss.
FROM node:24-bookworm-slim
WORKDIR /app
ENV PLAYWRIGHT_BROWSERS_PATH=/opt/playwright
COPY tools/portfolio-pdf/package*.json tools/portfolio-pdf/
RUN apt-get update \
    && apt-get install -y --no-install-recommends openjdk-17-jre-headless \
    && npm ci --prefix tools/portfolio-pdf --ignore-scripts --no-audit --no-fund \
    && node tools/portfolio-pdf/node_modules/playwright/cli.js install --with-deps chromium \
    && rm -rf /var/lib/apt/lists/*
COPY tools/portfolio-pdf/generate.mjs tools/portfolio-pdf/generate.mjs
COPY --from=builder /app/build/libs/study-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
