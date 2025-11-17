FROM maven:3.9.9-amazoncorretto-8-debian-bookworm

RUN apt-get update && \
    apt-get install -y fontconfig libfreetype6 && \
    rm -rf /var/lib/apt/lists/*

COPY metaeffekt-kontinuum /usr/src/metaeffekt-kontinuum

COPY local-maven-repo/org/metaeffekt /root/.m2/repository/org/metaeffekt
COPY local-maven-repo/com/metaeffekt /root/.m2/repository/com/metaeffekt

WORKDIR /usr/src/metaeffekt-kontinuum