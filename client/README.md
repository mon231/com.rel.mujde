# mujde
[LSPosed](https://github.com/LSPosed/LSPosed) (over [Magisk](https://github.com/topjohnwu/Magisk)) module to inject [frida](https://github.com/frida/frida) scripts to android applications

## Installation + Usage
1. Install [magisk + zygisk](https://www.youtube.com/watch?v=mMQ5DcsQqPs), and [LSPosed-framework module](https://github.com/LSPosed/LSPosed/releases/latest) <br />
1. Download [latest-release](https://github.com/mon231/mujde/releases/latest)'s `.apk` and install it
1. Enable the module in LSPosed configuration, and allow it access each app you'd like to inject frida-scripts into
1. Compile your frida scripts (using [frida-compile](https://github.com/frida/frida-compile)) and add them to `mujde`'s config-list, to match script with injected app

## How does it work?
NOT IMPLEMENTED YET..

### Planning
We'll have a main activity that provide the list of intercepted application. <br />
Each intercepted application will have it's own folder with it's own list of frida-scripts, <br />
And it's own frida-config file, with a unique symlink to instance of frida-gadget native on the disk.

## Notes and references
1. [Xposed API](https://api.xposed.info/)
1. [Frida API](https://frida.re/docs/javascript-api/)
