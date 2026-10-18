package eu.amadeos.installer.bootstrap;

import eu.amadeos.commons.app.AmadeApplication;
import eu.amadeos.commons.app.AppInfo;
import eu.amadeos.installer.ui.InstallerWindow;

/** Entry point of the installer wizard. Takes no arguments; the installation itself is done by amadeos-install. */
public final class AmadeosInstallerApplication {
    public static final AppInfo APP = new AppInfo("amadeos-installer", "Install amadeOS", "installer");

    private AmadeosInstallerApplication() {
    }

    public static void main(String[] args) {
        AmadeApplication.launch(APP, injector -> injector.getInstance(InstallerWindow.class).setVisible(true),
                new InstallerModule());
    }
}
