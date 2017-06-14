package de.unijena.bioinf.sirius.gui.dialogs;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 06.10.16.
 */

import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.gui.settings.ErrorReportSettingsPanel;
import de.unijena.bioinf.sirius.gui.settings.GerneralSettingsPanel;
import de.unijena.bioinf.sirius.gui.settings.ProxySettingsPanel;
import de.unijena.bioinf.sirius.gui.settings.SettingsPanel;
import de.unijena.bioinf.sirius.gui.configs.Icons;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SettingsDialog extends JDialog implements ActionListener {
    private JButton discard, save;
    private final Properties nuProps;
    private ProxySettingsPanel proxSettings;
    private GerneralSettingsPanel genSettings;
    private ErrorReportSettingsPanel errorSettings;
    private JTabbedPane settingsPane;

    public SettingsDialog(Frame owner) {
        this(owner, -1);
    }

    public SettingsDialog(Frame owner, int activeTab) {
        super(owner, true);
        setTitle("Settings");
        setLayout(new BorderLayout());
        nuProps = ApplicationCore.getUserCopyOfUserProperties();

//=============NORTH =================
        JPanel header = new DialogHaeder(Icons.GEAR_64);
        add(header, BorderLayout.NORTH);

//============= CENTER =================
        settingsPane = new JTabbedPane();
        genSettings = new GerneralSettingsPanel(nuProps);
        genSettings.addVerticalGlue();
        settingsPane.add(genSettings.name(), genSettings);

        proxSettings = new ProxySettingsPanel(nuProps);
        settingsPane.add(proxSettings.name(), proxSettings);

        errorSettings = new ErrorReportSettingsPanel(nuProps);
        errorSettings.addVerticalGlue();
        settingsPane.add(errorSettings.name(), errorSettings);

        if (activeTab >= 0 && activeTab < settingsPane.getTabCount())
            settingsPane.setSelectedIndex(activeTab);

        add(settingsPane, BorderLayout.CENTER);

//============= SOUTH =================
        discard = new JButton("Discard");
        discard.addActionListener(this);
        save = new JButton("Save");
        save.addActionListener(this);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(discard);
        buttons.add(save);

        add(buttons, BorderLayout.SOUTH);

        setMinimumSize(new Dimension(350, getMinimumSize().height)); //todo use maximum size of tab panes?
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    private void collectChangedProps() {
        for (Component c : settingsPane.getComponents()) {
            if (c instanceof SettingsPanel) {
                ((SettingsPanel) c).saveProperties();
            }
        }
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == discard) {
            this.dispose();
        } else {
            collectChangedProps();
            new SwingWorker<Integer, String>() {
                @Override
                protected Integer doInBackground() throws Exception {
                    LoggerFactory.getLogger(this.getClass()).info("Saving settings to properties File");
                    ApplicationCore.changeDefaultProptertiesPersistent(nuProps);
                    return 1;
                }
            }.execute();
            this.dispose();
        }
    }
}
