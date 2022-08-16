package com.ranull.graves.util;

import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public final class Base64Util {
    public static String objectToBase64(Object object) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream bukkitObjectOutputStream = new BukkitObjectOutputStream(byteArrayOutputStream);

            bukkitObjectOutputStream.writeObject(object);
            bukkitObjectOutputStream.close();

            return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
        } catch (IOException ignored) {
        }

        return null;
    }

    public static Object base64ToObject(String string) {
        try {
            return new BukkitObjectInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(string)))
                    .readObject();
        } catch (IOException | ClassNotFoundException ignored) {
        }

        return null;
    }
}
