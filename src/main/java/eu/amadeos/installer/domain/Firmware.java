package eu.amadeos.installer.domain;

/** How the computer booted, and so which boot loader amadeos-install installs. */
public enum Firmware {
    UEFI("uefi"),
    BIOS("bios");

    private final String option;

    Firmware(String option) {
        this.option = option;
    }

    /** The value for {@code FIRMWARE=} in the configuration of amadeos-install. */
    public String option() {
        return option;
    }
}
