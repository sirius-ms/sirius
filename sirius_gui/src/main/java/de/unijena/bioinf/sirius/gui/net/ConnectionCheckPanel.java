package de.unijena.bioinf.sirius.gui.net;

import de.unijena.bioinf.sirius.gui.fingerid.WebAPI;
import de.unijena.bioinf.sirius.gui.utils.BooleanJlabel;
import de.unijena.bioinf.sirius.gui.utils.TwoCloumnPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by fleisch on 06.06.17.
 */
public class ConnectionCheckPanel extends TwoCloumnPanel {
    final BooleanJlabel internet = new BooleanJlabel();
    final BooleanJlabel jena = new BooleanJlabel();
    final BooleanJlabel bioinf = new BooleanJlabel();
    final BooleanJlabel fingerID = new BooleanJlabel();
    final BooleanJlabel fingerID_WebAPI = new BooleanJlabel();

    JPanel resultPanel = null;

    public ConnectionCheckPanel(int state) {
        super(GridBagConstraints.WEST, GridBagConstraints.EAST);
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),"Internet connection test:"));

        add(new JLabel("Connection to the internet (google.com)"), internet, 15, false);
        add(new JLabel("Connection to uni-jena.de"), jena, 5, false);
        add(new JLabel("Connection to bio.informatics.uni-jena.de"), bioinf, 5, false);
        add(new JLabel("Connection to www.csi-fingerid.uni-jena.de"),fingerID, 5, false);
        add(new JLabel("Check CSI:FingerID REST API"),fingerID_WebAPI, 5, false);


        addVerticalGlue();


        refreshPanel(state);
    }

    public void refreshPanel(final int state) {
        internet.setState(state > 1 || state == 0);
        jena.setState(state > 2 || state == 0);
        bioinf.setState(state > 3 || state == 0);
        fingerID.setState(state > 4 || state == 0);
        fingerID_WebAPI.setState(state == 0);


        if (resultPanel != null)
            remove(resultPanel);
        resultPanel = createResultPanel(state);

        add(resultPanel,15,true);

        revalidate();
        repaint();
    }

    private JPanel createResultPanel(int state) {
        JPanel resultPanel = new JPanel();
        resultPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),"Description:"));

        final JLabel label;
        switch (state) {
            case 0:
                label = new JLabel("<html>Connection to CSI:FingerID Server successfully established <br>" +
                        "</html>");
                break;
            case 6:
                label = new JLabel("<html>" + " ErrorCode " + state +
                        " Could not reach the CSI:FingerID WebAPI <br>" +
                        "Out Service is no longer available for your current Sirius version <br>" +
                        "Please <a href=download>https://bio.informatik.uni-jena.de/software/sirius/</a> the current version of Sirius<br>" +
                        "</html>");
                break;
            case 5:
                label = new JLabel("<html>" + " ErrorCode " + state +
                        " Could not reach the CSI:FingerID WebAPI <br>" +
                        "Your Sirius version is still supported but the Service <br>" +
                        "is unfortunately not available.<br>" +
                        "Please <a href=mailto:sirius@uni-jena.de>contact</a> the developer for help.<br>" +
                        "</html>");
                break;
            case 4:
                label = new JLabel("<html>" + " ErrorCode " + state +
                        " Could not connect to the CSI:FingerID Server <br>" +
                        " Either our the CSI:FingerID server is temporary not available<br>" +
                        " or its URL cannot be reached because of your network configuration <br>" +
                        "</html>");
                break;
            case 3:
                label = new JLabel("<html>" + " ErrorCode " + state +
                        " Could not reach https://bio.informatik.uni-jena.de <br>" +
                        "Either our webserver is temporary not available<br>" +
                        " or it cannot be reached because of your network configuration <br>" +
                        "</html>");
                break;
            case 2:
                label = new JLabel("<html>" + " ErrorCode " + state +
                        " Could not reach uni-jena.de. <br>" +
                        "Either the whole uni-jena.de domain is temporary not available<br>" +
                        " or it cannot be reached because of your network configuration <br>" +
                        "</html>");
                break;
            case 1:
                label = new JLabel("<html>" + " ErrorCode " + state +
                        " Could not establish an internet connection.<br>" +
                        "Please check if your computer is connected to the internet.<br>" +
                        "All features depending on the database won't work without internet connection.<br>" +
                        "If you use a proxy, please check the proxy settings.<br>" +
                        "Note: You have to restart Sirius if you change system wide proxy settings.<br></html>");

                break;
            default:
                label = new JLabel("<html> An unknown Network Error occurred!." +
                        "</html>");
        }
        resultPanel.add(label);
        return resultPanel;
    }


    private void decorateWithLink(final JLabel website, final String URL) {
        website.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(URL));
                } catch (URISyntaxException | IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

}
