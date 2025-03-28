package com.rel.mujde;
import java.io.File;

public class AccessibilityUtils {
    public static void makeFileWorldReadable(File file) {
        try {
            final String MODE_RW_R_R = "644";
            Runtime.getRuntime()
            .exec("chmod " + MODE_RW_R_R + " " + file.getAbsolutePath())
            .waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void makeDirWorldReadable(File dir) {
        try {
            final String MODE_RW_R_R = "755";
            Runtime.getRuntime()
            .exec("chmod " + MODE_RW_R_R + " " + dir.getAbsolutePath())
            .waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
