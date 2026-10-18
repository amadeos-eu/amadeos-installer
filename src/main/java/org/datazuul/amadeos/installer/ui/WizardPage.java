package org.datazuul.amadeos.installer.ui;

import org.datazuul.amadeos.installer.domain.InstallSettings;
import org.datazuul.amadeos.installer.i18n.Texts;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Optional;

/** One page of the wizard. Pages read and write {@link InstallSettings}; the window moves between them. */
public abstract class WizardPage extends JPanel {
    protected final InstallerWindow window;
    protected final InstallSettings settings;

    protected WizardPage(InstallerWindow window, InstallSettings settings) {
        super(new BorderLayout(0, 10));
        this.window = window;
        this.settings = settings;
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
    }

    /** Key of the page's name in the step list and title ({@code step.*}). */
    public abstract String titleKey();

    /** Key of the text of the Next button. */
    public String nextKey() {
        return "nav.next";
    }

    /** Key of the text of the Cancel button. */
    public String cancelKey() {
        return "nav.cancel";
    }

    public boolean canGoBack() {
        return true;
    }

    /** Called when the page is shown. */
    public void onEnter() {
    }

    /** The problem that keeps the user on this page, as a text, or empty when Next may proceed. */
    public Optional<String> leaveProblem() {
        return Optional.empty();
    }

    /** A paragraph of explanation, wrapped, in the colours of the panel. */
    protected static JTextArea paragraph(String text) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setBorder(null);
        area.setFont(new JLabel().getFont());
        area.setForeground(new JLabel().getForeground());
        return area;
    }

    protected static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, label.getFont().getSize2D() + 3f));
        return label;
    }

    protected static String tr(String key, Object... arguments) {
        return Texts.tr(key, arguments);
    }
}
