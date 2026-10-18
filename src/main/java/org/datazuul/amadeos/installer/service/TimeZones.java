package org.datazuul.amadeos.installer.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * The time zones to choose from and the one the live system runs in: the same list as the login greeter
 * offers (UTC, then the zones of {@code zone1970.tab}), read from the running system.
 */
public class TimeZones {
    public static final String UTC = "UTC";
    static final String ZONEINFO = "usr/share/zoneinfo";
    static final String TABLE = "zone1970.tab";
    private static final Pattern NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_+-]*(/[A-Za-z0-9_+-]+){0,2}");

    private final Path root;

    public TimeZones() {
        this(Path.of("/"));
    }

    TimeZones(Path root) {
        this.root = root;
    }

    /** UTC, then the zones of {@code zone1970.tab} in alphabetical order; the JDK's zones without the table. */
    public List<String> available() {
        List<String> zones = new ArrayList<>();
        zones.add(UTC);
        Path table = root.resolve(ZONEINFO).resolve(TABLE);
        if (Files.isReadable(table)) {
            try {
                zones.addAll(parseTable(Files.readString(table, StandardCharsets.UTF_8)));
                return zones;
            } catch (IOException e) {
                // fall through to the JDK's list
            }
        }
        TreeSet<String> ids = new TreeSet<>();
        for (String id : ZoneId.getAvailableZoneIds()) {
            if (id.indexOf('/') > 0 && !id.startsWith("Etc/") && !id.startsWith("SystemV/") && NAME.matcher(id).matches()) {
                ids.add(id);
            }
        }
        zones.addAll(ids);
        return zones;
    }

    /** The zone names (third column) of {@code zone1970.tab}, sorted. */
    static List<String> parseTable(String text) {
        TreeSet<String> zones = new TreeSet<>();
        for (String line : text.split("\n")) {
            if (line.startsWith("#") || line.isBlank()) continue;
            String[] columns = line.split("\t");
            if (columns.length >= 3 && NAME.matcher(columns[2]).matches()) zones.add(columns[2]);
        }
        return new ArrayList<>(zones);
    }

    /** The zone {@code /etc/localtime} points to (the one chosen at login), else the JVM's default zone. */
    public Optional<String> current() {
        Path localtime = root.resolve("etc").resolve("localtime");
        try {
            if (Files.isSymbolicLink(localtime)) {
                Path target = Files.readSymbolicLink(localtime);
                Path resolved = (target.isAbsolute() ? target : Path.of("/etc").resolve(target)).normalize();
                Path zoneinfo = Path.of("/").resolve(ZONEINFO);
                if (resolved.startsWith(zoneinfo) && !resolved.equals(zoneinfo)) {
                    String zone = zoneinfo.relativize(resolved).toString();
                    if (NAME.matcher(zone).matches()) return Optional.of(zone);
                }
            }
        } catch (IOException | SecurityException e) {
            // fall through
        }
        String id = ZoneId.systemDefault().getId();
        return NAME.matcher(id).matches() ? Optional.of(id) : Optional.empty();
    }
}
