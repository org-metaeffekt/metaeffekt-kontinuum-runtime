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

## Building / Publishing

A new version of the metaeffekt-kontinuum-runtime container can be buit via the included [build-locally.sh](build-locally.sh)
script. The script will prompt for all required parameters and build a multi-architecture image locally.
After the script has successfully run, simply push the created image with the correct tag to the remote
repository on docker hub. Note: This requires the correct credentials.
