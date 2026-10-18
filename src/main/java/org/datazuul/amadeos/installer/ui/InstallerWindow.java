package org.datazuul.amadeos.installer.ui;

import com.google.inject.Inject;
import org.datazuul.amadeos.commons.i18n.Mnemonics;
import org.datazuul.amadeos.commons.swing.AmadeUI;
import org.datazuul.amadeos.commons.swing.EdtExecutor;
import org.datazuul.amadeos.installer.domain.InstallSettings;
import org.datazuul.amadeos.installer.i18n.Texts;
import org.datazuul.amadeos.installer.service.BlockDevices;
import org.datazuul.amadeos.installer.service.InstallRunner;
import org.datazuul.amadeos.installer.service.KeyboardLayouts;
import org.datazuul.amadeos.installer.service.SystemFacts;
import org.datazuul.amadeos.installer.service.TimeZones;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * The assistant: the list of steps on the left, the current page in the middle, Back/Next/Cancel below.
 * Pages are rebuilt when the language changes on the first page; the settings they hold stay.
 */
public final class InstallerWindow extends JFrame {
    private final InstallSettings settings;
    private final SystemFacts facts;
    private final List<Supplier<WizardPage>> pageFactories = new ArrayList<>();
    private final List<WizardPage> pages = new ArrayList<>();
    private final List<JLabel> stepLabels = new ArrayList<>();
    private final JPanel steps = new JPanel();
    private final JPanel cards = new JPanel(new CardLayout());
    private final JLabel title = new JLabel();
    private final JLabel problem = new JLabel(" ");
    private final JButton back = new JButton();
    private final JButton next = new JButton();
    private final JButton cancel = new JButton();
    private int index;
    private boolean locked;

    @Inject
    public InstallerWindow(InstallSettings settings, SystemFacts facts, BlockDevices devices, TimeZones zones,
                           KeyboardLayouts layouts, InstallRunner runner, ExecutorService executor, EdtExecutor edt) {
        this.settings = settings;
        this.facts = facts;
        settings.setFirmware(facts.firmware());
        pageFactories.add(() -> new LanguagePage(this, settings, facts));
        pageFactories.add(() -> new KeyboardPage(this, settings, layouts));
        pageFactories.add(() -> new TimeZonePage(this, settings, zones));
        pageFactories.add(() -> new DiskPage(this, settings, devices, facts, executor, edt));
        pageFactories.add(() -> new UserPage(this, settings));
        pageFactories.add(() -> new SummaryPage(this, settings));
        pageFactories.add(() -> new ProgressPage(this, settings, runner));
        pageFactories.add(() -> new FinishPage(this, settings));

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancel();
            }
        });

        steps.setLayout(new BoxLayout(steps, BoxLayout.Y_AXIS));
        steps.setBorder(BorderFactory.createCompoundBorder(AmadeUI.sunken(), BorderFactory.createEmptyBorder(10, 12, 10, 16)));
        add(steps, BorderLayout.WEST);

        JPanel center = new JPanel(new BorderLayout());
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 4f));
        title.setBorder(BorderFactory.createEmptyBorder(12, 16, 0, 16));
        center.add(title, BorderLayout.NORTH);
        center.add(cards, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        back.addActionListener(e -> back());
        next.addActionListener(e -> next());
        cancel.addActionListener(e -> cancel());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.add(back);
        buttons.add(next);
        buttons.add(Box.createHorizontalStrut(12));
        buttons.add(cancel);
        problem.setForeground(new Color(0xA0, 0x10, 0x10));
        JPanel bottom = new JPanel(new BorderLayout(12, 4));
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 16, 10, 10));
        bottom.add(new JSeparator(), BorderLayout.NORTH);
        bottom.add(problem, BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
        getRootPane().registerKeyboardAction(e -> cancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        // For development with scripts/fake-install.sh: -Damadeos.installer.page=N starts on page N (0-based).
        index = Math.clamp(Integer.getInteger("amadeos.installer.page", 0), 0, pageFactories.size() - 1);
        rebuild();
        setSize(new Dimension(820, 580));
        setMinimumSize(new Dimension(680, 480));
        setLocationRelativeTo(null);
    }

    /** Creates the pages anew in the current language and shows the current one again. */
    public void rebuild() {
        setTitle(Texts.tr("app.title"));
        cards.removeAll();
        pages.clear();
        steps.removeAll();
        stepLabels.clear();
        for (Supplier<WizardPage> factory : pageFactories) {
            WizardPage page = factory.get();
            pages.add(page);
            cards.add(page, String.valueOf(pages.size() - 1));
            JLabel label = new JLabel(Texts.tr(page.titleKey()));
            label.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
            stepLabels.add(label);
            steps.add(label);
        }
        steps.add(Box.createVerticalGlue());
        getRootPane().setDefaultButton(next);
        show(index);
        steps.revalidate();
        steps.repaint();
    }

    private void show(int newIndex) {
        index = newIndex;
        WizardPage page = pages.get(index);
        ((CardLayout) cards.getLayout()).show(cards, String.valueOf(index));
        title.setText(Texts.tr(page.titleKey()));
        Font plain = stepLabels.get(0).getFont().deriveFont(Font.PLAIN);
        for (int i = 0; i < stepLabels.size(); i++) {
            JLabel label = stepLabels.get(i);
            label.setText((i == index ? "▶ " : "   ") + Texts.tr(pages.get(i).titleKey()));
            label.setFont(i == index ? plain.deriveFont(Font.BOLD) : plain);
            label.setEnabled(i <= index);
        }
        problem.setText(" ");
        Mnemonics.apply(back, Texts.tr("nav.back"));
        Mnemonics.apply(next, Texts.tr(page.nextKey()));
        Mnemonics.apply(cancel, Texts.tr(page.cancelKey()));
        updateButtons();
        page.onEnter();
        revalidate();
        repaint();
    }

    private void updateButtons() {
        WizardPage page = pages.get(index);
        back.setEnabled(!locked && index > 0 && page.canGoBack());
        next.setEnabled(!locked);
        cancel.setEnabled(!locked);
    }

    /** While the installation runs: no navigation, no closing. */
    public void setLocked(boolean locked) {
        this.locked = locked;
        updateButtons();
    }

    public void showProblem(String text) {
        problem.setText(text);
    }

    public void back() {
        if (locked || index == 0 || !pages.get(index).canGoBack()) return;
        show(index - 1);
    }

    public void next() {
        if (locked) return;
        WizardPage page = pages.get(index);
        var trouble = page.leaveProblem();
        if (trouble.isPresent()) {
            problem.setText(trouble.get());
            return;
        }
        if (index + 1 < pages.size()) show(index + 1);
    }

    /** Moves to the given page without checks (used by the progress page when it is done). */
    void jumpTo(Class<? extends WizardPage> type) {
        for (int i = 0; i < pages.size(); i++) {
            if (type.isInstance(pages.get(i))) {
                show(i);
                return;
            }
        }
    }

    public void cancel() {
        if (locked) return;
        WizardPage page = pages.get(index);
        if (page instanceof FinishPage || page instanceof ProgressPage) {
            dispose();
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this, Texts.tr("nav.cancel.confirm"), Texts.tr("app.title"),
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) dispose();
    }

    /** Switches the whole wizard to the language; called by the welcome page. */
    public void changeLanguage(Locale locale) {
        Locale.setDefault(locale);
        JComponent.setDefaultLocale(locale);
        SwingUtilities.invokeLater(this::rebuild);
    }

    InstallSettings settings() {
        return settings;
    }

    SystemFacts facts() {
        return facts;
    }
}
