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

RUN bash -c ' apt-get --quiet update --yes && apt-get --quiet install --yes wget tar unzip lib32stdc++6 lib32z1 && export ANDROID_HOME="${PWD}/android-home" && install -d $ANDROID_HOME && wget --output-document=$ANDROID_HOME/cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_SDK_TOOLS}_latest.zip && pushd $ANDROID_HOME && unzip -d cmdline-tools cmdline-tools.zip && pushd cmdline-tools && mv cmdline-tools tools || true && popd && popd && export PATH=$PATH:${ANDROID_HOME}/cmdline-tools/tools/bin/ && sdkmanager --version && yes | sdkmanager --licenses || true && sdkmanager "platforms;android-${ANDROID_COMPILE_SDK}" && sdkmanager "platform-tools" && sdkmanager "build-tools;${ANDROID_BUILD_TOOLS}"'



