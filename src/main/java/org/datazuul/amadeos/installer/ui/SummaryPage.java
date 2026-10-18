package org.datazuul.amadeos.installer.ui;

import org.datazuul.amadeos.commons.i18n.Mnemonics;
import org.datazuul.amadeos.installer.domain.BlockDevice;
import org.datazuul.amadeos.installer.domain.Firmware;
import org.datazuul.amadeos.installer.domain.InstallSettings;
import org.datazuul.amadeos.installer.domain.TargetMode;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Optional;

/** Everything that was chosen, the warning, and the confirmation that the data may be lost. */
final class SummaryPage extends WizardPage {
    private final JPanel rows = new JPanel(new GridBagLayout());
    private final JTextArea warning = paragraph("");
    private final JCheckBox confirm = new JCheckBox();

    SummaryPage(InstallerWindow window, InstallSettings settings) {
        super(window, settings);
        add(paragraph(tr("summary.intro")), BorderLayout.NORTH);
        JPanel holder = new JPanel(new BorderLayout());
        holder.add(rows, BorderLayout.NORTH);
        add(holder, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        warning.setForeground(new Color(0xA0, 0x10, 0x10));
        warning.setFont(warning.getFont().deriveFont(Font.BOLD));
        bottom.add(warning, BorderLayout.NORTH);
        bottom.add(confirm, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    @Override
    public void onEnter() {
        rows.removeAll();
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        row(c, tr("summary.language"), settings.language().nativeName());
        row(c, tr("summary.keyboard"), settings.keyboardLayout()
                + (settings.keyboardVariant().isEmpty() ? "" : " (" + settings.keyboardVariant() + ")"));
        row(c, tr("summary.timezone"), settings.timeZone());
        String targetText;
        if (settings.targetMode() == TargetMode.WHOLE_DISK) {
            String swapText = settings.swap().equals(InstallSettings.NO_SWAP) ? "" : tr("summary.target.swap", settings.swap().replace("G", " GB"));
            targetText = tr("summary.target.whole", settings.disk().map(BlockDevice::displayName).orElse("?"), swapText);
        } else {
            targetText = tr("summary.target.partition", settings.rootPartition().map(BlockDevice::path).orElse("?"));
            if (settings.firmware() == Firmware.UEFI) {
                targetText += "\n" + tr("summary.target.efi", settings.efiPartition().map(BlockDevice::path).orElse(tr("disk.efi.auto")));
            }
        }
        row(c, tr("summary.target"), targetText);
        row(c, tr("summary.firmware"), tr(settings.firmware() == Firmware.UEFI ? "summary.firmware.uefi" : "summary.firmware.bios"));
        row(c, tr("summary.hostname"), settings.hostname());
        row(c, tr("summary.user"), settings.username() + (settings.fullName().isEmpty() ? "" : " (" + settings.fullName() + ")"));
        rows.revalidate();
        rows.repaint();

        String device = settings.target().map(BlockDevice::path).orElse("?");
        warning.setText(tr(settings.targetMode() == TargetMode.WHOLE_DISK ? "summary.warning.whole" : "summary.warning.partition", device));
        Mnemonics.apply(confirm, tr("summary.confirm", device));
        confirm.setSelected(false);
    }

    private void row(GridBagConstraints c, String label, String value) {
        c.gridx = 0;
        c.weightx = 0;
        c.anchor = GridBagConstraints.FIRST_LINE_END;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(3, 0, 3, 12);
        JLabel name = new JLabel(label + ":");
        rows.add(name, c);
        c.gridx = 1;
        c.weightx = 1;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 0, 3, 0);
        JTextArea text = paragraph(value);
        text.setFont(text.getFont().deriveFont(Font.BOLD));
        rows.add(text, c);
        c.gridy++;
    }

    @Override
    public String nextKey() {
        return "nav.install";
    }

    @Override
    public Optional<String> leaveProblem() {
        return confirm.isSelected() ? Optional.empty() : Optional.of(tr("summary.problem.confirm"));
    }

    @Override
    public String titleKey() {
        return "step.summary";
    }
}
