package org.datazuul.amadeos.installer.ui;

import org.datazuul.amadeos.commons.i18n.Language;
import org.datazuul.amadeos.commons.i18n.Mnemonics;
import org.datazuul.amadeos.installer.domain.InstallSettings;
import org.datazuul.amadeos.installer.service.SystemFacts;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

/** Welcome, and the language of the installed system (and of the wizard from here on). */
final class LanguagePage extends WizardPage {
    LanguagePage(InstallerWindow window, InstallSettings settings, SystemFacts facts) {
        super(window, settings);
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        // Every child left-aligned, or BoxLayout centres the narrower ones.
        JTextArea intro = paragraph(tr("language.intro"));
        intro.setAlignmentX(LEFT_ALIGNMENT);
        column.add(intro);
        column.add(Box.createVerticalStrut(18));

        JComboBox<Language> languages = new JComboBox<>(Language.values());
        languages.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                                                                   boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof Language language) setText(language.nativeName());
                return this;
            }
        });
        languages.setSelectedItem(settings.language());
        languages.addActionListener(e -> {
            Language chosen = (Language) languages.getSelectedItem();
            if (chosen != null && chosen != settings.language()) {
                settings.setLanguage(chosen);
                window.changeLanguage(chosen.locale());
            }
        });
        JLabel label = Mnemonics.apply(new JLabel(), tr("language.label"));
        label.setLabelFor(languages);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.add(label);
        row.add(Box.createHorizontalStrut(8));
        languages.setPreferredSize(new Dimension(240, languages.getPreferredSize().height));
        row.add(languages);
        row.setAlignmentX(LEFT_ALIGNMENT);
        column.add(row);

        if (!facts.isLiveSystem()) {
            column.add(Box.createVerticalStrut(18));
            JTextArea warning = paragraph(tr("language.not-live"));
            warning.setForeground(new Color(0xA0, 0x10, 0x10));
            warning.setAlignmentX(LEFT_ALIGNMENT);
            column.add(warning);
        }
        add(column, BorderLayout.NORTH);
    }

    @Override
    public String titleKey() {
        return "step.language";
    }
}
