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

    private static final int CONNECT_TIMEOUT_MILLIS = 5000;
    private static final int READ_TIMEOUT_MILLIS = 10000;

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

            httpsURLConnection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            httpsURLConnection.setReadTimeout(READ_TIMEOUT_MILLIS);
            httpsURLConnection.setDoOutput(true);
            httpsURLConnection.setUseCaches(false);
            httpsURLConnection.setRequestMethod("POST");

            try (DataOutputStream dataOutputStream = new DataOutputStream(httpsURLConnection.getOutputStream())) {
                dataOutputStream.write(data.getBytes(StandardCharsets.UTF_8));
            }

            String response;

            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpsURLConnection.getInputStream(),
                                                                                         StandardCharsets.UTF_8))) {
                response = bufferedReader.readLine();
            }

            if (response == null) {
                return null;
            }

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
