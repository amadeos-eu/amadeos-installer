package eu.amadeos.installer.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Map;

/**
 * The configuration file for {@code amadeos-install --config}: {@code KEY=VALUE}, one per line, the value
 * everything after the first {@code =}. Readable by its owner only, since it carries the password; root
 * reads it anyway.
 */
public final class ConfigFile {
    private ConfigFile() {
    }

    public static String render(Map<String, String> config) {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, String> entry : config.entrySet()) {
            String value = entry.getValue();
            if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
                throw new IllegalArgumentException(entry.getKey() + " must not contain line breaks");
            }
            text.append(entry.getKey()).append('=').append(value).append('\n');
        }
        return text.toString();
    }

    /** Writes the file into the temporary directory with mode 0600 and returns its path. */
    public static Path write(Map<String, String> config) throws IOException {
        EnumSet<PosixFilePermission> ownerOnly = EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
        Path file = Files.createTempFile("amadeos-install-", ".conf", PosixFilePermissions.asFileAttribute(ownerOnly));
        Files.writeString(file, render(config), StandardCharsets.UTF_8);
        return file;
    }
}
