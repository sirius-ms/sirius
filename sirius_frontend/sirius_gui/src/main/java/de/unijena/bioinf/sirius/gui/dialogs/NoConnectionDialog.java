package de.unijena.bioinf.sirius.gui.dialogs;

import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.gui.fingerid.WebAPI;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import org.jdesktop.swingx.VerticalLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by Marcus Ludwig on 17.11.16.
 */
public class NoConnectionDialog extends JDialog implements ActionListener {

    private JButton proxy;

    public NoConnectionDialog(Dialog owner) {
        super(owner, "No connection to server.", ModalityType.APPLICATION_MODAL);
        initDialog();
    }

    public NoConnectionDialog(Frame owner) {
        super(owner, "No connection to server.", ModalityType.APPLICATION_MODAL);
        initDialog();
    }

    private void initDialog(){
        setLayout(new BorderLayout());

        final JPanel subpanel = new JPanel(new VerticalLayout(5));
        final JLabel label = new JLabel("<html>Either you have no internet connection or our server is not responding.<br>" +
                "All features depending on the database won't work.<br><br>" +
                "If you use a proxy, please specify it in the proxy setting.<br><br>" +
                "You have to restart Sirius after changing proxy settings.<br><br></html>");
        label.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
        subpanel.add(label);
        final JLabel testWebsite = new JLabel("<html><a href=\"\">Test website</a></html>");
        testWebsite.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
        subpanel.add(testWebsite);
        testWebsite.setCursor(new Cursor(Cursor.HAND_CURSOR));
        goWebsite(testWebsite);

        subpanel.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
        add(subpanel, BorderLayout.CENTER);

        final JPanel okPanel = new JPanel(new FlowLayout());
        proxy = new JButton("Open proxy settings");
        proxy.addActionListener(this);
        okPanel.add(proxy);

        JButton ok = new JButton("Ok");
        ok.addActionListener(this);
        okPanel.add(ok);



        okPanel.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
        add(okPanel, BorderLayout.SOUTH);
        pack();
        setVisible(true);
    }

    private void goWebsite(final JLabel website) {
        website.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(WebAPI.FINGERID_WEBSITE));
                } catch (URISyntaxException | IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        this.dispose();
        if (e.getSource().equals(proxy)){
            new SettingsDialog(MainFrame.MF,1);
        }
    }
}