package org.datazuul.amadeos.installer.ui;

import org.datazuul.amadeos.commons.i18n.Mnemonics;
import org.datazuul.amadeos.installer.domain.InstallSettings;
import org.datazuul.amadeos.installer.service.KeyboardLayouts;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Optional;

/** The keyboard layout, applied to the live session at once so that it can be tried. */
final class KeyboardPage extends WizardPage {
    private final KeyboardLayouts layouts;
    private final JList<KeyboardLayouts.Layout> list = new JList<>();
    private final JTextField variant = new JTextField(12);
    private boolean chosenOnce;

    KeyboardPage(InstallerWindow window, InstallSettings settings, KeyboardLayouts layouts) {
        super(window, settings);
        this.layouts = layouts;
        add(paragraph(tr("keyboard.intro")), BorderLayout.NORTH);

        DefaultListModel<KeyboardLayouts.Layout> model = new DefaultListModel<>();
        List<KeyboardLayouts.Layout> all = layouts.list();
        model.addAll(all);
        list.setModel(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(10);
        JScrollPane scroll = new JScrollPane(list);
        add(scroll, BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 0, 2, 8);
        c.anchor = GridBagConstraints.LINE_START;
        c.gridx = 0;
        c.gridy = 0;
        JLabel variantLabel = Mnemonics.apply(new JLabel(), tr("keyboard.variant"));
        variantLabel.setLabelFor(variant);
        form.add(variantLabel, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        variant.setText(settings.keyboardVariant());
        form.add(variant, c);
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        JTextField tryField = new JTextField();
        JLabel tryLabel = Mnemonics.apply(new JLabel(), tr("keyboard.try"));
        tryLabel.setLabelFor(tryField);
        form.add(tryLabel, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(tryField, c);
        add(form, BorderLayout.SOUTH);

        variant.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { changed(); }
            public void removeUpdate(DocumentEvent e) { changed(); }
            public void changedUpdate(DocumentEvent e) { changed(); }
            private void changed() {
                settings.setKeyboardVariant(variant.getText());
                apply();
            }
        });
        list.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            KeyboardLayouts.Layout chosen = list.getSelectedValue();
            if (chosen != null) {
                settings.setKeyboardLayout(chosen.code());
                if (chosenOnce) apply();
                chosenOnce = true;
            }
        });
        select(all, settings.keyboardLayout());
    }

    @Override
    public void onEnter() {
        // The first visit proposes the layout of the live session, else the one usual for the language.
        if (!chosenOnce) {
            String code = layouts.current().orElse(KeyboardLayouts.defaultFor(settings.language()));
            select(((DefaultListModel<KeyboardLayouts.Layout>) list.getModel()).elements().asIterator(), code);
        }
        list.requestFocusInWindow();
    }

    private void select(Iterable<KeyboardLayouts.Layout> all, String code) {
        for (KeyboardLayouts.Layout layout : all) {
            if (layout.code().equals(code)) {
                list.setSelectedValue(layout, true);
                return;
            }
        }
    }

    private void select(java.util.Iterator<KeyboardLayouts.Layout> all, String code) {
        select(() -> all, code);
    }

    private void apply() {
        if (InstallSettings.isValidKeyboardVariant(settings.keyboardVariant())) {
            layouts.apply(settings.keyboardLayout(), settings.keyboardVariant());
        }
    }

    @Override
    public Optional<String> leaveProblem() {
        if (list.getSelectedValue() == null) return Optional.of(tr("keyboard.problem.none"));
        if (!InstallSettings.isValidKeyboardVariant(settings.keyboardVariant())) return Optional.of(tr("keyboard.problem.variant"));
        return Optional.empty();
    }

    @Override
    public String titleKey() {
        return "step.keyboard";
    }
}
