package eu.amadeos.installer.ui;

import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.installer.domain.InstallSettings;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Optional;

/** The first user (full name, login name, password) and the computer's name. */
final class UserPage extends WizardPage {
    private final JTextField fullName = new JTextField(24);
    private final JTextField username = new JTextField(24);
    private final JPasswordField password = new JPasswordField(24);
    private final JPasswordField repeat = new JPasswordField(24);
    private final JTextField hostname = new JTextField(24);
    private boolean usernameEdited;

    UserPage(InstallerWindow window, InstallSettings settings) {
        super(window, settings);
        add(paragraph(tr("user.intro")), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        fullName.setText(settings.fullName());
        username.setText(settings.username());
        usernameEdited = !settings.username().isEmpty()
                && !settings.username().equals(InstallSettings.proposeUsername(settings.fullName()));
        password.setText(settings.password());
        repeat.setText(settings.password());
        hostname.setText(settings.hostname());
        row(form, c, "user.fullname", fullName);
        row(form, c, "user.username", username);
        row(form, c, "user.password", password);
        row(form, c, "user.repeat", repeat);
        JCheckBox show = Mnemonics.apply(new JCheckBox(), tr("user.show"));
        char echo = password.getEchoChar();
        show.addActionListener(e -> {
            password.setEchoChar(show.isSelected() ? (char) 0 : echo);
            repeat.setEchoChar(show.isSelected() ? (char) 0 : echo);
        });
        c.gridx = 1;
        c.insets = new Insets(0, 0, 10, 0);
        c.anchor = GridBagConstraints.LINE_START;
        form.add(show, c);
        c.gridy++;
        row(form, c, "user.hostname", hostname);
        JPanel holder = new JPanel(new BorderLayout());
        holder.add(form, BorderLayout.NORTH);
        add(holder, BorderLayout.CENTER);

        fullName.getDocument().addDocumentListener(onChange(() -> {
            settings.setFullName(fullName.getText());
            if (!usernameEdited) username.setText(InstallSettings.proposeUsername(fullName.getText()));
        }));
        username.getDocument().addDocumentListener(onChange(() -> {
            settings.setUsername(username.getText());
            if (username.hasFocus()) usernameEdited = !username.getText().isEmpty();
        }));
        password.getDocument().addDocumentListener(onChange(() -> settings.setPassword(new String(password.getPassword()))));
        hostname.getDocument().addDocumentListener(onChange(() -> settings.setHostname(hostname.getText())));
    }

    private void row(JPanel form, GridBagConstraints c, String key, JComponent field) {
        c.gridx = 0;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.LINE_END;
        c.insets = new Insets(3, 0, 3, 8);
        JLabel label = Mnemonics.apply(new JLabel(), tr(key));
        label.setLabelFor(field);
        form.add(label, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(3, 0, 3, 0);
        form.add(field, c);
        c.gridy++;
    }

    private static DocumentListener onChange(Runnable action) {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { action.run(); }
            public void removeUpdate(DocumentEvent e) { action.run(); }
            public void changedUpdate(DocumentEvent e) { action.run(); }
        };
    }

    @Override
    public void onEnter() {
        (fullName.getText().isEmpty() ? fullName : password).requestFocusInWindow();
    }

    @Override
    public Optional<String> leaveProblem() {
        Optional<String> problem = InstallSettings.fullNameProblem(settings.fullName());
        if (problem.isEmpty()) problem = InstallSettings.usernameProblem(settings.username());
        if (problem.isEmpty()) problem = InstallSettings.passwordProblem(settings.password(), new String(repeat.getPassword()));
        if (problem.isEmpty()) problem = InstallSettings.hostnameProblem(settings.hostname());
        return problem.map(key -> tr(key, InstallSettings.MIN_PASSWORD_LENGTH));
    }

    @Override
    public String titleKey() {
        return "step.user";
    }
}
