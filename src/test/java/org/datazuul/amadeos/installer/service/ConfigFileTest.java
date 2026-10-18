package org.datazuul.amadeos.installer.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigFileTest {
    @Test
    void rendersOneKeyPerLineWithTheValueAsItIs() {
        Map<String, String> config = new LinkedHashMap<>();
        config.put("DISK", "/dev/vda");
        config.put("PASSWORD", " pass=word \"quoted\" #not a comment ");
        config.put("FULLNAME", "");
        assertEquals("DISK=/dev/vda\nPASSWORD= pass=word \"quoted\" #not a comment \nFULLNAME=\n", ConfigFile.render(config));
    }

    @Test
    void refusesLineBreaks() {
        assertThrows(IllegalArgumentException.class, () -> ConfigFile.render(Map.of("PASSWORD", "a\nb")));
    }

    @Test
    void writesAPrivateFile() throws IOException {
        Path file = ConfigFile.write(Map.of("USERNAME", "ralf"));
        try {
            assertEquals("USERNAME=ralf\n", Files.readString(file));
            assertEquals(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
                    Files.getPosixFilePermissions(file));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
