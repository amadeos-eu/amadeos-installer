package eu.amadeos.installer.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeZonesTest {
    @Test
    void parsesTheTable() {
        String table = """
                # tz zone descriptions
                #country-\tcoordinates\tTZ\tcomments
                DE,DK,NO,SE,SJ\t+5230+01322\tEurope/Berlin\tmost of Germany
                AD\t+4230+00131\tEurope/Andorra
                BAD\t+0000+00000\t../etc/passwd
                """;
        assertEquals(List.of("Europe/Andorra", "Europe/Berlin"), TimeZones.parseTable(table));
    }

    @Test
    void listsUtcFirstAndReadsLocaltime(@TempDir Path root) throws IOException {
        Path zoneinfo = root.resolve(TimeZones.ZONEINFO);
        Files.createDirectories(zoneinfo);
        Files.writeString(zoneinfo.resolve(TimeZones.TABLE), "DE\t+5230+01322\tEurope/Berlin\n");
        Files.createDirectories(root.resolve("etc"));
        Files.createSymbolicLink(root.resolve("etc/localtime"), Path.of("/usr/share/zoneinfo/Europe/Berlin"));
        TimeZones zones = new TimeZones(root);
        assertEquals(List.of("UTC", "Europe/Berlin"), zones.available());
        assertEquals(Optional.of("Europe/Berlin"), zones.current());
    }

    @Test
    void fallsBackToTheJdkWithoutTheTable(@TempDir Path root) {
        List<String> zones = new TimeZones(root).available();
        assertEquals("UTC", zones.get(0));
        assertTrue(zones.contains("Europe/Berlin"));
        assertTrue(new TimeZones(root).current().isPresent());
    }
}
