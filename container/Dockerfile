FROM maven:3.9.11-amazoncorretto-17-debian

RUN apt-get update && \
    apt-get install -y fontconfig libfreetype6 && \
    rm -rf /var/lib/apt/lists/*

COPY metaeffekt-kontinuum /usr/src/metaeffekt-kontinuum

COPY local-maven-repo/org/metaeffekt /root/.m2/repository/org/metaeffekt
COPY local-maven-repo/com/metaeffekt /root/.m2/repository/com/metaeffekt
COPY local-maven-repo/org/jetbrains/kotlin /root/.m2/repository/org/jetbrains/kotlin