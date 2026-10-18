package eu.amadeos.installer.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InstallOutputTest {
    @Test
    void parsesTheMarkers() {
        assertEquals(new InstallOutput.Step(4, 10, "copy"), InstallOutput.parse("@@step 4/10 copy"));
        assertEquals(new InstallOutput.Percent(42), InstallOutput.parse("@@percent 42"));
        assertEquals(new InstallOutput.Percent(100), InstallOutput.parse("@@percent 250"));
        assertEquals(new InstallOutput.Error("The installation failed (step copy); see /var/log/amadeos-install.log"),
                InstallOutput.parse("@@error The installation failed (step copy); see /var/log/amadeos-install.log"));
        assertEquals(new InstallOutput.Done(), InstallOutput.parse("@@done"));
    }

    @Test
    void keepsPlainTextAndBrokenMarkers() {
        assertEquals(new InstallOutput.Text("+ mkfs.ext4 -q -F -L amadeos /dev/vda3"),
                InstallOutput.parse("+ mkfs.ext4 -q -F -L amadeos /dev/vda3"));
        assertEquals(new InstallOutput.Text("@@step x/y"), InstallOutput.parse("@@step x/y"));
        assertEquals(new InstallOutput.Text("@@percent many"), InstallOutput.parse("@@percent many"));
        assertEquals(new InstallOutput.Text("@@unknown"), InstallOutput.parse("@@unknown"));
    }
}
