package org.datazuul.amadeos.installer.ui;

import org.datazuul.amadeos.installer.domain.InstallSettings;
import org.datazuul.amadeos.installer.service.InstallRunner;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.Optional;

/** Runs amadeos-install and shows its steps, the copy progress and the log. */
final class ProgressPage extends WizardPage implements InstallRunner.Listener {
    private final InstallRunner runner;
    private final JLabel step = new JLabel(" ");
    private final JProgressBar bar = new JProgressBar(0, 100);
    private final JTextArea log = new JTextArea();
    private int currentStep;
    private int totalSteps = 10;
    private boolean started;
    private boolean finished;
    private boolean succeeded;

    ProgressPage(InstallerWindow window, InstallSettings settings, InstallRunner runner) {
        super(window, settings);
        this.runner = runner;
        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.add(paragraph(tr("progress.intro")), BorderLayout.NORTH);
        step.setFont(step.getFont().deriveFont(Font.BOLD));
        top.add(step, BorderLayout.CENTER);
        bar.setStringPainted(true);
        top.add(bar, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(log);
        scroll.setBorder(BorderFactory.createTitledBorder(tr("progress.log")));
        add(scroll, BorderLayout.CENTER);
    }

    @Override
    public void onEnter() {
        if (started) return;
        started = true;
        window.setLocked(true);
        bar.setIndeterminate(true);
        step.setText(tr("progress.starting"));
        runner.start(settings, this);
    }

    @Override
    public void step(int number, int total, String id) {
        currentStep = number;
        if (total > 0) totalSteps = total;
        String task = tr("progress.task." + id);
        step.setText(tr("progress.step", number, totalSteps, task));
        bar.setIndeterminate(false);
        setOverall(0);
    }

    @Override
    public void percent(int percent) {
        setOverall(percent);
    }

    private void setOverall(int percentOfStep) {
        int overall = ((Math.max(currentStep, 1) - 1) * 100 + percentOfStep) / Math.max(totalSteps, 1);
        bar.setValue(overall);
        bar.setString(overall + " %");
    }

    @Override
    public void line(String text) {
        log.append(text + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    @Override
    public void finished(InstallRunner.Result result) {
        finished = true;
        succeeded = result.success();
        window.setLocked(false);
        bar.setIndeterminate(false);
        if (result.success()) {
            bar.setValue(100);
            bar.setString("100 %");
            window.jumpTo(FinishPage.class);
        } else {
            step.setText(tr("progress.failed", result.error().orElse(tr("progress.failed.exit", result.exitCode()))));
            step.setForeground(new Color(0xA0, 0x10, 0x10));
            window.showProblem(tr("progress.failed", result.error().orElse("")));
            started = false;  // Back to the summary and Next again retries
        }
    }

    @Override
    public boolean canGoBack() {
        return finished && !succeeded;
    }

    @Override
    public String cancelKey() {
        return "nav.close";
    }

    @Override
    public Optional<String> leaveProblem() {
        if (finished && succeeded) return Optional.empty();
        return Optional.of(tr(finished ? "progress.problem.failed" : "progress.problem.running"));
    }

    @Override
    public String titleKey() {
        return "step.progress";
    }
}
