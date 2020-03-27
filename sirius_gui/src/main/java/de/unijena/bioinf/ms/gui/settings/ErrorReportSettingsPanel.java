package de.unijena.bioinf.ms.gui.settings;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 07.10.16.
 */

import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import de.unijena.bioinf.utils.mailService.Mail;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.Properties;

import static de.unijena.bioinf.ms.gui.utils.GuiUtils.SMALL_GAP;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ErrorReportSettingsPanel extends TwoColumnPanel implements SettingsPanel {
    private Properties props;
    private JTextField emailField;
    private JCheckBox uesrCopy, hardwareInfo;

    public ErrorReportSettingsPanel(Properties properties) {
        super();
        this.props = properties;

        String email = PropertyManager.getProperty("de.unijena.bioinf.sirius.core.mailService.usermail");
        if (email != null && !email.isEmpty())
            emailField = new JTextField(email);
        else
            emailField = new JTextField();
        add(new JLabel("Contact email adress: "), emailField);

        hardwareInfo = new JCheckBox("Send hardware and OS information?", Boolean.valueOf(PropertyManager.getProperty("de.unijena.bioinf.sirius.core.errorReporting.systemInfo")));
        uesrCopy = new JCheckBox("Send a copy to my mail address?", Boolean.valueOf(PropertyManager.getProperty("de.unijena.bioinf.sirius.core.errorReporting.sendUsermail")));
        add(hardwareInfo, SMALL_GAP, false);
        add(uesrCopy);
    }


    @Override
    public void saveProperties() {
        String mail = emailField.getText();
        if (Mail.validateMailAdress(mail)) {
            props.setProperty("de.unijena.bioinf.sirius.core.mailService.usermail", emailField.getText());
        } else {
            LoggerFactory.getLogger(this.getClass()).warn("No Valid mail Address. Email Address not saved");
        }

        props.setProperty("de.unijena.bioinf.sirius.core.errorReporting.systemInfo", String.valueOf(hardwareInfo.isSelected()));
        props.setProperty("de.unijena.bioinf.sirius.core.errorReporting.sendUsermail", String.valueOf(uesrCopy.isSelected()));
    }

    @Override
    public String name() {
        return "Error Report";
    }


}
