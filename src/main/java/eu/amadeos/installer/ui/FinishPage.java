package eu.amadeos.installer.ui;

import eu.amadeos.installer.domain.BlockDevice;
import eu.amadeos.installer.domain.InstallSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.io.IOException;
import java.util.Optional;

/** Done: restart into the installed system, or close and keep using the live system. */
final class FinishPage extends WizardPage {
    private static final Logger LOG = LoggerFactory.getLogger(FinishPage.class);
    private final JTextArea text = paragraph("");

    FinishPage(InstallerWindow window, InstallSettings settings) {
        super(window, settings);
        add(text, BorderLayout.NORTH);
    }

    @Override
    public void onEnter() {
        text.setText(tr("finish.intro", settings.target().map(BlockDevice::path).orElse("?")));
    }

    @Override
    public boolean canGoBack() {
        return false;
    }

    @Override
    public String nextKey() {
        return "nav.restart";
    }

    @Override
    public String cancelKey() {
        return "nav.close";
    }

    /** Next means restart here; the problem text is shown only when that fails. */
    @Override
    public Optional<String> leaveProblem() {
        try {
            Process process = new ProcessBuilder("systemctl", "reboot").redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes());
            int exit = process.waitFor();
            if (exit != 0) return Optional.of(tr("finish.restart.failed", output.strip().isEmpty() ? "exit " + exit : output.strip()));
            window.dispose();
            return Optional.empty();
        } catch (IOException e) {
            LOG.error("Restart failed", e);
            return Optional.of(tr("finish.restart.failed", e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.of(tr("finish.restart.failed", "interrupted"));
        }
    }

    @Override
    public String titleKey() {
        return "step.finish";
    }
}
