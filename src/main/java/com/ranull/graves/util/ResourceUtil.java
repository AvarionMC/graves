package com.ranull.graves.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class ResourceUtil {
    public static void copyResources(String inputPath, String outputPath, JavaPlugin plugin) {
        copyResources(inputPath, outputPath, true, plugin);
    }

    public static void copyResources(String inputPath, String outputPath, boolean overwrite, JavaPlugin plugin) {
        inputPath = formatString(inputPath);
        outputPath = formatString(outputPath);

        saveResources(getResources(inputPath, plugin), inputPath, outputPath, overwrite);
    }

    private static Map<String, InputStream> getResources(String path, JavaPlugin plugin) {
        Map<String, InputStream> inputStreamHashMap = new HashMap<>();
        URL url = plugin.getClass().getClassLoader().getResource(path);

        if (url != null) {
            try {
                JarURLConnection connection = (JarURLConnection) url.openConnection();
                JarFile jarFile = connection.getJarFile();
                Enumeration<JarEntry> jarEntryEnumeration = jarFile.entries();

                while (jarEntryEnumeration.hasMoreElements()) {
                    JarEntry jarEntry = jarEntryEnumeration.nextElement();

                    if (!jarEntry.isDirectory() && jarEntry.getName().startsWith(path)) {
                        inputStreamHashMap.put(jarEntry.getName(), plugin.getResource(jarEntry.getName()));
                    }
                }
            } catch (IOException ignored) {
            }
        }

        return inputStreamHashMap;
    }

    private static void saveResources(Map<String, InputStream> inputStreamMap, String inputPath, String outputPath,
                                      boolean overwrite) {
        for (Map.Entry<String, InputStream> entry : inputStreamMap.entrySet()) {
            String path = entry.getKey();
            InputStream inputStream = entry.getValue();
            File outputFile = new File(outputPath + File.separator + path.replaceFirst(inputPath, ""));

            if (!outputFile.exists() || overwrite) {
                if (createDirectories(outputFile)) {
                    try {
                        OutputStream outputStream = Files.newOutputStream(outputFile.toPath());
                        byte[] bytes = new byte[1024];
                        int len;

                        while ((len = entry.getValue().read(bytes)) > 0) {
                            outputStream.write(bytes, 0, len);
                        }

                        outputStream.close();
                        inputStream.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }

    private static boolean createDirectories(File file) {
        File parentFile = file.getParentFile();

        return parentFile != null && (parentFile.exists() || parentFile.mkdirs());
    }

    private static String formatString(String string) {
        return string.replace("/", File.separator);
    }
}
