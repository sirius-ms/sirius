package de.unijena.bioinf.sirius.gui.dialogs;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 06.10.16.
 */

import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.mainframe.settings.ProxySettingsPanel;

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
    public SettingsDialog(Frame owner) {
        super(owner, true);

        nuProps = ApplicationCore.getUserCopyOfUserProperties();
        setTitle(ApplicationCore.VERSION_STRING + " - Settings");
        this.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.PAGE_AXIS));
        setMinimumSize(new Dimension(400, getMinimumSize().height));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel l = new JLabel();
        l.setIcon(new ImageIcon(MainFrame.class.getResource("/icons/new/settings2-64.png")));
        header.add(l);
        JLabel intro = new JLabel(/*"Settings"*/);
//        intro.setFont(intro.getFont().deriveFont(48f));
        header.add(intro);
        add(header);

        proxSettings = new ProxySettingsPanel(nuProps);
        add(proxSettings);


        discard = new JButton("Discard");
        discard.addActionListener(this);
        save = new JButton("Save");
        save.addActionListener(this);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(discard);
        buttons.add(save);

        add(buttons);


        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    private void collectChangedProps(){
        proxSettings.saveProperties();
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == discard){
            this.dispose();
        }else{
            collectChangedProps();
            new SwingWorker<Integer, String>() {
                @Override
                protected Integer doInBackground() throws Exception {
                    ApplicationCore.changeDefaultProptertiesPersistent(nuProps);
                    return 1;
                }
            }.execute();
            this.dispose();
        }
    }
}
