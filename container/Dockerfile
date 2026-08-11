FROM maven:3.9.11-amazoncorretto-17-debian

RUN apt-get update && \
    apt-get install -y fontconfig libfreetype6 git rsync unzip && \
    rm -rf /var/lib/apt/lists/*

COPY target/metaeffekt-kontinuum /usr/src/metaeffekt-kontinuum

COPY target/.m2/repository/org/metaeffekt /root/.m2/repository/org/metaeffekt
COPY target/.m2/repository/com/metaeffekt /root/.m2/repository/com/metaeffekt
COPY target/.m2/repository/org/jetbrains/kotlin /root/.m2/repository/org/jetbrains/kotlin

COPY target/.m2/repository/com/github/luben/zstd-jni /root/.m2/repository/com/github/luben/zstd-jni
COPY target/.m2/repository/com/google/api/grpc/proto-google-common-protos /root/.m2/repository/com/google/api/grpc/proto-google-common-protos
COPY target/.m2/repository/com/google/guava/guava /root/.m2/repository/com/google/guava/guava
COPY target/.m2/repository/com/ibm/icu/icu4j /root/.m2/repository/com/ibm/icu/icu4j
COPY target/.m2/repository/io/fabric8/knative-model /root/.m2/repository/io/fabric8/knative-model
COPY target/.m2/repository/io/fabric8/kubernetes-model-core /root/.m2/repository/io/fabric8/kubernetes-model-core
COPY target/.m2/repository/org/apache/lucene/lucene-core /root/.m2/repository/org/apache/lucene/lucene-core
COPY target/.m2/repository/org/apache/poi /root/.m2/repository/org/apache/poi
COPY target/.m2/repository/org/bouncycastle/bcprov-jdk18on /root/.m2/repository/org/bouncycastle/bcprov-jdk18on
COPY target/.m2/repository/org/scala-lang/scala-library /root/.m2/repository/org/scala-lang/scala-library
COPY target/.m2/repository/org/spdx/java-spdx-library /root/.m2/repository/org/spdx/java-spdx-library
COPY target/.m2/repository/org/xerial/sqlite-jdbc /root/.m2/repository/org/xerial/sqlite-jdbc
COPY target/.m2/repository/xalan/xalan /root/.m2/repository/xalan/xalan