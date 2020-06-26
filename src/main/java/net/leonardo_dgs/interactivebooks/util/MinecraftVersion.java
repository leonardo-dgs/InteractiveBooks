package net.leonardo_dgs.interactivebooks.util;

import lombok.Getter;
import org.bukkit.Bukkit;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MinecraftVersion implements Comparable<MinecraftVersion> {

    private static final Pattern VERSION_PATTERN = Pattern.compile(".*\\(.*MC.\\s*([a-zA-z0-9\\-\\.]+)\\s*\\)");
    private static final MinecraftVersion RUNNING_VERSION = parseServerVersion(Bukkit.getVersion());

    @Getter
    private int major;
    @Getter
    private int minor;
    @Getter
    private int build;

    public MinecraftVersion(int major, int minor, int build)
    {
        this.major = major;
        this.minor = minor;
        this.build = build;
    }

    @Override
    public int compareTo(MinecraftVersion other)
    {
        int majorDiff = getMajor() - other.getMajor();
        int minorDiff = getMinor() - other.getMinor();
        int buildDiff = getBuild() - other.getBuild();

        if(majorDiff != 0)
            return majorDiff > 0 ? 3 : -3;
        if(minorDiff != 0)
            return minorDiff > 0 ? 2 : -2;
        if(buildDiff != 0)
            return buildDiff > 0 ? 1 : -1;

        return 0;
    }

    public boolean isAfter(MinecraftVersion other)
    {
        return compareTo(other) > 0;
    }

    public boolean isAfterOrEqual(MinecraftVersion other)
    {
        return compareTo(other) >= 0;
    }

    public boolean isBefore(MinecraftVersion other)
    {
        return compareTo(other) < 0;
    }

    public boolean isBeforeOrEqual(MinecraftVersion other)
    {
        return compareTo(other) <= 0;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof MinecraftVersion)) return false;

        MinecraftVersion other = (MinecraftVersion) obj;
        return getMajor() == other.getMajor() &&
                getMinor() == other.getMinor() &&
                getBuild() == other.getBuild();
    }

    public static MinecraftVersion getRunningVersion()
    {
        return RUNNING_VERSION;
    }

    public static MinecraftVersion of(int major, int minor, int build)
    {
        return new MinecraftVersion(major, minor, build);
    }

    public static MinecraftVersion parse(String version)
    {
        String[] elements = version.split("\\.");
        int[] versionParts = new int[3];
        if(elements.length < 1)
            throw new IllegalArgumentException("Invalid version format: " + version);

        for (int i = 0; i < versionParts.length; i++)
            versionParts[i] = elements.length > i ? Integer.parseInt(elements[i].trim()) : 0;

        return new MinecraftVersion(versionParts[0], versionParts[1], versionParts[2]);
    }

    private static MinecraftVersion parseServerVersion(String serverVersion)
    {
        Matcher version = Objects.requireNonNull(VERSION_PATTERN).matcher(serverVersion);

        if (version.matches() && version.group(1) != null)
        {
            return MinecraftVersion.parse(version.group(1));
        }
        else
        {
            throw new IllegalStateException("Cannot parse version String '" + serverVersion + "'");
        }
    }

}
