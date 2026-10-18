package org.datazuul.amadeos.installer.service;

import org.datazuul.amadeos.installer.domain.Firmware;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** What the running system tells about itself: live or installed, UEFI or BIOS, memory, root or not. */
public class SystemFacts {
    private final Path root;

    public SystemFacts() {
        this(Path.of("/"));
    }

    /** @param root the directory that stands for {@code /}; tests use a temporary one */
    SystemFacts(Path root) {
        this.root = root;
    }

    /** True when the kernel command line has {@code boot=live}, as on the live image. */
    public boolean isLiveSystem() {
        return read(root.resolve("proc/cmdline")).map(line -> (" " + line.strip() + " ").contains(" boot=live "))
                .orElse(false);
    }

    /** UEFI when the firmware interface is present under {@code /sys}. */
    public Firmware firmware() {
        return Files.isDirectory(root.resolve("sys/firmware/efi")) ? Firmware.UEFI : Firmware.BIOS;
    }

    /** {@code MemTotal} of {@code /proc/meminfo} in bytes; empty when unreadable. */
    public Optional<Long> memoryBytes() {
        return read(root.resolve("proc/meminfo")).flatMap(SystemFacts::parseMemTotal);
    }

    static Optional<Long> parseMemTotal(String meminfo) {
        for (String line : meminfo.split("\n")) {
            if (line.startsWith("MemTotal:")) {
                String[] parts = line.substring("MemTotal:".length()).strip().split("\\s+");
                try {
                    return Optional.of(Long.parseLong(parts[0]) * 1024);
                } catch (NumberFormatException e) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    /** True when the wizard itself runs as root, so amadeos-install needs no pkexec. */
    public boolean isRoot() {
        return "root".equals(System.getProperty("user.name"));
    }

    private static Optional<String> read(Path file) {
        try {
            return Optional.of(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }
}
