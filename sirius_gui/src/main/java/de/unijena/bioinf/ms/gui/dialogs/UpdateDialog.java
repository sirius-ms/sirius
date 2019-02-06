package de.unijena.bioinf.ms.gui.dialogs;

import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.fingerid.webapi.VersionsInfo;
import de.unijena.bioinf.fingerid.utils.FingerIDProperties;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class UpdateDialog extends JDialog implements ActionListener {

    JButton ignore, download;

    public UpdateDialog(Frame owner, VersionsInfo version) {
        super(owner, "Update for SIRIUS is available", ModalityType.APPLICATION_MODAL);
        this.setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        StringBuilder message = new StringBuilder();
        message.append("<html>There is a new version of SIRIUS available.<br> Update to <b>SIRIUS ")
                .append(version.siriusGuiVersion)
                .append("</b> to receive the newest upgrades.<br> Your current version is ")
                .append(FingerIDProperties.sirius_guiVersion())
                .append("<br>");
        if (version.finishJobs()) {
            if (version.acceptJobs()) {
                message.append("The CSI:FingerID webservice will accept jobs from your current version until <b>")
                        .append(version.acceptJobs.toString()).append("</b>.<br>");
            } else {
                message.append("The CSI:FingerID webservice will no longer accept jobs from your current version")
                        .append("<br>");
            }
            message.append("Submitted jobs will be allowed to finish until <b>").append(version.finishJobs.toString()).append("</b>.");
        } else {
            message.append("Your Sirius version is not longer supported by the CSI:FingerID webservice.<br> Therefore the CSI:FingerID search is disabled in this version");
        }
        message.append("</html>");
        final JLabel label = new JLabel(message.toString());
        label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(label, BorderLayout.CENTER);
        final JPanel subpanel = new JPanel(new FlowLayout());
        ignore = new JButton("Ignore update");
        download = new JButton("Download SIRIUS " + version.siriusGuiVersion);
        subpanel.add(download);
        subpanel.add(ignore);
        subpanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(subpanel, BorderLayout.SOUTH);
        download.addActionListener(this);
        ignore.addActionListener(this);
        pack();
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == ignore) {
        } else if (e.getSource() == download) {
            try {
                Desktop.getDesktop().browse(new URI(PropertyManager.getProperty("de.unijena.bioinf.sirius.download")));
            } catch (IOException | URISyntaxException e1) {
                LoggerFactory.getLogger(this.getClass()).error(e1.getMessage(), e1);
            }
        }
        this.dispose();
    }
}
