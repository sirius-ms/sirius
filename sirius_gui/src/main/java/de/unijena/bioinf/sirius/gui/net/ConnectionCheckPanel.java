package de.unijena.bioinf.sirius.gui.net;

import de.unijena.bioinf.ChemistryBase.properties.PropertyManager;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.fingerworker.WorkerList;
import de.unijena.bioinf.sirius.gui.utils.BooleanJlabel;
import de.unijena.bioinf.sirius.gui.utils.TwoCloumnPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.EnumSet;

/**
 * Created by fleisch on 06.06.17.
 */
public class ConnectionCheckPanel extends TwoCloumnPanel {
    final BooleanJlabel internet = new BooleanJlabel();
    final BooleanJlabel jena = new BooleanJlabel();
    final BooleanJlabel bioinf = new BooleanJlabel();
    final BooleanJlabel fingerID = new BooleanJlabel();
    final BooleanJlabel fingerID_WebAPI = new BooleanJlabel();
    final BooleanJlabel fingerID_Worker = new BooleanJlabel();

    JPanel resultPanel = null;

    public ConnectionCheckPanel(int state, @Nullable WorkerList workerInfoList) {
        super(GridBagConstraints.WEST, GridBagConstraints.EAST);
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "Connection check:"));

        add(new JLabel("Connection to the internet (google.com)"), internet, 15, false);
        add(new JLabel("Connection to uni-jena.de"), jena, 5, false);
        add(new JLabel("Connection to bio.informatics.uni-jena.de"), bioinf, 5, false);
        add(new JLabel("Connection to www.csi-fingerid.uni-jena.de"), fingerID, 5, false);
        add(new JLabel("Check CSI:FingerID REST API"), fingerID_WebAPI, 5, false);
        add(new JLabel("All necessary workers available?"), fingerID_Worker, 5, false);

        addVerticalGlue();

        if (workerInfoList != null) {
            refreshPanel(
                    state,
                    workerInfoList.getActiveSupportedTypes(Instant.ofEpochSecond(600)),
                    workerInfoList.getPendingJobs()
            );
        } else {
            refreshPanel(state, EnumSet.noneOf(PredictorType.class), Integer.MIN_VALUE);
        }
    }

    public void refreshPanel(final int state, final EnumSet<PredictorType> availableTypes, final int pendingJobs) {
        internet.setState(state > 1 || state == 0);
        jena.setState(state > 2 || state == 0);
        bioinf.setState(state > 3 || state == 0);
        fingerID.setState(state > 4 || state == 0);
        fingerID_WebAPI.setState(state == 0);

        final EnumSet<PredictorType> neededTypes = PredictorType.parse(PropertyManager.getProperty("de.unijena.bioinf.fingerid.usedPredictors"));
        fingerID_Worker.setState(availableTypes.containsAll(neededTypes));

        if (resultPanel != null)
            remove(resultPanel);

        resultPanel = createResultPanel(state, neededTypes, availableTypes, pendingJobs);

        add(resultPanel, 15, true);

        revalidate();
        repaint();
    }

    private JPanel createResultPanel(final int state, final EnumSet<PredictorType> neededTypes, final EnumSet<PredictorType> availableTypes, final int pendingJobs) {
        JPanel resultPanel = new JPanel();
        resultPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "Description:"));

        final JLabel label;
        switch (state) {
            case 0:
                StringBuilder text = new StringBuilder();
                text.append("<html>Connection to CSI:FingerID Server successfully established!<br><br>");


                neededTypes.removeAll(availableTypes);

                String on = availableTypes.toString();
                on = on.substring(1, on.length() - 1);

                String off;
                if (neededTypes.isEmpty()) {
                    off = "<font color='green'>none</font>";
                } else {
                    off = neededTypes.toString();
                    off = off.substring(1, off.length() - 1);
                }

                text.append("<font color='green'>Worker instances available for:<br>")
                        .append("<b>").append(on).append("</font></b><br><br>");
                text.append("<font color='red'>Worker instances unavailable for:<br>")
                        .append("<b>").append(off).append("</font></b><br><br>");

                text.append("<font color='black'>Pending jobs on Server: <b>").append(pendingJobs < 0 ? "Unknown" : pendingJobs).append("</font></b>");
//                text.append("<br><br>");

                text.append("</html>");
                label = new JLabel(text.toString());
                break;
            case 6:
                label = new JLabel("<html>" + " ErrorCode " + state + ": " +
                        " Could not reach the CSI:FingerID WebAPI. <br>" +
                        "Our Service is no longer available for your current Sirius version. <br>" +
                        "Please <a href=https://bio.informatik.uni-jena.de/software/sirius/>download</a> the current version of Sirius<br>" +
                        "</html>");
                break;
            case 5:
                label = new JLabel("<html>" + " ErrorCode " + state + ": " +
                        " Could not reach the CSI:FingerID WebAPI. <br>" +
                        "Your Sirius version is still supported but the Service <br>" +
                        "is unfortunately not available.<br>" +
                        "Please <a href=mailto:sirius@uni-jena.de>contact</a> the developer for help.<br>" +
                        "</html>");
                break;
            case 4:
                label = new JLabel("<html>" + " ErrorCode " + state + ": " +
                        " Could not connect to the CSI:FingerID Server. <br>" +
                        " Either the CSI:FingerID server is temporary not available<br>" +
                        " or its URL cannot be reached because of your network configuration.<br>" +
                        "</html>");
                break;
            case 3:
                label = new JLabel("<html>" + " ErrorCode " + state + ": " +
                        " Could not reach https://bio.informatik.uni-jena.de. <br>" +
                        "Either our web server is temporary not available<br>" +
                        " or it cannot be reached because of your network configuration.<br>" +
                        "</html>");
                break;
            case 2:
                label = new JLabel("<html>" + " ErrorCode " + state + ": " +
                        " Could not reach uni-jena.de. <br>" +
                        "Either the whole uni-jena.de domain is temporary not available<br>" +
                        " or it cannot be reached because of your network configuration. <br>" +
                        "</html>");
                break;
            case 1:
                label = new JLabel("<html>" + " ErrorCode " + state + ": " +
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
