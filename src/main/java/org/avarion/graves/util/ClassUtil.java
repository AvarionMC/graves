package org.avarion.graves.util;

public final class ClassUtil {

    private ClassUtil() {
        // Don't do anything here
    }

    public static void loadClass(String string) {
        try {
            Class.forName(string);
        }
        catch (ClassNotFoundException exception) {
            exception.printStackTrace();
        }
    }

}
