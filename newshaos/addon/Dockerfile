# use java image as that is known working
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY newshaos /app/newshaos

RUN chmod +x /app/newshaos

EXPOSE 8088

ENTRYPOINT ["/app/newshaos"]
