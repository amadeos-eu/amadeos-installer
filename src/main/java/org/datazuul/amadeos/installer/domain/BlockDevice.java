package org.datazuul.amadeos.installer.domain;

import java.util.List;
import java.util.Locale;

/**
 * One line of {@code lsblk}: a disk or a partition.
 *
 * @param name        kernel name, e.g. {@code sda} or {@code nvme0n1p2}
 * @param path        device path, e.g. {@code /dev/sda}
 * @param size        in bytes
 * @param model       the disk's model, often empty for partitions and virtual disks
 * @param type        {@code disk}, {@code part}, {@code loop}, {@code rom}, ...
 * @param removable   removable medium (USB stick, card reader)
 * @param readOnly    read-only device
 * @param mountPoints where it is mounted; empty when it is not
 * @param parentName  the disk a partition belongs to, empty for a disk
 * @param fsType      file system on it, empty for none
 * @param transport   {@code sata}, {@code usb}, {@code nvme}, ..., or empty
 * @param partType    GPT type GUID or MBR type of a partition, empty for a disk
 * @param label       file system label, or empty
 */
public record BlockDevice(String name, String path, long size, String model, String type, boolean removable,
                          boolean readOnly, List<String> mountPoints, String parentName, String fsType,
                          String transport, String partType, String label) {
    /** GPT type of an EFI System Partition. */
    public static final String EFI_SYSTEM_TYPE = "c12a7328-f81b-11d2-ba4b-00a0c93ec93b";

    public BlockDevice {
        mountPoints = List.copyOf(mountPoints);
    }

    public boolean isDisk() {
        return "disk".equals(type);
    }

    public boolean isPartition() {
        return "part".equals(type);
    }

    public boolean isMounted() {
        return !mountPoints.isEmpty();
    }

    public boolean isEfiSystem() {
        return EFI_SYSTEM_TYPE.equalsIgnoreCase(partType);
    }

    /** True for a partition of the disk (or of any device with that name). */
    public boolean isChildOf(BlockDevice disk) {
        return parentName.equals(disk.name());
    }

    /** The size in decimal units as disks are sold: {@code 500 GB}, {@code 1.0 TB}, {@code 512 MB}. */
    public String sizeText() {
        return sizeText(size);
    }

    public static String sizeText(long bytes) {
        String[] units = {"B", "kB", "MB", "GB", "TB", "PB"};
        double value = bytes;
        int unit = 0;
        while (value >= 1000 && unit < units.length - 1) {
            value /= 1000;
            unit++;
        }
        if (unit == 0) return bytes + " B";
        return String.format(Locale.ROOT, value < 10 ? "%.1f %s" : "%.0f %s", value, units[unit]);
    }

    /** What the user sees: model and transport if known, then the name and size. */
    public String displayName() {
        StringBuilder text = new StringBuilder();
        if (!model.isBlank()) text.append(model.strip()).append(' ');
        text.append('(').append(name);
        if (!transport.isBlank()) text.append(", ").append(transport.toUpperCase(Locale.ROOT));
        text.append(", ").append(sizeText()).append(')');
        return text.toString();
    }
}
