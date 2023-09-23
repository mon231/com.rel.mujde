#include <linux/kernel.h>
#include <linux/module.h>
#include <linux/kprobes.h>

static struct kprobe kp;

// Your pre_handler function to intercept process creation
static int handler_pre(struct kprobe *p, struct pt_regs *regs) {
    // Implement your logic here to intercept the process creation
    // You can inspect 'regs' to get information about the process
    return 0;
}

static int __init my_module_init(void) {
    kp.pre_handler = handler_pre;
    kp.symbol_name = "do_execve"; // Function responsible for process execution

    if (register_kprobe(&kp) < 0) {
        pr_err("Failed to register kprobe\n");
        return -1;
    }
    pr_info("Kprobe registered successfully\n");
    return 0;
}

static void __exit my_module_exit(void) {
    unregister_kprobe(&kp);
    pr_info("Kprobe unregistered\n");
}

module_init(my_module_init);
module_exit(my_module_exit);
MODULE_LICENSE("GPL");
