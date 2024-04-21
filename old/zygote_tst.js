// run: frida -U -n zygote64 -l <script path>
console.log('im in');
Interceptor.detachAll();

// Interceptor.attach(Module.getExportByName('libc.so', 'fork'), {
//     onEnter(args) {
//         console.log('fork()');
//     },
//     onLeave(retval)
//     {
//         console.log('fork returned:', retval)
//     }
// });
// execl, execlp, execle, execv, execvp, execvpe

Interceptor.attach(Module.getExportByName('libc.so', 'open'), {
    onEnter(args) {
        console.log('open(', JSON.stringify(args), ')');
    },
    onLeave(retval)
    {
        console.log('open returned:', retval)
    }
});

Interceptor.attach(Module.getExportByName('libc.so', 'execvpe'), {
    onEnter(args) {
        console.log('execvpe(', args, ')');
    },
    onLeave(retval)
    {
        console.log('execvpe returned:', retval)
    }
});

Interceptor.attach(Module.getExportByName('libc.so', 'execvp'), {
    onEnter(args) {
        console.log('execvp(', args, ')');
    },
    onLeave(retval)
    {
        console.log('execvp returned:', retval)
    }
});

Interceptor.attach(Module.getExportByName('libc.so', 'execv'), {
    onEnter(args) {
        console.log('execv(', args, ')');
    },
    onLeave(retval)
    {
        console.log('execv returned:', retval)
    }
});

Interceptor.attach(Module.getExportByName('libc.so', 'execlp'), {
    onEnter(args) {
        console.log('execlp(', args, ')');
    },
    onLeave(retval)
    {
        console.log('execlp returned:', retval)
    }
});

Interceptor.attach(Module.getExportByName('libc.so', 'execle'), {
    onEnter(args) {
        console.log('execle(', args, ')');
    },
    onLeave(retval)
    {
        console.log('execle returned:', retval)
    }
});

Interceptor.attach(Module.getExportByName('libc.so', 'execl'), {
    onEnter(args) {
        console.log('execl(', args, ')');
    },
    onLeave(retval)
    {
        console.log('execl returned:', retval)
    }
});

Interceptor.attach(Module.getExportByName('libc.so', 'execve'), {
    onEnter(args) {
        console.log('execve(', args, ')');
    },
    onLeave(retval)
    {
        console.log('execve returned:', retval)
    }
});

// Interceptor.attach(Module.getExportByName('libc.so', 'read'), {
//     onEnter(args) {
//         console.log('read', args);
//     //   this.fileDescriptor = args[0].toInt32();
//     },
//     onLeave(retval) {
//       if (retval.toInt32() > 0) {
//         /* do something with this.fileDescriptor */
//       }
//     }
// });
