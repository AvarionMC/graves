package org.avarion.graves.util;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version implements Comparable<Version> {
    public final int major;
    public final int minor;
    public final int patch;

    private final static Pattern versionPattern = Pattern.compile("^(?:v|version|ver|\\s)*(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");

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

    @Override
    public int compareTo(Version other) {
        if (this.major != other.major) {
            return Integer.compare(this.major, other.major);
        }
        if (this.minor != other.minor) {
            return Integer.compare(this.minor, other.minor);
        }
        return Integer.compare(this.patch, other.patch);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Version other = (Version) obj;
        return major == other.major && minor == other.minor && patch == other.patch;
    }

    @Override
    public int hashCode() {
        int result = major;
        result = 31 * result + minor;
        result = 31 * result + patch;
        return result;
    }

    @Override
    public @NotNull String toString() {
        return major + "." + minor + "." + patch;
    }
}
