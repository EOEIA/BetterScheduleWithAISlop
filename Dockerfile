# https://github.com/mingchen/docker-android-build-box
FROM openjdk:11-jdk

# ANDROID_COMPILE_SDK is the version of Android you're compiling with.
# It should match compileSdkVersion.
ENV ANDROID_COMPILE_SDK="30"

# ANDROID_BUILD_TOOLS is the version of the Android build tools you are using.
# It should match buildToolsVersion.
ENV ANDROID_BUILD_TOOLS="30.0.3"

# It's what version of the command line tools we're going to download from the official site.
# Official Site-> https://developer.android.com/studio/index.html
# There, look down below at the cli tools only, sdk tools package is of format:
#        commandlinetools-os_type-ANDROID_SDK_TOOLS_latest.zip
# when the script was last modified for latest compileSdkVersion, it was which is written down below
ENV ANDROID_SDK_TOOLS="7583922"

# Android
RUN bash -c ' \
    apt-get --quiet update --yes && \
    apt-get --quiet install --yes wget tar unzip lib32stdc++6 lib32z1 && \
  # Setup path as ANDROID_HOME for moving/exporting the downloaded sdk into it \
    export ANDROID_HOME="${PWD}/android-home" && \
  # Create a new directory at specified location \
    install -d $ANDROID_HOME && \
  # Here we are installing androidSDK tools from official source, \
  # (the key thing here is the url from where you are downloading these sdk tool for command line, so please do note this url pattern there and here as well) \
  # after that unzipping those tools and \
  # then running a series of SDK manager commands to install necessary android SDK packages that'll allow the app to build \
    wget --output-document=$ANDROID_HOME/cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_SDK_TOOLS}_latest.zip && \
  # move to the archive at ANDROID_HOME \
    pushd $ANDROID_HOME && \
    unzip -d cmdline-tools cmdline-tools.zip && \
    pushd cmdline-tools && \
  # since commandline tools version 7583922 the root folder is named "cmdline-tools" so we rename it if necessary \
    mv cmdline-tools tools || true && \
    popd && \
    popd && \
    export PATH=$PATH:${ANDROID_HOME}/cmdline-tools/tools/bin/ && \
  # Nothing fancy here, just checking sdkManager version \
    sdkmanager --version && \
  # use yes to accept all licenses \
    yes | sdkmanager --licenses || true && \
    sdkmanager "platforms;android-${ANDROID_COMPILE_SDK}" && \
    sdkmanager "platform-tools" && \
    sdkmanager "build-tools;${ANDROID_BUILD_TOOLS}" \
'

# utilities
RUN apt-get install -y jq xxd

# Fastlane
RUN bash -c '\
  apt-get install -y ruby-full build-essential g++ && \
  gem install bundler \
  '
