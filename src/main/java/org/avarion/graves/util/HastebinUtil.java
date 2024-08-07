package org.avarion.graves.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class HastebinUtil {

    private HastebinUtil() {
        // Don't do anything here
    }

    public static @Nullable String postDataToHastebin(@NotNull String data, boolean raw) {
        String urlString = "https://www.toptal.com/developers/hastebin/documents/";
        String pasteRawURLString = "https://www.toptal.com/developers/hastebin/raw/";
        String pasteURLString = "https://www.toptal.com/developers/hastebin/";

        try {
            URL url = URI.create(urlString).toURL();
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();

            httpsURLConnection.setDoOutput(true);
            httpsURLConnection.setUseCaches(false);
            httpsURLConnection.setRequestMethod("POST");

            DataOutputStream dataOutputStream = new DataOutputStream(httpsURLConnection.getOutputStream());

            dataOutputStream.write(data.getBytes(StandardCharsets.UTF_8));

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpsURLConnection.getInputStream()));
            String response = bufferedReader.readLine();

            if (response.contains("\"key\"")) {
                response = response.substring(response.indexOf(":") + 2, response.length() - 2);
                response = ((raw ? pasteRawURLString : pasteURLString) + response);
            }

            return !response.equals(urlString) ? response : null;
        }
        catch (IOException exception) {
            exception.printStackTrace();
        }

        return null;
    }

}
