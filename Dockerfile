FROM ubuntu:latest

RUN apt update && apt upgrade -y
RUN apt install -y cmake ninja-build clang python3
RUN apt install -y wget unzip curl git

RUN wget https://dl.google.com/android/repository/android-ndk-r26-linux.zip -O ndk.zip
RUN unzip ndk.zip -d /opt/ndk
RUN rm ndk.zip ~/.gitconfig
