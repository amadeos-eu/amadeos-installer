package org.datazuul.amadeos.installer.service;

import java.util.Optional;

/** One line of amadeos-install's output, read by the wizard: progress markers or plain log text. */
public sealed interface InstallOutput {
    /** {@code @@step N/TOTAL ID}: a new step begins. */
    record Step(int number, int total, String id) implements InstallOutput {}

    /** {@code @@percent N}: progress of the copy step. */
    record Percent(int percent) implements InstallOutput {}

    /** {@code @@error MESSAGE}: the script stopped. */
    record Error(String message) implements InstallOutput {}

    /** {@code @@done}: the installation is complete. */
    record Done() implements InstallOutput {}

    /** Anything else: for the log. */
    record Text(String text) implements InstallOutput {}

    static InstallOutput parse(String line) {
        if (!line.startsWith("@@")) return new Text(line);
        String rest = line.substring(2);
        if (rest.startsWith("step ")) {
            String[] parts = rest.substring(5).strip().split("\\s+", 2);
            String[] numbers = parts[0].split("/", 2);
            try {
                int number = Integer.parseInt(numbers[0]);
                int total = numbers.length > 1 ? Integer.parseInt(numbers[1]) : 0;
                return new Step(number, total, parts.length > 1 ? parts[1].strip() : "");
            } catch (NumberFormatException e) {
                return new Text(line);
            }
        }
        if (rest.startsWith("percent ")) {
            return percent(rest.substring(8).strip()).<InstallOutput>map(Percent::new).orElse(new Text(line));
        }
        if (rest.startsWith("error")) return new Error(rest.substring(5).strip());
        if (rest.strip().equals("done")) return new Done();
        return new Text(line);
    }

    private static Optional<Integer> percent(String value) {
        try {
            return Optional.of(Math.clamp(Integer.parseInt(value), 0, 100));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
