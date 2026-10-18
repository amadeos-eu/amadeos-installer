package eu.amadeos.installer.i18n;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.TranslationCheck;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslationTest {
    private static final List<Locale> LOCALES = List.of(Locale.ENGLISH, Locale.GERMANY, Locale.FRANCE,
            Locale.of("es", "ES"), Locale.ITALY);
    /** The step ids amadeos-install prints, each with a text. */
    private static final List<String> STEP_IDS = List.of("partition", "format", "mount", "copy", "live", "configure",
            "user", "initramfs", "bootloader", "finish");

    private static Messages in(Locale locale) {
        return Messages.load(Texts.class, Texts.BUNDLE, locale);
    }

    @Test
    void everyLanguageHasEveryText() {
        assertEquals(Map.of(), TranslationCheck.problems(Texts.class, Texts.BUNDLE));
    }

    @Test
    void everyStepOfTheScriptHasAText() {
        Messages english = in(Locale.ENGLISH);
        for (String id : STEP_IDS) assertTrue(english.has("progress.task." + id), id);
    }

    @Test
    void navigationMnemonicsAreUniquePerLanguage() {
        List<String> buttons = List.of("nav.back", "nav.next", "nav.cancel");
        for (Locale locale : LOCALES) {
            Set<Character> letters = new HashSet<>();
            for (String key : buttons) {
                String text = in(locale).get(key);
                int amp = text.indexOf('&');
                assertTrue(amp >= 0, locale + " " + key);
                assertTrue(letters.add(Character.toLowerCase(text.charAt(amp + 1))), locale + " " + key + " " + text);
            }
        }
    }

    @Test
    void userPageMnemonicsAreUniquePerLanguage() {
        List<String> labels = List.of("user.fullname", "user.username", "user.password", "user.repeat", "user.show",
                "user.hostname", "nav.back", "nav.next", "nav.cancel");
        for (Locale locale : LOCALES) {
            Set<Character> letters = new HashSet<>();
            for (String key : labels) {
                String text = in(locale).get(key);
                int amp = text.indexOf('&');
                assertTrue(amp >= 0, locale + " " + key);
                assertTrue(letters.add(Character.toLowerCase(text.charAt(amp + 1))), locale + " " + key + " " + text);
            }
        }
    }

    @Test
    void diskPageMnemonicsAreUniquePerLanguage() {
        List<String> labels = List.of("disk.whole", "disk.disk", "disk.swap", "disk.partition", "disk.root",
                "disk.refresh", "nav.back", "nav.next", "nav.cancel");
        for (Locale locale : LOCALES) {
            Set<Character> letters = new HashSet<>();
            for (String key : labels) {
                String text = in(locale).get(key);
                int amp = text.indexOf('&');
                assertTrue(amp >= 0, locale + " " + key);
                assertTrue(letters.add(Character.toLowerCase(text.charAt(amp + 1))), locale + " " + key + " " + text);
            }
        }
    }

    @Test
    void summaryPageMnemonicsAreUniquePerLanguage() {
        List<String> labels = List.of("summary.confirm", "nav.back", "nav.install", "nav.cancel");
        for (Locale locale : LOCALES) {
            Set<Character> letters = new HashSet<>();
            for (String key : labels) {
                String text = in(locale).get(key);
                int amp = text.indexOf('&');
                assertTrue(amp >= 0, locale + " " + key);
                assertTrue(letters.add(Character.toLowerCase(text.charAt(amp + 1))), locale + " " + key + " " + text);
            }
        }
    }

    @Test
    void argumentsAreFilledIn() {
        assertEquals("Step 4 of 10: Copying the system", in(Locale.ENGLISH).format("progress.step", 4, 10, "Copying the system"));
        assertEquals("Alle Daten auf /dev/sda werden gelöscht.", in(Locale.GERMANY).format("summary.warning.whole", "/dev/sda"));
    }
}
