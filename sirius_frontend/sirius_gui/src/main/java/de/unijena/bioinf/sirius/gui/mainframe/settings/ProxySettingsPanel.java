package de.unijena.bioinf.sirius.gui.mainframe.settings;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 06.10.16.
 */

import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.core.PasswordCrypter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ProxySettingsPanel extends JPanel implements ActionListener, SettingsPanel {
    private GridBagConstraints both, left, right;
    private Properties props;
    private JCheckBox useProxy, useCredentials;
    private JPanel cred;
    private JTextField proxyHost, proxyUser;
    private JSpinner proxyPort;
    private JComboBox<String> proxyScheme;
    private JPasswordField pw;

    public static void main(String[] args){
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                    ex.printStackTrace();
                }

                String s = ApplicationCore.VERSION_STRING;
                JFrame frame = new JFrame("Testing");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.add(new ProxySettingsPanel(new Properties(System.getProperties())));
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
    public ProxySettingsPanel(Properties properties) {
        super();
        this.props = properties;
        setLayout(new GridBagLayout());
        setBorder(new TitledBorder(new EmptyBorder(5, 20, 5, 5), "Proxy Settings"));


        left = new GridBagConstraints();
        left.gridx = 0;
        left.fill = GridBagConstraints.NONE;
        left.anchor = GridBagConstraints.EAST;
        left.weightx = 0;
        left.weighty = 0;
        left.insets = new Insets(0, 0, 0, 10);
//            left.ipady = 2;

        right = new GridBagConstraints();
        right.gridx = 1;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.anchor = GridBagConstraints.WEST;
        right.weightx = 1;
        right.weighty = 0;

        both = new GridBagConstraints();
        both.gridx = 0;
        both.gridwidth = 2;
        both.fill = GridBagConstraints.BOTH;
        both.insets = new Insets(0, 0, 5, 0);
        setRow(0);


        useProxy = new JCheckBox();
        useProxy.addActionListener(this);
        useProxy.setText("Use Proxy Server");
        useProxy.setSelected(Boolean.valueOf(props.getProperty("de.unijena.bioinf.sirius.proxy")));
        add(this, useProxy);

        proxyHost = new JTextField();
        proxyHost.setText(props.getProperty("de.unijena.bioinf.sirius.proxy.hostname"));
        add(this, new JLabel("Hostname:"), proxyHost);

        proxyPort = new JSpinner(new SpinnerNumberModel(8080, 1, 99999, 1));
        proxyPort.setEditor(new JSpinner.NumberEditor(proxyPort,"#"));
        proxyPort.setValue(Integer.valueOf(props.getProperty("de.unijena.bioinf.sirius.proxy.port")));
        add(this, new JLabel("Proxy Port:"), proxyPort);
        proxyScheme = new JComboBox<>(new String[]{"http","https"});
        proxyScheme.setSelectedItem(props.getProperty("de.unijena.bioinf.sirius.proxy.scheme"));
        add(this, new JLabel("Proxy Scheme:"), proxyScheme);

        //############# Credentials Stuff ########################


        cred = new JPanel();
//            cred.setLayout(new BoxLayout(cred, BoxLayout.PAGE_AXIS));
        cred.setLayout(new GridBagLayout());
        cred.setBorder(new TitledBorder(new EmptyBorder(5, 5, 5, 5), "Proxy Credentials"));
        both.insets = new Insets(15, 0, 0, 0);
        add(this, cred);
        both.insets = new Insets(0, 0, 5, 0);


        //reset for new Panel
        setRow(0);

        useCredentials = new JCheckBox();
        useCredentials.addActionListener(this);
        useCredentials.setText("Use Credentials:");
        useCredentials.setSelected(Boolean.valueOf(props.getProperty("de.unijena.bioinf.sirius.proxy.credentials")));
        add(cred, useCredentials);

        proxyUser = new JTextField();
        proxyUser.setText(props.getProperty("de.unijena.bioinf.sirius.proxy.user"));
        add(cred, new JLabel("Username:"), proxyUser);

        pw = new JPasswordField();
        String text = PasswordCrypter.decryptProp("de.unijena.bioinf.sirius.proxy.pw",props);
        pw.setText(text);
        add(cred, new JLabel("Password:"), pw);

        refreshValues();
    }

    private void setRow(int i) {
        left.gridy = 0;
        right.gridy = 0;
        both.gridy = 0;
    }

    void add(JPanel parent, JComponent leftComp, JComponent rightComp) {

        if (leftComp != null)
            parent.add(leftComp, left);

        if (rightComp != null)
            parent.add(rightComp, right);

        left.gridy++;
        right.gridy++;
        both.gridy++;
    }

    void add(JPanel parent, JComponent comp) {
        parent.add(comp, both);
        left.gridy++;
        right.gridy++;
        both.gridy++;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("Action");
        if (e.getSource() == useProxy) {
            System.out.println("useProxy");
            refreshValues();

        } else if (e.getSource() == useCredentials) {
            System.out.println("useCreds");
            refreshValues();
        }
    }

    @Override
    public Properties getProperties() {
        return null;
    }

    @Override
    public void refreshValues() {
        System.out.println("refresh");
        useCredentials.setEnabled(useProxy.isSelected());
        proxyHost.setEnabled(useProxy.isSelected());
        proxyPort.setEnabled(useProxy.isSelected());
        proxyScheme.setEnabled(useProxy.isSelected());
//        cred.setEnabled(useProxy.isSelected());
        proxyUser.setEnabled(useCredentials.isSelected() && useProxy.isSelected());
        pw.setEnabled(useCredentials.isSelected() && useProxy.isSelected());
    }

    @Override
    public void saveProperties() {
        System.out.println("SAVE");
        props.setProperty("de.unijena.bioinf.sirius.proxy",String.valueOf(useProxy.isSelected()));
        props.setProperty("de.unijena.bioinf.sirius.proxy.credentials",String.valueOf(useCredentials.isSelected()));
        props.setProperty("de.unijena.bioinf.sirius.proxy.hostname",proxyHost.getText());
        props.setProperty("de.unijena.bioinf.sirius.proxy.port",String.valueOf(proxyPort.getValue()));
        props.setProperty("de.unijena.bioinf.sirius.proxy.scheme",(String) proxyScheme.getSelectedItem());
        props.setProperty("de.unijena.bioinf.sirius.proxy.user",proxyUser.getText());

        PasswordCrypter.setEncryptetProp("de.unijena.bioinf.sirius.proxy.pw",String.valueOf(pw.getPassword()),props);
    }
}
