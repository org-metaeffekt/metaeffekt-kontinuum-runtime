FROM maven:3.9.11-amazoncorretto-17-debian

RUN apt-get update && \
    apt-get install -y fontconfig libfreetype6 rsync && \
    rm -rf /var/lib/apt/lists/*

COPY metaeffekt-kontinuum /usr/src/metaeffekt-kontinuum

COPY local-maven-repo/org/metaeffekt /root/.m2/repository/org/metaeffekt
COPY local-maven-repo/com/metaeffekt /root/.m2/repository/com/metaeffekt
COPY local-maven-repo/org/jetbrains/kotlin /root/.m2/repository/org/jetbrains/kotlin

COPY local-maven-repo/com/github/luben/zstd-jni /root/.m2/repository/com/github/luben/zstd-jni
COPY local-maven-repo/com/google/api/grpc/proto-google-common-protos /root/.m2/repository/com/google/api/grpc/proto-google-common-protos
COPY local-maven-repo/com/google/guava/guava /root/.m2/repository/com/google/guava/guava
COPY local-maven-repo/com/ibm/icu/icu4j /root/.m2/repository/com/ibm/icu/icu4j
COPY local-maven-repo/io/fabric8/knative-model /root/.m2/repository/io/fabric8/knative-model
COPY local-maven-repo/io/fabric8/kubernetes-model-core /root/.m2/repository/io/fabric8/kubernetes-model-core
COPY local-maven-repo/org/apache/lucene/lucene-core /root/.m2/repository/org/apache/lucene/lucene-core
COPY local-maven-repo/org/apache/poi /root/.m2/repository/org/apache/poi
COPY local-maven-repo/org/bouncycastle/bcprov-jdk18on /root/.m2/repository/org/bouncycastle/bcprov-jdk18on
COPY local-maven-repo/org/scala-lang/scala-library /root/.m2/repository/org/scala-lang/scala-library
COPY local-maven-repo/org/spdx/java-spdx-library /root/.m2/repository/org/spdx/java-spdx-library
COPY local-maven-repo/org/xerial/sqlite-jdbc /root/.m2/repository/org/xerial/sqlite-jdbc
COPY local-maven-repo/xalan/xalan /root/.m2/repository/xalan/xalan