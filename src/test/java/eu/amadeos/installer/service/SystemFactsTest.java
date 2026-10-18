package eu.amadeos.installer.service;

import eu.amadeos.installer.domain.Firmware;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemFactsTest {
    @Test
    void readsTheKernelCommandLineAndFirmware(@TempDir Path root) throws IOException {
        Files.createDirectories(root.resolve("proc"));
        Files.writeString(root.resolve("proc/cmdline"), "BOOT_IMAGE=/live/vmlinuz boot=live components quiet\n");
        SystemFacts facts = new SystemFacts(root);
        assertTrue(facts.isLiveSystem());
        assertEquals(Firmware.BIOS, facts.firmware());
        Files.createDirectories(root.resolve("sys/firmware/efi"));
        assertEquals(Firmware.UEFI, facts.firmware());
        Files.writeString(root.resolve("proc/cmdline"), "BOOT_IMAGE=/vmlinuz root=UUID=1 ro quiet\n");
        assertFalse(facts.isLiveSystem());
    }

    @Test
    void parsesMemTotal() {
        assertEquals(Optional.of(16_384_000L * 1024), SystemFacts.parseMemTotal("MemTotal:       16384000 kB\nMemFree: 1 kB\n"));
        assertEquals(Optional.empty(), SystemFacts.parseMemTotal("MemFree: 1 kB\n"));
    }
}
