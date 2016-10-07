package de.unijena.bioinf.sirius.gui.dialogs;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 06.10.16.
 */

import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.mainframe.settings.ErrorReportSettingsPanel;
import de.unijena.bioinf.sirius.gui.mainframe.settings.GerneralSettingsPanel;
import de.unijena.bioinf.sirius.gui.mainframe.settings.ProxySettingsPanel;
import de.unijena.bioinf.sirius.gui.mainframe.settings.SettingsPanel;
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
        super(owner, true);
//=============NORTH =================
        nuProps = ApplicationCore.getUserCopyOfUserProperties();
        setTitle(ApplicationCore.VERSION_STRING + " - Settings");
        this.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.PAGE_AXIS));
        this.setLayout(new BorderLayout());
        setMinimumSize(new Dimension(400, getMinimumSize().height));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel l = new JLabel();
        l.setIcon(new ImageIcon(MainFrame.class.getResource("/icons/circular-icons/c-gear.png")));
        header.add(l);
        JLabel intro = new JLabel(/*"Settings"*/);
//        intro.setFont(intro.getFont().deriveFont(48f));
        header.add(intro);
        add(header, BorderLayout.NORTH);


        //============= CENTER =================
        settingsPane = new JTabbedPane();
        genSettings = new GerneralSettingsPanel(nuProps);
        settingsPane.add(genSettings.name(), genSettings);

        proxSettings = new ProxySettingsPanel(nuProps);
        settingsPane.add(proxSettings.name(), proxSettings);

        errorSettings = new ErrorReportSettingsPanel(nuProps);
        settingsPane.add(errorSettings.name(), errorSettings);

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
