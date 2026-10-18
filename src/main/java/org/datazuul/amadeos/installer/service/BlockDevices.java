package org.datazuul.amadeos.installer.service;

import org.datazuul.amadeos.installer.domain.BlockDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The disks and partitions of the computer, from {@code lsblk}, and which of them amadeOS can go onto. */
public class BlockDevices {
    private static final Logger LOG = LoggerFactory.getLogger(BlockDevices.class);
    /** The image needs about 4 GB; smaller disks are not offered. */
    public static final long MIN_DISK_BYTES = 8L * 1000 * 1000 * 1000;
    static final String[] COLUMNS = {"NAME", "PATH", "SIZE", "MODEL", "TYPE", "RM", "RO", "MOUNTPOINTS", "PKNAME",
            "FSTYPE", "TRAN", "PARTTYPE", "LABEL"};
    private static final Pattern PAIR = Pattern.compile("(\\w+)=\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern HEX_ESCAPE = Pattern.compile("\\\\x([0-9a-fA-F]{2})");

    /** All block devices, disks before their partitions. */
    public List<BlockDevice> list() throws IOException {
        return parse(run("lsblk", "--bytes", "--pairs", "--output", String.join(",", COLUMNS)));
    }

    /** The disk the live system runs from (USB stick, DVD), which must never be a target. */
    public Optional<String> liveMediumDisk(List<BlockDevice> devices) {
        try {
            String source = run("findmnt", "--noheadings", "--output", "SOURCE", "/run/live/medium").strip();
            int bracket = source.indexOf('[');
            if (bracket >= 0) source = source.substring(0, bracket);
            if (source.isEmpty()) return Optional.empty();
            return Optional.of(diskOf(source, devices));
        } catch (IOException e) {
            LOG.debug("No live medium found", e);
            return Optional.empty();
        }
    }

    /** The path of the disk the device (a partition, or a disk already) is on. */
    static String diskOf(String path, List<BlockDevice> devices) {
        Map<String, BlockDevice> byName = new HashMap<>();
        for (BlockDevice device : devices) byName.put(device.name(), device);
        BlockDevice current = devices.stream().filter(d -> d.path().equals(path)).findFirst().orElse(null);
        while (current != null && !current.parentName().isEmpty() && byName.containsKey(current.parentName())) {
            current = byName.get(current.parentName());
        }
        return current == null ? path : current.path();
    }

    /**
     * The disks amadeOS can be installed on: real disks (no loop, zram or optical drive), large enough,
     * writable, not the live medium and with nothing mounted on them or their partitions.
     */
    public List<BlockDevice> installableDisks(List<BlockDevice> devices, Optional<String> liveDisk) {
        List<BlockDevice> disks = new ArrayList<>();
        for (BlockDevice device : devices) {
            if (!device.isDisk() || device.readOnly() || device.size() < MIN_DISK_BYTES) continue;
            if (device.name().startsWith("zram") || device.name().startsWith("loop")) continue;
            if (liveDisk.isPresent() && liveDisk.get().equals(device.path())) continue;
            boolean mounted = device.isMounted()
                    || devices.stream().anyMatch(d -> d.isChildOf(device) && d.isMounted());
            if (mounted) continue;
            disks.add(device);
        }
        return disks;
    }

    /** The partitions of the disk that are not mounted, in order. */
    public List<BlockDevice> partitionsOf(BlockDevice disk, List<BlockDevice> devices) {
        List<BlockDevice> partitions = new ArrayList<>();
        for (BlockDevice device : devices) {
            if (device.isPartition() && device.isChildOf(disk) && !device.isMounted()) partitions.add(device);
        }
        return partitions;
    }

    /** Parses the {@code --pairs} output of lsblk, one device per line. */
    static List<BlockDevice> parse(String output) {
        List<BlockDevice> devices = new ArrayList<>();
        for (String line : output.split("\n")) {
            if (line.isBlank()) continue;
            Map<String, String> values = new HashMap<>();
            Matcher matcher = PAIR.matcher(line);
            while (matcher.find()) values.put(matcher.group(1), unescape(matcher.group(2)));
            if (!values.containsKey("NAME")) continue;
            String name = values.get("NAME");
            String path = values.getOrDefault("PATH", "/dev/" + name);
            List<String> mountPoints = Arrays.stream(values.getOrDefault("MOUNTPOINTS", "").split("\n"))
                    .map(String::strip).filter(s -> !s.isEmpty()).toList();
            devices.add(new BlockDevice(name, path, parseLong(values.get("SIZE")), values.getOrDefault("MODEL", ""),
                    values.getOrDefault("TYPE", ""), "1".equals(values.get("RM")), "1".equals(values.get("RO")),
                    mountPoints, values.getOrDefault("PKNAME", ""), values.getOrDefault("FSTYPE", ""),
                    values.getOrDefault("TRAN", ""), values.getOrDefault("PARTTYPE", "").toLowerCase(Locale.ROOT),
                    values.getOrDefault("LABEL", "")));
        }
        return devices;
    }

    private static long parseLong(String value) {
        try {
            return value == null || value.isBlank() ? 0 : Long.parseLong(value.strip());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** lsblk writes special characters as {@code \x20}-style escapes; new lines separate several mount points. */
    static String unescape(String value) {
        Matcher matcher = HEX_ESCAPE.matcher(value);
        StringBuilder plain = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(plain, Matcher.quoteReplacement(
                    String.valueOf((char) Integer.parseInt(matcher.group(1), 16))));
        }
        matcher.appendTail(plain);
        return plain.toString().replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String run(String... command) throws IOException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output;
        try (var in = process.getInputStream()) {
            output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        try {
            int exit = process.waitFor();
            if (exit != 0) throw new IOException(command[0] + " failed (" + exit + "): " + output.strip());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(command[0] + " interrupted", e);
        }
        return output;
    }
}
