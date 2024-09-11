package org.avarion.graves.util;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version {
    public final int major;
    public final int minor;
    public final int patch;

    private static final Pattern versionPattern = Pattern.compile("^\\s*(?:version|ver|v)?\\s*(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");

    public Version(@Nullable String version) {
        Matcher matcher = versionPattern.matcher(version == null ? "" : version);

        if (matcher.find()) {
            this.major = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) : 0;
            this.minor = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
            this.patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
        }
        else {
            this.major = 0;
            this.minor = 0;
            this.patch = 0;
        }
    }

    /**
     * Is `other` greater than me?
     * ==> other > this?
     */
    public boolean isOutdated(Version other) {
        if (other == null) {
            return false;
        }

        if (other.major > this.major) {
            return true;
        }
        if (other.major < this.major) {
            return false;
        }

        // Major versions are equal, check minor
        if (other.minor > this.minor) {
            return true;
        }
        if (other.minor < this.minor) {
            return false;
        }

        // Major and minor versions are equal, check patch
        return other.patch > this.patch;
    }

    @Override
    public @NotNull String toString() {
        return major + "." + minor + "." + patch;
    }
}
