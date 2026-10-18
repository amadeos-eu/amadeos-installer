package eu.amadeos.installer.ui;

import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.installer.domain.InstallSettings;
import eu.amadeos.installer.service.TimeZones;

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
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** The time zone, from the same list as the login greeter, with a filter field. */
final class TimeZonePage extends WizardPage {
    private final List<String> zones;
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);
    private final JTextField filter = new JTextField();
    private boolean entered;
    private final TimeZones timeZones;

    TimeZonePage(InstallerWindow window, InstallSettings settings, TimeZones timeZones) {
        super(window, settings);
        this.timeZones = timeZones;
        this.zones = timeZones.available();
        add(paragraph(tr("timezone.intro")), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 6));
        JPanel filterRow = new JPanel(new BorderLayout(8, 0));
        JLabel filterLabel = Mnemonics.apply(new JLabel(), tr("timezone.filter"));
        filterLabel.setLabelFor(filter);
        filterRow.add(filterLabel, BorderLayout.WEST);
        filterRow.add(filter, BorderLayout.CENTER);
        center.add(filterRow, BorderLayout.NORTH);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        center.add(new JScrollPane(list), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        filter.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refill(); }
            public void removeUpdate(DocumentEvent e) { refill(); }
            public void changedUpdate(DocumentEvent e) { refill(); }
        });
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && list.getSelectedValue() != null) settings.setTimeZone(list.getSelectedValue());
        });
        refill();
    }

    private void refill() {
        String needle = filter.getText().strip().toLowerCase(Locale.ROOT).replace(' ', '_');
        String selected = settings.timeZone();
        model.clear();
        for (String zone : zones) {
            if (needle.isEmpty() || zone.toLowerCase(Locale.ROOT).contains(needle)) model.addElement(zone);
        }
        if (model.contains(selected)) {
            list.setSelectedValue(selected, true);
        } else if (model.getSize() == 1) {
            list.setSelectedIndex(0);
        }
    }

    @Override
    public void onEnter() {
        if (!entered) {
            entered = true;
            timeZones.current().filter(zones::contains).ifPresent(zone -> {
                settings.setTimeZone(zone);
                list.setSelectedValue(zone, true);
            });
        }
        filter.requestFocusInWindow();
    }

    @Override
    public Optional<String> leaveProblem() {
        return zones.contains(settings.timeZone()) ? Optional.empty() : Optional.of(tr("timezone.problem.none"));
    }

    @Override
    public String titleKey() {
        return "step.timezone";
    }
}
