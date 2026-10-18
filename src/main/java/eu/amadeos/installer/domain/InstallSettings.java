package eu.amadeos.installer.domain;

import eu.amadeos.commons.i18n.Language;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * What the wizard collects, page by page, and hands to amadeos-install as its configuration file. The
 * same rules as in the script, so that a problem is shown on the page and not by the script afterwards.
 */
public final class InstallSettings {
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final String DEFAULT_HOSTNAME = "amadeos";
    public static final String NO_SWAP = "none";
    private static final Pattern HOSTNAME = Pattern.compile("[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?");
    private static final Pattern USERNAME = Pattern.compile("[a-z_][a-z0-9_-]{0,31}");
    private static final Pattern FULLNAME_FORBIDDEN = Pattern.compile("[:,=\\n]");
    private static final Pattern KEYBOARD_VARIANT = Pattern.compile("[a-zA-Z0-9_,-]*");

    private Language language = Language.of(Locale.getDefault()).orElse(Language.ENGLISH);
    private String keyboardLayout = "us";
    private String keyboardVariant = "";
    private String timeZone = "UTC";
    private TargetMode targetMode = TargetMode.WHOLE_DISK;
    private BlockDevice disk;
    private BlockDevice rootPartition;
    private BlockDevice efiPartition;
    private String swap = NO_SWAP;
    private Firmware firmware = Firmware.BIOS;
    private String hostname = DEFAULT_HOSTNAME;
    private String fullName = "";
    private String username = "";
    private String password = "";

    public Language language() { return language; }
    public void setLanguage(Language language) { this.language = language; }
    public String keyboardLayout() { return keyboardLayout; }
    public void setKeyboardLayout(String keyboardLayout) { this.keyboardLayout = keyboardLayout; }
    public String keyboardVariant() { return keyboardVariant; }
    public void setKeyboardVariant(String keyboardVariant) { this.keyboardVariant = keyboardVariant.strip(); }
    public String timeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }
    public TargetMode targetMode() { return targetMode; }
    public void setTargetMode(TargetMode targetMode) { this.targetMode = targetMode; }
    public Optional<BlockDevice> disk() { return Optional.ofNullable(disk); }
    public void setDisk(BlockDevice disk) { this.disk = disk; }
    public Optional<BlockDevice> rootPartition() { return Optional.ofNullable(rootPartition); }
    public void setRootPartition(BlockDevice rootPartition) { this.rootPartition = rootPartition; }
    /** Empty: amadeos-install takes the first EFI System Partition of the disk. */
    public Optional<BlockDevice> efiPartition() { return Optional.ofNullable(efiPartition); }
    public void setEfiPartition(BlockDevice efiPartition) { this.efiPartition = efiPartition; }
    /** {@code none} or a size amadeos-install understands, e.g. {@code 2G}. */
    public String swap() { return swap; }
    public void setSwap(String swap) { this.swap = swap; }
    public Firmware firmware() { return firmware; }
    public void setFirmware(Firmware firmware) { this.firmware = firmware; }
    public String hostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname.strip(); }
    public String fullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName.strip(); }
    public String username() { return username; }
    public void setUsername(String username) { this.username = username.strip(); }
    public String password() { return password; }
    public void setPassword(String password) { this.password = password; }

    /** The device that is erased: the disk, or the root partition. */
    public Optional<BlockDevice> target() {
        return targetMode == TargetMode.WHOLE_DISK ? disk() : rootPartition();
    }

    /** Key of the problem with the host name, or empty. */
    public static Optional<String> hostnameProblem(String hostname) {
        return HOSTNAME.matcher(hostname.strip()).matches() ? Optional.empty() : Optional.of("user.problem.hostname");
    }

    public static Optional<String> usernameProblem(String username) {
        String name = username.strip();
        if (name.equals("root")) return Optional.of("user.problem.username.root");
        return USERNAME.matcher(name).matches() ? Optional.empty() : Optional.of("user.problem.username");
    }

    public static Optional<String> fullNameProblem(String fullName) {
        return FULLNAME_FORBIDDEN.matcher(fullName).find() ? Optional.of("user.problem.fullname") : Optional.empty();
    }

    public static Optional<String> passwordProblem(String password, String repeated) {
        if (password.isEmpty()) return Optional.of("user.problem.password");
        if (password.length() < MIN_PASSWORD_LENGTH) return Optional.of("user.problem.password.short");
        if (!password.equals(repeated)) return Optional.of("user.problem.repeat");
        return Optional.empty();
    }

    public static boolean isValidKeyboardVariant(String variant) {
        return KEYBOARD_VARIANT.matcher(variant).matches();
    }

    /**
     * A login name made from the full name, as a proposal: the first word, lower case, letters and digits
     * only, accents stripped; empty when nothing usable is left.
     */
    public static String proposeUsername(String fullName) {
        String first = fullName.strip().split("\\s+", 2)[0];
        String plain = java.text.Normalizer.normalize(first, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replace("ß", "ss")
                .replaceAll("[^a-z0-9]", "");
        if (plain.isEmpty() || !Character.isLetter(plain.charAt(0))) return "";
        return plain.length() > 32 ? plain.substring(0, 32) : plain;
    }

    /** The configuration file of amadeos-install, key by key, in its order. */
    public Map<String, String> toConfig() {
        Map<String, String> config = new LinkedHashMap<>();
        if (targetMode == TargetMode.WHOLE_DISK) {
            config.put("DISK", disk().map(BlockDevice::path).orElseThrow(() -> new IllegalStateException("no disk")));
            config.put("SWAP", swap);
        } else {
            config.put("ROOT_PARTITION", rootPartition().map(BlockDevice::path)
                    .orElseThrow(() -> new IllegalStateException("no root partition")));
            efiPartition().ifPresent(efi -> config.put("EFI_PARTITION", efi.path()));
        }
        config.put("FIRMWARE", firmware.option());
        config.put("HOSTNAME", hostname);
        config.put("USERNAME", username);
        config.put("FULLNAME", fullName);
        config.put("PASSWORD", password);
        config.put("LOCALE", language.posixLocale());
        config.put("KEYBOARD", keyboardLayout);
        config.put("KEYBOARD_VARIANT", keyboardVariant);
        config.put("TIMEZONE", timeZone);
        return config;
    }
}
