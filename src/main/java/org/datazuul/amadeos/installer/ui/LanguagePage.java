package org.datazuul.amadeos.installer.ui;

import org.datazuul.amadeos.commons.i18n.Language;
import org.datazuul.amadeos.commons.i18n.Mnemonics;
import org.datazuul.amadeos.installer.domain.InstallSettings;
import org.datazuul.amadeos.installer.service.SystemFacts;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Welcome, and the language of the installed system (and of the wizard from here on). */
final class LanguagePage extends WizardPage {
    LanguagePage(InstallerWindow window, InstallSettings settings, SystemFacts facts) {
        super(window, settings);
        // The intro at the top like on every page; the form below it in a holder, so that it keeps its
        // preferred height instead of being squeezed or centred.
        add(paragraph(tr("language.intro")), BorderLayout.NORTH);

        JComboBox<Language> languages = new JComboBox<>(Language.values());
        languages.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof Language language) setText(language.nativeName());
                return this;
            }
        });
        languages.setSelectedItem(settings.language());
        languages.setPreferredSize(new Dimension(240, languages.getPreferredSize().height));
        languages.addActionListener(e -> {
            Language chosen = (Language) languages.getSelectedItem();
            if (chosen != null && chosen != settings.language()) {
                settings.setLanguage(chosen);
                window.changeLanguage(chosen.locale());
            }
        });
        JLabel label = Mnemonics.apply(new JLabel(), tr("language.label"));
        label.setLabelFor(languages);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(8, 0, 0, 8);
        form.add(label, c);
        c.gridx = 1;
        c.insets = new Insets(8, 0, 0, 0);
        form.add(languages, c);
        // Takes the rest of the row, so that label and list stay at the left.
        c.gridx = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(new JPanel(), c);
        if (!facts.isLiveSystem()) {
            JTextArea warning = paragraph(tr("language.not-live"));
            warning.setForeground(new Color(0xA0, 0x10, 0x10));
            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = 3;
            c.insets = new Insets(18, 0, 0, 0);
            form.add(warning, c);
        }
        JPanel holder = new JPanel(new BorderLayout());
        holder.add(form, BorderLayout.NORTH);
        add(holder, BorderLayout.CENTER);
    }

    @Override
    public String titleKey() {
        return "step.language";
    }
}
