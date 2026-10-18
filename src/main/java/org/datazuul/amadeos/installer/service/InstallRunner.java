package org.datazuul.amadeos.installer.service;

import com.google.inject.Inject;
import org.datazuul.amadeos.commons.swing.EdtExecutor;
import org.datazuul.amadeos.installer.domain.InstallSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * Runs {@code amadeos-install --config FILE --yes} as root (through pkexec unless the wizard is root already)
 * and reports its progress lines on the EDT. {@code AMADEOS_INSTALL_COMMAND} in the environment replaces the
 * command, for trying the wizard with {@code scripts/fake-install.sh}.
 */
public class InstallRunner {
    private static final Logger LOG = LoggerFactory.getLogger(InstallRunner.class);
    public static final String INSTALL_SCRIPT = "/usr/sbin/amadeos-install";
    public static final String COMMAND_VARIABLE = "AMADEOS_INSTALL_COMMAND";

    /** Receives the progress, on the EDT. */
    public interface Listener {
        void step(int number, int total, String id);
        void percent(int percent);
        void line(String text);
        /** Once, at the end: success, or the message of the failure. */
        void finished(Result result);
    }

    public record Result(boolean success, Optional<String> error, int exitCode) {
        public static Result ok() {
            return new Result(true, Optional.empty(), 0);
        }

        public static Result failed(String error, int exitCode) {
            return new Result(false, Optional.of(error), exitCode);
        }
    }

    private final SystemFacts facts;
    private final ExecutorService executor;
    private final EdtExecutor edt;

    @Inject
    public InstallRunner(SystemFacts facts, ExecutorService executor, EdtExecutor edt) {
        this.facts = facts;
        this.executor = executor;
        this.edt = edt;
    }

    /** The command without the configuration file. */
    List<String> command() {
        String override = System.getenv(COMMAND_VARIABLE);
        if (override != null && !override.isBlank()) return new ArrayList<>(List.of(override.strip().split("\\s+")));
        return facts.isRoot() ? new ArrayList<>(List.of(INSTALL_SCRIPT))
                : new ArrayList<>(List.of("pkexec", INSTALL_SCRIPT));
    }

    /** Starts the installation in the background; {@code listener} hears from it on the EDT. */
    public void start(InstallSettings settings, Listener listener) {
        executor.execute(() -> run(settings, listener));
    }

    private void run(InstallSettings settings, Listener listener) {
        Path config = null;
        Result result;
        try {
            config = ConfigFile.write(settings.toConfig());
            List<String> command = command();
            command.addAll(List.of("--config", config.toString(), "--yes"));
            LOG.info("Starting {}", String.join(" ", command));
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String lastError = null;
            boolean done = false;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOG.debug("install: {}", line);
                    switch (InstallOutput.parse(line)) {
                        case InstallOutput.Step step -> edt.execute(() -> listener.step(step.number(), step.total(), step.id()));
                        case InstallOutput.Percent percent -> edt.execute(() -> listener.percent(percent.percent()));
                        case InstallOutput.Error error -> {
                            lastError = error.message();
                            String text = line;
                            edt.execute(() -> listener.line(text));
                        }
                        case InstallOutput.Done ignored -> done = true;
                        case InstallOutput.Text text -> edt.execute(() -> listener.line(text.text()));
                    }
                }
            }
            int exit = process.waitFor();
            if (exit == 0 && done) {
                result = Result.ok();
            } else {
                result = Result.failed(lastError != null ? lastError : exitMessage(exit), exit);
            }
        } catch (IOException | RuntimeException e) {
            LOG.error("amadeos-install could not be started", e);
            result = Result.failed(e.getMessage(), -1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result = Result.failed("interrupted", -1);
        } finally {
            if (config != null) {
                try {
                    Files.deleteIfExists(config);
                } catch (IOException e) {
                    LOG.warn("Cannot delete {}", config, e);
                }
            }
        }
        Result finalResult = result;
        edt.execute(() -> listener.finished(finalResult));
    }

    private static String exitMessage(int exit) {
        // pkexec's own codes: 126 when the user dismissed the authentication, 127 when it was refused.
        return switch (exit) {
            case 126 -> "not authorized (authentication dismissed)";
            case 127 -> "not authorized (polkit refused)";
            default -> "exit code " + exit;
        };
    }
}
