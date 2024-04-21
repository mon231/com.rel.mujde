# mujde
Magisk module to inject frida scripts to applications

## Usage

### Installation
Simply download [latest-release](https://github.com/mon231/mujde/releases/latest) module file (.zip) targeted to your phone's abi (usually arm64-v8a) <br />
Place the downloaded zip in your phone, then use `Magisk->Modules->Install from storage`, provide the chosen path, and wait a few seconds for installation-success message!

### Injections
The module follows a folder tree (at `/data/data/rel.mujde`), where each targeted package will have it's own folder, containing list of scripts to inject whenever the processes starts! <br />
All you have to do is to create a folder, named after the package you want to inject the scripts into, then upload your scripts into that folder! <br />
```
- /data/data/rel.mujde
  - com.example
  | - first_script.js
  | - example_file.js

  - com.yourapp
  | - myscript.js
```

### Notes
It's highly recommended (and sometimes even required) to reboot your device after modules installation <br />
To get your phone's abi:
> adb shell getprop ro.product.cpu.abi

## How does it work?
First of all, we'll make sure that [frida-server](https://github.com/frida/frida) is always running, re-executing server's loop when it crushes <br />
Secondly, whenever an android app starts, we'll check if we have any script to inject to it. <br />
We'll use kernel-level hooking to catch the process creation before it executes, so injection will take action as soon as possible.

## Credits
[Magisk](https://github.com/topjohnwu/Magisk) <br />
[Frida](https://github.com/frida/frida)

```
POST / HTTP/1.1
Host: localhost:27042
Accept-Encoding: gzip, deflate, br
Accept: */*
Connection: keep-alive
Content-Length: 93
Content-Type: application/json

{
  "type": "spawn",
  "name": "il.idf.idfcertwallet",
  "script": "console.log('hillow');"
}
```
