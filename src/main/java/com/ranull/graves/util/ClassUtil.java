package com.ranull.graves.util;

public final class ClassUtil {
    public static void loadClass(String string) {
        try {
            Class.forName(string);
        } catch (ClassNotFoundException exception) {
            exception.printStackTrace();
        }
    }
}
