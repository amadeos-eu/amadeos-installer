package org.datazuul.amadeos.installer.service;

import org.datazuul.amadeos.installer.domain.BlockDevice;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockDevicesTest {
    /** lsblk --bytes --pairs of a live system from a USB stick with one empty disk and one with data. */
    private static final String LSBLK = """
            NAME="loop0" PATH="/dev/loop0" SIZE="3221225472" MODEL="" TYPE="loop" RM="0" RO="1" MOUNTPOINTS="/run/live/rootfs/filesystem.squashfs" PKNAME="" FSTYPE="squashfs" TRAN="" PARTTYPE="" LABEL=""
            NAME="sda" PATH="/dev/sda" SIZE="500107862016" MODEL="Samsung SSD 860" TYPE="disk" RM="0" RO="0" MOUNTPOINTS="" PKNAME="" FSTYPE="" TRAN="sata" PARTTYPE="" LABEL=""
            NAME="sda1" PATH="/dev/sda1" SIZE="536870912" MODEL="" TYPE="part" RM="0" RO="0" MOUNTPOINTS="" PKNAME="sda" FSTYPE="vfat" TRAN="" PARTTYPE="c12a7328-f81f-11d2-ba4b-00a0c93ec93b" LABEL="EFI"
            NAME="sda2" PATH="/dev/sda2" SIZE="499570991104" MODEL="" TYPE="part" RM="0" RO="0" MOUNTPOINTS="" PKNAME="sda" FSTYPE="ext4" TRAN="" PARTTYPE="0fc63daf-8483-4772-8e79-3d69d8477de4" LABEL="Old\\x20Data"
            NAME="sdb" PATH="/dev/sdb" SIZE="15518924800" MODEL="DataTraveler 3.0" TYPE="disk" RM="1" RO="0" MOUNTPOINTS="" PKNAME="" FSTYPE="iso9660" TRAN="usb" PARTTYPE="" LABEL="amadeos"
            NAME="sdb1" PATH="/dev/sdb1" SIZE="3300000000" MODEL="" TYPE="part" RM="1" RO="0" MOUNTPOINTS="/run/live/medium" PKNAME="sdb" FSTYPE="iso9660" TRAN="" PARTTYPE="0x0" LABEL="amadeos"
            NAME="sdc" PATH="/dev/sdc" SIZE="4000000000" MODEL="Small Stick" TYPE="disk" RM="1" RO="0" MOUNTPOINTS="" PKNAME="" FSTYPE="" TRAN="usb" PARTTYPE="" LABEL=""
            NAME="sr0" PATH="/dev/sr0" SIZE="1073741312" MODEL="QEMU DVD-ROM" TYPE="rom" RM="1" RO="0" MOUNTPOINTS="" PKNAME="" FSTYPE="" TRAN="sata" PARTTYPE="" LABEL=""
            NAME="nvme0n1" PATH="/dev/nvme0n1" SIZE="1000204886016" MODEL="WD Black SN770" TYPE="disk" RM="0" RO="0" MOUNTPOINTS="" PKNAME="" FSTYPE="" TRAN="nvme" PARTTYPE="" LABEL=""
            NAME="nvme0n1p1" PATH="/dev/nvme0n1p1" SIZE="1000204886016" MODEL="" TYPE="part" RM="0" RO="0" MOUNTPOINTS="/media/user/data\\x0a/mnt/data" PKNAME="nvme0n1" FSTYPE="ext4" TRAN="" PARTTYPE="0fc63daf-8483-4772-8e79-3d69d8477de4" LABEL="data"
            """;

    @Test
    void parsesPairs() {
        List<BlockDevice> devices = BlockDevices.parse(LSBLK);
        assertEquals(10, devices.size());
        BlockDevice sda2 = devices.get(3);
        assertEquals("sda2", sda2.name());
        assertEquals("Old Data", sda2.label());
        assertEquals("sda", sda2.parentName());
        assertTrue(sda2.isPartition());
        assertFalse(sda2.isEfiSystem());
        assertTrue(devices.get(2).isEfiSystem());
        assertEquals(List.of("/media/user/data", "/mnt/data"), devices.get(9).mountPoints());
        assertEquals("Samsung SSD 860", devices.get(1).model());
        assertTrue(devices.get(4).removable());
    }

    @Test
    void findsTheDiskOfTheLiveMedium() {
        List<BlockDevice> devices = BlockDevices.parse(LSBLK);
        assertEquals("/dev/sdb", BlockDevices.diskOf("/dev/sdb1", devices));
        assertEquals("/dev/sda", BlockDevices.diskOf("/dev/sda", devices));
        assertEquals("/dev/unknown", BlockDevices.diskOf("/dev/unknown", devices));
    }

    @Test
    void offersOnlyFittingDisks() {
        BlockDevices blockDevices = new BlockDevices();
        List<BlockDevice> devices = BlockDevices.parse(LSBLK);
        List<BlockDevice> disks = blockDevices.installableDisks(devices, Optional.of("/dev/sdb"));
        // Not the loop device, not the live stick, not the 4 GB stick, not the DVD, not the mounted NVMe.
        assertEquals(List.of("sda"), disks.stream().map(BlockDevice::name).toList());
        assertEquals(List.of("sda", "nvme0n1"),
                blockDevices.installableDisks(devices.stream().filter(d -> !d.name().equals("nvme0n1p1")).toList(),
                        Optional.of("/dev/sdb")).stream().map(BlockDevice::name).toList());
    }

    @Test
    void listsUnmountedPartitionsOfADisk() {
        BlockDevices blockDevices = new BlockDevices();
        List<BlockDevice> devices = BlockDevices.parse(LSBLK);
        assertEquals(List.of("sda1", "sda2"),
                blockDevices.partitionsOf(devices.get(1), devices).stream().map(BlockDevice::name).toList());
        assertEquals(List.of(), blockDevices.partitionsOf(devices.get(8), devices));
    }

    @Test
    void unescapes() {
        assertEquals("a b", BlockDevices.unescape("a\\x20b"));
        assertEquals("say \"hi\"", BlockDevices.unescape("say \\\"hi\\\""));
        assertEquals("plain", BlockDevices.unescape("plain"));
    }
}
