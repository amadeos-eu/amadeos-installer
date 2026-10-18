package eu.amadeos.installer.service;

import eu.amadeos.commons.i18n.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The XKB keyboard layouts, from {@code evdev.lst}, and the one the live session uses. */
public class KeyboardLayouts {
    private static final Logger LOG = LoggerFactory.getLogger(KeyboardLayouts.class);
    static final Path EVDEV_LIST = Path.of("/usr/share/X11/xkb/rules/evdev.lst");
    private static final Pattern LIST_LINE = Pattern.compile("^\\s+([a-z]{2,8})\\s+(.+?)\\s*$");

    /** A layout: the XKB code and the name xkeyboard-config gives it, e.g. {@code de} and {@code German}. */
    public record Layout(String code, String name) {
        @Override
        public String toString() {
            return name + " (" + code + ")";
        }
    }

    private final Path evdevList;

    public KeyboardLayouts() {
        this(EVDEV_LIST);
    }

    KeyboardLayouts(Path evdevList) {
        this.evdevList = evdevList;
    }

    /** All layouts, in the order of {@code evdev.lst}; a short built-in list when the file is missing. */
    public List<Layout> list() {
        try {
            if (Files.isReadable(evdevList)) {
                List<Layout> layouts = parse(Files.readString(evdevList, StandardCharsets.UTF_8));
                if (!layouts.isEmpty()) return layouts;
            }
        } catch (IOException e) {
            LOG.warn("Cannot read {}", evdevList, e);
        }
        return List.of(new Layout("us", "English (US)"), new Layout("gb", "English (UK)"), new Layout("de", "German"),
                new Layout("at", "German (Austria)"), new Layout("ch", "German (Switzerland)"), new Layout("fr", "French"),
                new Layout("be", "Belgian"), new Layout("es", "Spanish"), new Layout("latam", "Spanish (Latin American)"),
                new Layout("it", "Italian"), new Layout("pt", "Portuguese"), new Layout("nl", "Dutch"),
                new Layout("dk", "Danish"), new Layout("se", "Swedish"), new Layout("no", "Norwegian"),
                new Layout("fi", "Finnish"), new Layout("pl", "Polish"), new Layout("cz", "Czech"));
    }

    /** The {@code ! layout} section of evdev.lst: {@code  de              German}. */
    static List<Layout> parse(String text) {
        List<Layout> layouts = new ArrayList<>();
        boolean inSection = false;
        for (String line : text.split("\n")) {
            if (line.startsWith("!")) {
                inSection = line.strip().equals("! layout");
                continue;
            }
            if (!inSection) continue;
            Matcher matcher = LIST_LINE.matcher(line);
            if (matcher.matches()) layouts.add(new Layout(matcher.group(1), matcher.group(2)));
        }
        return layouts;
    }

    /** The layout usual for the language: German keyboards for German, and so on. */
    public static String defaultFor(Language language) {
        return switch (language) {
            case GERMAN -> "de";
            case FRENCH -> "fr";
            case SPANISH -> "es";
            case ITALIAN -> "it";
            case ENGLISH -> "us";
        };
    }

    /** The first layout of the running X session ({@code setxkbmap -query}), e.g. {@code de}. */
    public Optional<String> current() {
        try {
            Process process = new ProcessBuilder("setxkbmap", "-query").redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            process.waitFor();
            return parseQuery(output);
        } catch (IOException | RuntimeException e) {
            LOG.debug("setxkbmap -query failed", e);
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    static Optional<String> parseQuery(String output) {
        for (String line : output.split("\n")) {
            if (line.startsWith("layout:")) {
                String value = line.substring("layout:".length()).strip();
                int comma = value.indexOf(',');
                String first = comma >= 0 ? value.substring(0, comma) : value;
                return first.isEmpty() ? Optional.empty() : Optional.of(first);
            }
        }
        return Optional.empty();
    }

    /** Switches the running X session to the layout, so that the user can try it. */
    public void apply(String layout, String variant) {
        List<String> command = new ArrayList<>(List.of("setxkbmap", "-layout", layout));
        if (!variant.isBlank()) command.addAll(List.of("-variant", variant));
        try {
            new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        } catch (IOException e) {
            LOG.debug("setxkbmap failed", e);
        }
    }
}
