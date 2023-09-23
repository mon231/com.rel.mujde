FROM ubuntu:latest

RUN apt update && apt upgrade -y
RUN apt install -y cmake ninja-build clang python3
RUN wget https://dl.google.com/android/repository/android-ndk-r26-linux.zip -o ndk.zip
