# https://github.com/mingchen/docker-android-build-box
FROM mingc/android-build-box:1.24.0

RUN rm /etc/apt/sources.list.d/*

RUN apt-get update && \
    apt-get install -y jq && \
    update-alternatives --set java /usr/lib/jvm/java-11-openjdk-arm64/bin/java
