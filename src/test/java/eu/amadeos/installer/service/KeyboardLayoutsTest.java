package eu.amadeos.installer.service;

import eu.amadeos.commons.i18n.Language;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class KeyboardLayoutsTest {
    private static final String EVDEV_LST = """
            ! model
              pc101           Generic 101-key PC
            
            ! layout
              us              English (US)
              de              German
              ch              German (Switzerland)
              latam           Spanish (Latin American)
            
            ! variant
              chr             us: Cherokee
            """;

    @Test
    void parsesTheLayoutSection() {
        List<KeyboardLayouts.Layout> layouts = KeyboardLayouts.parse(EVDEV_LST);
        assertEquals(List.of(new KeyboardLayouts.Layout("us", "English (US)"), new KeyboardLayouts.Layout("de", "German"),
                new KeyboardLayouts.Layout("ch", "German (Switzerland)"),
                new KeyboardLayouts.Layout("latam", "Spanish (Latin American)")), layouts);
    }

    @Test
    void fallsBackToABuiltInListWithoutTheFile() {
        List<KeyboardLayouts.Layout> layouts = new KeyboardLayouts(java.nio.file.Path.of("/nonexistent/evdev.lst")).list();
        assertFalse(layouts.isEmpty());
        assertEquals("us", layouts.get(0).code());
    }

    @Test
    void readsTheFirstLayoutOfTheSession() {
        assertEquals(Optional.of("de"), KeyboardLayouts.parseQuery("rules:      evdev\nmodel:      pc105\nlayout:     de,us\noptions:    grp:alt_shift_toggle\n"));
        assertEquals(Optional.of("us"), KeyboardLayouts.parseQuery("layout:     us\n"));
        assertEquals(Optional.empty(), KeyboardLayouts.parseQuery("Cannot open display\n"));
    }

    @Test
    void proposesTheLayoutOfTheLanguage() {
        assertEquals("de", KeyboardLayouts.defaultFor(Language.GERMAN));
        assertEquals("us", KeyboardLayouts.defaultFor(Language.ENGLISH));
    }
}
