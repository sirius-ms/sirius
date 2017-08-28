package de.unijena.bioinf.sirius.gui.dialogs;

import de.unijena.bioinf.sirius.gui.configs.Icons;
import de.unijena.bioinf.sirius.gui.fingerid.WebAPI;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.net.ConnectionCheckPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by Marcus Ludwig on 17.11.16.
 */
public class ConnectionDialog extends JDialog implements ActionListener {
    private final static String name = "Internet Connection checkConnectionToUrl";
    private JButton proxy;
    private ConnectionCheckPanel connectionCheck;

    public ConnectionDialog(Dialog owner) {
        super(owner, name, ModalityType.APPLICATION_MODAL);
        initDialog(WebAPI.checkFingerIDConnectionStatic());
    }

    public ConnectionDialog(Frame owner) {
        super(owner, name, ModalityType.APPLICATION_MODAL);
        initDialog(WebAPI.checkFingerIDConnectionStatic());
    }

    public ConnectionDialog(Frame owner, int state) {
        super(owner, name, ModalityType.APPLICATION_MODAL);
        initDialog(state);
    }

    private void initDialog(int state){
        setLayout(new BorderLayout());

        //header
        JPanel header = new DialogHaeder(Icons.NET_64);
        add(header, BorderLayout.NORTH);

        connectionCheck = new ConnectionCheckPanel(state);
        add(connectionCheck,BorderLayout.CENTER);


        //south
        proxy = new JButton("Open proxy settings");
        proxy.addActionListener(this);

        JButton ok = new JButton("Ok");
        ok.addActionListener(this);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(proxy);
        buttons.add(ok);

        add(buttons,BorderLayout.SOUTH);


        setMinimumSize(new Dimension(350, getPreferredSize().height));
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
        if (state > WebAPI.MAX_STATE)
            if (getParent() instanceof Dialog) {
                new ErrorReportDialog((Dialog) getParent(), "An unknown Network Error occurred!");
            }else{
                new ErrorReportDialog((Frame) getParent(), "An unknown Network Error occurred!");
            }
    }



    @Override
    public void actionPerformed(ActionEvent e) {
        this.dispose();
        if (e.getSource().equals(proxy)){
            new SettingsDialog(MainFrame.MF,1);
        }
    }
}
