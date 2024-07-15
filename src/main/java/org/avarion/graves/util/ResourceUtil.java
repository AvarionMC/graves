package org.avarion.graves.util;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class ResourceUtil {

    private ResourceUtil() {
        // Don't do anything here
    }

    public static void copyResources(String inputPath, String outputPath, JavaPlugin plugin) {
        copyResources(inputPath, outputPath, true, plugin);
    }

    public static void copyResources(String inputPath, String outputPath, boolean overwrite, JavaPlugin plugin) {
        inputPath = formatString(inputPath);
        outputPath = formatString(outputPath);

        saveResources(getResources(inputPath, plugin), inputPath, outputPath, overwrite);
    }

    private static @NotNull Map<String, InputStream> getResources(@NotNull String path, @NotNull JavaPlugin plugin) {
        Map<String, InputStream> inputStreamHashMap = new HashMap<>();
        URL url = plugin.getClass().getClassLoader().getResource(path);

        if (url == null) {
            return inputStreamHashMap;
        }

        if (url.getProtocol().equals("file")) {
            getResourcesAsFile(path, url, inputStreamHashMap);
        }
        else {
            getResourcesAsJar(path, plugin, url, inputStreamHashMap);
        }

        return inputStreamHashMap;
    }

    private static void getResourcesAsJar(@NotNull String path, @NotNull JavaPlugin plugin, @NotNull URL url, @NotNull Map<String, InputStream> inputStreamHashMap) {
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
        }
        catch (IOException ignored) {
        }
    }

    private static void getResourcesAsFile(@NotNull String path, @NotNull URL url, @NotNull Map<String, InputStream> inputStreamHashMap) {
        File dir;
        try {
            dir = new File(url.toURI());
        }
        catch (URISyntaxException ignored) {
            return;
        }

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                String relative = file.getParentFile().getName() + File.separator + file.getName();
                if (!file.isDirectory() && relative.startsWith(path)) {
                    try {
                        inputStreamHashMap.put(relative, new FileInputStream(file));
                    }
                    catch (FileNotFoundException ignored) {
                    }
                }
            }
        }
    }

    private static void saveResources(@NotNull Map<String, InputStream> inputStreamMap, String inputPath, String outputPath, boolean overwrite) {
        for (Map.Entry<String, InputStream> entry : inputStreamMap.entrySet()) {
            String path = entry.getKey();
            InputStream inputStream = entry.getValue();
            File outputFile = new File(outputPath + File.separator + path.replaceFirst(inputPath, ""));

            if ((!outputFile.exists() || overwrite) && createDirectories(outputFile)) {
                try (OutputStream outputStream = Files.newOutputStream(outputFile.toPath())) {
                    byte[] bytes = new byte[1024];
                    int len;

                    while ((len = entry.getValue().read(bytes)) > 0) {
                        outputStream.write(bytes, 0, len);
                    }

                    inputStream.close();
                }
                catch (IOException ignored) {
                }
            }
        }
    }

}

    private static boolean createDirectories(@NotNull File file) {
        File parentFile = file.getParentFile();

        return parentFile != null && (parentFile.exists() || parentFile.mkdirs());
    }

    @Contract(pure = true)
    private static @NotNull String formatString(@NotNull String string) {
        return string.replace("/", File.separator);
    }
}
