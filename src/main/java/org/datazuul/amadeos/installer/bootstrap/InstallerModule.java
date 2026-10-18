package org.datazuul.amadeos.installer.bootstrap;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import org.datazuul.amadeos.installer.domain.InstallSettings;
import org.datazuul.amadeos.installer.service.BlockDevices;
import org.datazuul.amadeos.installer.service.InstallRunner;
import org.datazuul.amadeos.installer.service.KeyboardLayouts;
import org.datazuul.amadeos.installer.service.SystemFacts;
import org.datazuul.amadeos.installer.service.TimeZones;
import org.datazuul.amadeos.installer.ui.InstallerWindow;

/**
 * Installer bindings. Executor, theme, locale and the EDT executor come from
 * {@link org.datazuul.amadeos.commons.app.CommonsModule}.
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
