package de.unijena.bioinf.sirius.gui.dialogs;

import de.unijena.bioinf.sirius.gui.fingerid.WebAPI;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class UpdateDialog extends JDialog implements ActionListener{

    JButton ignore, download;

    public UpdateDialog(Frame owner, String version) {
        super(owner, "Update for SIRIUS is available", ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout());
        final JLabel label = new JLabel("<html>There is a new version of SIRIUS available.<br> Update to <b>SIRIUS " + version + "</b> to receive the newest upgrades.<br> Your current version is " + WebAPI.VERSION + "<br>To avoid compatibility issues with the CSI:FingerId webservice,<br> the CSI:FingerId search is disabled in this outdated version of SIRIUS.</html>");
        label.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
        add(label, BorderLayout.CENTER);
        final JPanel subpanel = new JPanel(new FlowLayout());
        ignore = new JButton("Ignore update");
        download = new JButton("Download SIRIUS " + version);
        subpanel.add(download);
        subpanel.add(ignore);
        subpanel.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
        add(subpanel, BorderLayout.SOUTH);
        download.addActionListener(this);
        ignore.addActionListener(this);
        pack();
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource()==ignore) {
        } else if (e.getSource()==download) {
            try {
                Desktop.getDesktop().browse(new URI(WebAPI.SIRIUS_DOWNLOAD));
            } catch (IOException | URISyntaxException e1) {
                LoggerFactory.getLogger(this.getClass()).error(e1.getMessage(),e1);
            }
        }
        this.dispose();
    }
}
