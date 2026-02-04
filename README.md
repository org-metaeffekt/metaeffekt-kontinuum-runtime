# metaeffekt-kontinuum-runtime
Defines a docker container for use with metaeffekt-kontinuum.

## Container Information

The configuration of the container built via the github workflow in this repository is described in the
corresponding [Dockerfile](container/Dockerfile). A breakdown of the container contents is as follows:

- **Base image:** 
  - maven:3.9.9-amazoncorretto-8-debian-bookworm
- **Instance of the metaeffekt-kontinuuum repository:**
  - /usr/src/metaeffekt-kontinuum (set as working directory)
- **Metaeffekt maven artifacts required by metaeffekt-kontinuum**
  - /root/.m2/repository/com/metaeffekt
  - /root/.m2/repository/org/metaeffekt

## Publishing

Copy all required directories from your local maven repository into metaeffekt-kontinuum-runtime/local-maven-repo/. The
list of the required subdirectories can be found in the [Dockerfile](container/Dockerfile). Clone the desired release of the
[metaeffekt-kontinuum](https://github.com/org-metaeffekt/metaeffekt-kontinuum) into metaeffekt-kontinuum-runtime/metaeffekt-kontinuum.

   ```bash
    cd [...]/metaeffekt-kontinuuum-runtime
    docker build -f container/Dockerfile .
    docker tag IMAGE_ID metaeffekt/metaeffekt-kontinuuum-runtime:VERSION
    docker push metaeffekt/metaeffekt-kontinuum-runtime:VERSION
   ```