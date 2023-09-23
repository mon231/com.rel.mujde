FROM 3.12.0rc3-alpine3.18

RUN pip install ipython frida-tools
RUN apt install -y cmake ninja-build clang android-ndk-r21d
