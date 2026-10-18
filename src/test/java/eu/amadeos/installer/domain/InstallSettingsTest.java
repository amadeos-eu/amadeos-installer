package eu.amadeos.installer.domain;

import eu.amadeos.commons.i18n.Language;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstallSettingsTest {
    private static BlockDevice disk(String name) {
        return new BlockDevice(name, "/dev/" + name, 20_000_000_000L, "QEMU HARDDISK", "disk", false, false, List.of(),
                "", "", "virtio", "", "");
    }

    @Test
    void validatesNames() {
        assertEquals(Optional.empty(), InstallSettings.hostnameProblem("my-pc"));
        assertTrue(InstallSettings.hostnameProblem("My PC").isPresent());
        assertTrue(InstallSettings.hostnameProblem("-pc").isPresent());
        assertTrue(InstallSettings.hostnameProblem("").isPresent());
        assertEquals(Optional.empty(), InstallSettings.usernameProblem("ralf"));
        assertEquals(Optional.of("user.problem.username.root"), InstallSettings.usernameProblem("root"));
        assertTrue(InstallSettings.usernameProblem("Ralf").isPresent());
        assertTrue(InstallSettings.usernameProblem("1abc").isPresent());
        assertEquals(Optional.empty(), InstallSettings.fullNameProblem("Ralf Eichinger"));
        assertTrue(InstallSettings.fullNameProblem("a:b").isPresent());
    }

    @Test
    void validatesPasswords() {
        assertEquals(Optional.of("user.problem.password"), InstallSettings.passwordProblem("", ""));
        assertEquals(Optional.of("user.problem.password.short"), InstallSettings.passwordProblem("short", "short"));
        assertEquals(Optional.of("user.problem.repeat"), InstallSettings.passwordProblem("longenough", "different"));
        assertEquals(Optional.empty(), InstallSettings.passwordProblem("longenough", "longenough"));
    }

    @Test
    void proposesLoginNames() {
        assertEquals("ralf", InstallSettings.proposeUsername("Ralf Eichinger"));
        assertEquals("jose", InstallSettings.proposeUsername("José García"));
        assertEquals("muller", InstallSettings.proposeUsername("Müller"));
        assertEquals("", InstallSettings.proposeUsername("123"));
        assertEquals("", InstallSettings.proposeUsername(""));
    }

    @Test
    void writesTheConfigurationForAWholeDisk() {
        InstallSettings settings = new InstallSettings();
        settings.setLanguage(Language.GERMAN);
        settings.setKeyboardLayout("de");
        settings.setTimeZone("Europe/Berlin");
        settings.setDisk(disk("vda"));
        settings.setSwap("4G");
        settings.setFirmware(Firmware.UEFI);
        settings.setHostname("pc");
        settings.setUsername("ralf");
        settings.setFullName("Ralf Eichinger");
        settings.setPassword("secret=pass word");
        Map<String, String> config = settings.toConfig();
        assertEquals("/dev/vda", config.get("DISK"));
        assertEquals("4G", config.get("SWAP"));
        assertFalse(config.containsKey("ROOT_PARTITION"));
        assertEquals("uefi", config.get("FIRMWARE"));
        assertEquals("de_DE.UTF-8", config.get("LOCALE"));
        assertEquals("de", config.get("KEYBOARD"));
        assertEquals("", config.get("KEYBOARD_VARIANT"));
        assertEquals("Europe/Berlin", config.get("TIMEZONE"));
        assertEquals("secret=pass word", config.get("PASSWORD"));
        assertEquals("Ralf Eichinger", config.get("FULLNAME"));
    }

    @Test
    void writesTheConfigurationForAPartition() {
        InstallSettings settings = new InstallSettings();
        settings.setTargetMode(TargetMode.PARTITION);
        BlockDevice root = new BlockDevice("sda3", "/dev/sda3", 50_000_000_000L, "", "part", false, false, List.of(),
                "sda", "ext4", "", BlockDevice.EFI_SYSTEM_TYPE.replace('c', 'd'), "");
        settings.setRootPartition(root);
        settings.setUsername("u");
        settings.setPassword("p");
        Map<String, String> config = settings.toConfig();
        assertEquals("/dev/sda3", config.get("ROOT_PARTITION"));
        assertFalse(config.containsKey("DISK"));
        assertFalse(config.containsKey("EFI_PARTITION"));
        assertEquals(Optional.of(root), settings.target());
    }

    @Test
    void refusesAConfigurationWithoutTarget() {
        InstallSettings settings = new InstallSettings();
        assertThrows(IllegalStateException.class, settings::toConfig);
    }
}
