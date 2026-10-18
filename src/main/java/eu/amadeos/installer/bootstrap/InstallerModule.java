package eu.amadeos.installer.bootstrap;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import eu.amadeos.installer.domain.InstallSettings;
import eu.amadeos.installer.service.BlockDevices;
import eu.amadeos.installer.service.InstallRunner;
import eu.amadeos.installer.service.KeyboardLayouts;
import eu.amadeos.installer.service.SystemFacts;
import eu.amadeos.installer.service.TimeZones;
import eu.amadeos.installer.ui.InstallerWindow;

/**
 * Installer bindings. Executor, theme, locale and the EDT executor come from
 * {@link eu.amadeos.commons.app.CommonsModule}.
 */
public final class InstallerModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(InstallSettings.class).in(Scopes.SINGLETON);
        bind(SystemFacts.class).in(Scopes.SINGLETON);
        bind(BlockDevices.class).in(Scopes.SINGLETON);
        bind(TimeZones.class).in(Scopes.SINGLETON);
        bind(KeyboardLayouts.class).in(Scopes.SINGLETON);
        bind(InstallRunner.class).in(Scopes.SINGLETON);
        bind(InstallerWindow.class).in(Scopes.SINGLETON);
    }
}
