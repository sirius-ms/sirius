package de.unijena.bioinf.ms.gui.dialogs;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 06.10.16.
 */

import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.actions.CheckConnectionAction;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.settings.*;
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
    private AdductSettingsPanel addSettings;
    private ProxySettingsPanel proxSettings;
    private GerneralSettingsPanel genSettings;
    private ErrorReportSettingsPanel errorSettings;
    //    private ILPSettings ilpSettings;
    private JTabbedPane settingsPane;

    public SettingsDialog(Frame owner) {
        this(owner, -1);
    }

    public SettingsDialog(Frame owner, int activeTab) {
        super(owner, true);
        setTitle("Settings");
        setLayout(new BorderLayout());
        nuProps = SiriusProperties.SIRIUS_PROPERTIES_FILE().asProperties();

//=============NORTH =================
        JPanel header = new DialogHaeder(Icons.GEAR_64);
        add(header, BorderLayout.NORTH);

//============= CENTER =================
        settingsPane = new JTabbedPane();
        genSettings = new GerneralSettingsPanel(nuProps);
        genSettings.addVerticalGlue();
        settingsPane.add(genSettings.name(), genSettings);

        addSettings = new AdductSettingsPanel(nuProps);
        settingsPane.add(addSettings.name(), addSettings);

        /*ilpSettings = new ILPSettings(nuProps);
        settingsPane.add(ilpSettings.name(),ilpSettings);*/

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

    private boolean collectChangedProps() {
        boolean restartMessage = false;
        for (Component c : settingsPane.getComponents()) {
            if (c instanceof SettingsPanel) {
                ((SettingsPanel) c).saveProperties();
                restartMessage = restartMessage || ((SettingsPanel) c).restartRequired();
            }
        }

        SiriusProperties.SIRIUS_PROPERTIES_FILE().setProperties(nuProps);

        for (Component c : settingsPane.getComponents()) {
            if (c instanceof SettingsPanel) {
                ((SettingsPanel) c).reloadChanges();
            }
        }

        return restartMessage;
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == discard) {
            this.dispose();
        } else {
            boolean restartMessage = collectChangedProps();
            new SwingWorker<Integer, String>() {
                @Override
                protected Integer doInBackground() throws Exception {
                    LoggerFactory.getLogger(this.getClass()).info("Saving settings to properties File");
                    SiriusProperties.SIRIUS_PROPERTIES_FILE().store();
                    CheckConnectionAction.isConnectedAndLoad();
                    return 1;

                }
            }.execute();
            if (restartMessage)
                new ExceptionDialog(this, "For at least one change you made requires a restart of Sirius.");
            this.dispose();
        }
    }
}
