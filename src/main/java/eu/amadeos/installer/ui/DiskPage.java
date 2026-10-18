package eu.amadeos.installer.ui;

import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.commons.swing.EdtExecutor;
import eu.amadeos.installer.domain.BlockDevice;
import eu.amadeos.installer.domain.Firmware;
import eu.amadeos.installer.domain.InstallSettings;
import eu.amadeos.installer.domain.TargetMode;
import eu.amadeos.installer.service.BlockDevices;
import eu.amadeos.installer.service.SystemFacts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/** The target: a whole disk (erased), or an existing partition. */
final class DiskPage extends WizardPage {
    private static final Logger LOG = LoggerFactory.getLogger(DiskPage.class);
    private static final String[] SWAP_SIZES = {InstallSettings.NO_SWAP, "2G", "4G", "8G", "16G"};

    private final BlockDevices devices;
    private final SystemFacts facts;
    private final ExecutorService executor;
    private final EdtExecutor edt;
    private final JRadioButton wholeDisk = new JRadioButton();
    private final JRadioButton partition = new JRadioButton();
    private final JComboBox<BlockDevice> disks = new JComboBox<>();
    private final JComboBox<String> swap = new JComboBox<>(SWAP_SIZES);
    private final JComboBox<BlockDevice> partitions = new JComboBox<>();
    private final JComboBox<Object> efiPartitions = new JComboBox<>();
    private final JLabel status = new JLabel(" ");
    private final JLabel efiLabel = new JLabel();
    private List<BlockDevice> all = List.of();
    private boolean loadedOnce;

    DiskPage(InstallerWindow window, InstallSettings settings, BlockDevices devices, SystemFacts facts,
             ExecutorService executor, EdtExecutor edt) {
        super(window, settings);
        this.devices = devices;
        this.facts = facts;
        this.executor = executor;
        this.edt = edt;

        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.add(paragraph(tr("disk.intro")), BorderLayout.NORTH);
        top.add(paragraph(tr(settings.firmware() == Firmware.UEFI ? "disk.firmware.uefi" : "disk.firmware.bios")),
                BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.weightx = 1;
        c.insets = new Insets(6, 0, 0, 0);
        ButtonGroup group = new ButtonGroup();
        group.add(wholeDisk);
        group.add(partition);
        Mnemonics.apply(wholeDisk, tr("disk.whole"));
        form.add(wholeDisk, c);
        c.gridy++;
        c.insets = new Insets(0, 24, 4, 0);
        form.add(paragraph(tr("disk.whole.detail")), c);
        c.gridy++;
        row(form, c, tr("disk.disk"), disks);
        row(form, c, tr("disk.swap"), swap);
        c.gridx = 0;
        c.gridwidth = 2;
        c.insets = new Insets(14, 0, 0, 0);
        Mnemonics.apply(partition, tr("disk.partition"));
        form.add(partition, c);
        c.gridy++;
        c.insets = new Insets(0, 24, 4, 0);
        form.add(paragraph(tr("disk.partition.detail")), c);
        c.gridy++;
        row(form, c, tr("disk.root"), partitions);
        efiLabel.setText(tr("disk.efi"));
        c.gridx = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        c.insets = new Insets(2, 24, 2, 8);
        form.add(efiLabel, c);
        c.gridx = 1;
        c.weightx = 1;
        c.insets = new Insets(2, 0, 2, 0);
        form.add(efiPartitions, c);
        c.gridy++;
        boolean uefi = settings.firmware() == Firmware.UEFI;
        efiLabel.setVisible(uefi);
        efiPartitions.setVisible(uefi);
        // In a holder, so that the form stays at the top instead of being centred vertically.
        JPanel holder = new JPanel(new BorderLayout());
        holder.add(form, BorderLayout.NORTH);
        add(holder, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(12, 0));
        bottom.add(status, BorderLayout.CENTER);
        JButton refresh = Mnemonics.apply(new JButton(), tr("disk.refresh"));
        refresh.addActionListener(e -> load());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttons.add(refresh);
        bottom.add(buttons, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        disks.setRenderer(new DeviceRenderer());
        partitions.setRenderer(new DeviceRenderer());
        efiPartitions.setRenderer(new DeviceRenderer());
        swap.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                                                          boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (InstallSettings.NO_SWAP.equals(value)) setText(tr("disk.swap.none"));
                else if (value != null) setText(value.toString().replace("G", " GB"));
                return this;
            }
        });

        wholeDisk.addActionListener(e -> modeChanged());
        partition.addActionListener(e -> modeChanged());
        (settings.targetMode() == TargetMode.WHOLE_DISK ? wholeDisk : partition).setSelected(true);
        swap.setSelectedItem(settings.swap());
        swap.addActionListener(e -> settings.setSwap((String) swap.getSelectedItem()));
        disks.addActionListener(e -> settings.setDisk((BlockDevice) disks.getSelectedItem()));
        partitions.addActionListener(e -> {
            settings.setRootPartition((BlockDevice) partitions.getSelectedItem());
            fillEfi();
        });
        efiPartitions.addActionListener(e -> settings.setEfiPartition(
                efiPartitions.getSelectedItem() instanceof BlockDevice device ? device : null));
        modeChanged();
    }

    private void row(JPanel form, GridBagConstraints c, String labelText, JComboBox<?> field) {
        c.gridx = 0;
        c.gridwidth = 1;
        c.weightx = 0;
        c.insets = new Insets(2, 24, 2, 8);
        JLabel label = Mnemonics.apply(new JLabel(), labelText);
        label.setLabelFor(field);
        form.add(label, c);
        c.gridx = 1;
        c.weightx = 1;
        c.insets = new Insets(2, 0, 2, 0);
        form.add(field, c);
        c.gridy++;
    }

    private void modeChanged() {
        boolean whole = wholeDisk.isSelected();
        settings.setTargetMode(whole ? TargetMode.WHOLE_DISK : TargetMode.PARTITION);
        disks.setEnabled(whole);
        swap.setEnabled(whole);
        partitions.setEnabled(!whole);
        efiPartitions.setEnabled(!whole);
    }

    @Override
    public void onEnter() {
        if (!loadedOnce) load();
    }

    private void load() {
        status.setText(tr("disk.loading"));
        CompletableFuture.supplyAsync(() -> {
            try {
                List<BlockDevice> list = devices.list();
                Optional<String> live = devices.liveMediumDisk(list);
                return new Loaded(list, devices.installableDisks(list, live), live);
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }, executor).whenCompleteAsync((loaded, error) -> {
            loadedOnce = true;
            if (error != null) {
                LOG.error("Listing the disks failed", error);
                status.setText(tr("disk.error", error.getCause() != null ? error.getCause().getMessage() : error.getMessage()));
                return;
            }
            show(loaded);
        }, edt);
    }

    private record Loaded(List<BlockDevice> all, List<BlockDevice> installable, Optional<String> liveDisk) {}

    private void show(Loaded loaded) {
        all = loaded.all();
        BlockDevice previousDisk = settings.disk().orElse(null);
        disks.setModel(new DefaultComboBoxModel<>(loaded.installable().toArray(BlockDevice[]::new)));
        if (previousDisk != null) selectByPath(disks, previousDisk.path());
        settings.setDisk((BlockDevice) disks.getSelectedItem());

        // Partitions: of every disk except the live medium, large enough and not mounted.
        List<BlockDevice> candidates = new ArrayList<>();
        for (BlockDevice disk : all) {
            if (!disk.isDisk() || disk.readOnly() || disk.name().startsWith("loop") || disk.name().startsWith("zram")) continue;
            if (loaded.liveDisk().isPresent() && loaded.liveDisk().get().equals(disk.path())) continue;
            for (BlockDevice part : devices.partitionsOf(disk, all)) {
                if (part.size() >= BlockDevices.MIN_DISK_BYTES) candidates.add(part);
            }
        }
        BlockDevice previousPartition = settings.rootPartition().orElse(null);
        partitions.setModel(new DefaultComboBoxModel<>(candidates.toArray(BlockDevice[]::new)));
        if (previousPartition != null) selectByPath(partitions, previousPartition.path());
        settings.setRootPartition((BlockDevice) partitions.getSelectedItem());
        fillEfi();

        if (!loaded.installable().isEmpty() && settings.swap().equals(InstallSettings.NO_SWAP) && !swapChosen) {
            swap.setSelectedItem(proposedSwap());
        }
        status.setText(loaded.installable().isEmpty() && candidates.isEmpty()
                ? tr("disk.none", BlockDevice.sizeText(BlockDevices.MIN_DISK_BYTES)) : " ");
    }

    private boolean swapChosen;

    /** Swap the size of the memory, rounded up to one of the offered sizes; none when the memory is unknown. */
    private String proposedSwap() {
        swapChosen = true;
        Optional<Long> memory = facts.memoryBytes();
        if (memory.isEmpty()) return InstallSettings.NO_SWAP;
        double gib = memory.get() / (1024.0 * 1024 * 1024);
        for (String size : SWAP_SIZES) {
            if (size.equals(InstallSettings.NO_SWAP)) continue;
            if (Integer.parseInt(size.replace("G", "")) >= gib) return size;
        }
        return SWAP_SIZES[SWAP_SIZES.length - 1];
    }

    private void fillEfi() {
        if (settings.firmware() != Firmware.UEFI) return;
        BlockDevice root = (BlockDevice) partitions.getSelectedItem();
        List<Object> items = new ArrayList<>();
        items.add(tr("disk.efi.auto"));
        if (root != null) {
            for (BlockDevice device : all) {
                if (device.isPartition() && device.parentName().equals(root.parentName()) && device.isEfiSystem()
                        && !device.path().equals(root.path())) {
                    items.add(device);
                }
            }
        }
        BlockDevice previous = settings.efiPartition().orElse(null);
        efiPartitions.setModel(new DefaultComboBoxModel<>(items.toArray()));
        if (previous != null) selectByPath(efiPartitions, previous.path());
        settings.setEfiPartition(efiPartitions.getSelectedItem() instanceof BlockDevice d ? d : null);
    }

    private static void selectByPath(JComboBox<?> combo, String path) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i) instanceof BlockDevice device && device.path().equals(path)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    @Override
    public Optional<String> leaveProblem() {
        if (settings.targetMode() == TargetMode.WHOLE_DISK) {
            return settings.disk().isPresent() ? Optional.empty() : Optional.of(tr("disk.problem.none"));
        }
        return settings.rootPartition().isPresent() ? Optional.empty() : Optional.of(tr("disk.problem.partition"));
    }

    @Override
    public String titleKey() {
        return "step.disk";
    }

    /** A disk as "Model (sda, SATA, 500 GB)", a partition as "sda2 (ext4 "Data", 120 GB)". */
    private static final class DeviceRenderer extends javax.swing.DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                                                      boolean selected, boolean focus) {
            super.getListCellRendererComponent(list, value, index, selected, focus);
            if (value instanceof BlockDevice device) {
                if (device.isDisk()) {
                    setText(device.displayName());
                } else {
                    StringBuilder text = new StringBuilder(device.name()).append(" (");
                    if (!device.fsType().isBlank()) text.append(device.fsType()).append(' ');
                    if (!device.label().isBlank()) text.append('"').append(device.label()).append("\" ");
                    text.append(device.sizeText()).append(')');
                    setText(text.toString());
                }
            }
            return this;
        }
    }
}
