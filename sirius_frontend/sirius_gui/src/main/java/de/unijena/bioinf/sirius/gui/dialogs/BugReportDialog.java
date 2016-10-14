package de.unijena.bioinf.sirius.gui.dialogs;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 13.10.16.
 */

import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.core.errorReport.FinngerIDWebErrorReporter;
import de.unijena.bioinf.sirius.core.errorReport.SiriusDefaultErrorReport;
import de.unijena.bioinf.sirius.gui.settings.ErrorReportSettingsPanel;
import de.unijena.bioinf.sirius.gui.utils.Icons;
import de.unijena.bioinf.sirius.gui.utils.SwingUtils;
import de.unijena.bioinf.utils.errorReport.ErrorReport;
import de.unijena.bioinf.utils.errorReport.ErrorReporter;
import org.jdesktop.swingx.JXTitledSeparator;

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
public class BugReportDialog extends JDialog {
    private final JButton discard, send;
    private final ErrorReportSettingsPanel reportSettings;
    private final JTextField subjectField = new JTextField();
    private final JTextArea textarea;
    private final JComboBox<String> report;
    private static String[] reportTypes = {ErrorReport.TYPES[1], ErrorReport.TYPES[2]};

    private final Properties props = ApplicationCore.getUserCopyOfUserProperties();

    public BugReportDialog(Frame owner) {
        super(owner, true);
        setTitle("Bug Report");
        setLayout(new BorderLayout());

        //============= NORTH =================
        add(new DialogHaeder(Icons.BUG_64), BorderLayout.NORTH);

        //============= CENTER =================
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.PAGE_AXIS));
        center.setBorder(BorderFactory.createEmptyBorder(SwingUtils.LARGE_GAP, 0, 0, 0));

        report = new JComboBox<>(reportTypes);
        center.add(report);
        center.add(Box.createVerticalStrut(SwingUtils.MEDIUM_GAP));

        reportSettings = new ErrorReportSettingsPanel(props);
        reportSettings.add(new JXTitledSeparator("Report"), SwingUtils.MEDIUM_GAP, false);


        JPanel sub = new JPanel(new BorderLayout());
        sub.add(new JLabel("Subject:  "),BorderLayout.WEST);
        sub.add(subjectField,BorderLayout.CENTER);
        reportSettings.add(sub);

        textarea = new JTextArea();
        textarea.setEditable(true);
        final JScrollPane sc = new JScrollPane(textarea, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sc.setPreferredSize((new Dimension(sc.getPreferredSize().width, 200)));
        sc.setBorder(new TitledBorder(new EmptyBorder(SwingUtils.SMALL_GAP, SwingUtils.SMALL_GAP, SwingUtils.SMALL_GAP, SwingUtils.SMALL_GAP), "Message"));
        reportSettings.add(sc, SwingUtils.SMALL_GAP, true);

        center.add(reportSettings);

        add(center, BorderLayout.CENTER);


        //============= SOUTH =================
        discard = new JButton("Discard");
        discard.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        send = new JButton("Send");
        send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new SwingWorker<String,String>() {
                    @Override
                    protected String doInBackground() throws Exception {
                        reportSettings.saveProperties();
                        ApplicationCore.changeDefaultProptertiesPersistent(props);

                        boolean senMail = Boolean.valueOf(System.getProperty("de.unijena.bioinf.sirius.core.errorReporting.sendUsermail"));
                        String mail = System.getProperty("de.unijena.bioinf.sirius.core.mailService.usermail");
                        boolean systemInfo = Boolean.valueOf(System.getProperty("de.unijena.bioinf.sirius.core.errorReporting.systemInfo"));
                        ErrorReporter repoter = new FinngerIDWebErrorReporter(new SiriusDefaultErrorReport(subjectField.getText(), textarea.getText(), mail, systemInfo));
                        repoter.getReport().setSendReportToUser(senMail);
                        repoter.getReport().setType((String) report.getSelectedItem());
                        repoter.call();
                        return "SUCCESS";
                    }
                }.execute();

                dispose();
            }
        });


        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(discard);
        buttons.add(send);

        add(buttons, BorderLayout.SOUTH);

        setMinimumSize(new Dimension(350, getPreferredSize().height));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }


}
