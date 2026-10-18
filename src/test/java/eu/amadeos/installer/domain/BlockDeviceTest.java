package eu.amadeos.installer.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockDeviceTest {
    @Test
    void formatsSizesAsSold() {
        assertEquals("500 GB", BlockDevice.sizeText(500_107_862_016L));
        assertEquals("1.0 TB", BlockDevice.sizeText(1_000_204_886_016L));
        assertEquals("537 MB", BlockDevice.sizeText(536_870_912L));
        assertEquals("8.0 GB", BlockDevice.sizeText(8_000_000_000L));
        assertEquals("0 B", BlockDevice.sizeText(0));
    }

    @Test
    void describesADisk() {
        BlockDevice disk = new BlockDevice("sda", "/dev/sda", 500_107_862_016L, "Samsung SSD 860", "disk", false, false,
                List.of(), "", "", "sata", "", "");
        assertEquals("Samsung SSD 860 (sda, SATA, 500 GB)", disk.displayName());
        BlockDevice virtual = new BlockDevice("vda", "/dev/vda", 21_474_836_480L, "", "disk", false, false,
                List.of(), "", "", "", "", "");
        assertEquals("(vda, 21 GB)", virtual.displayName());
    }
}
