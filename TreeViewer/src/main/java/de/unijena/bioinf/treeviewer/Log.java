package de.unijena.bioinf.treeviewer;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Log extends JPanel{

    private final ArrayList<LogRecord> logs;
    private final JLabel lastMessage;
    private boolean expanded = false;
    private JButton expandButton;
    private AllLogs alllogs;
    private HashMap<Level, Icon> icons;
    private JPanel infoPanel;

    public Log() {
        this.icons = new HashMap<Level, Icon>();
        icons.put(Level.SEVERE, minimizeIcons(UIManager.getIcon("OptionPane.errorIcon")));
        icons.put(Level.WARNING, minimizeIcons(UIManager.getIcon("OptionPane.warningIcon")));
        icons.put(Level.INFO, minimizeIcons(UIManager.getIcon("OptionPane.informationIcon")));
        this.logs = new ArrayList<LogRecord>();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(new JSeparator());
        final JPanel labelPan = new JPanel();
        labelPan.setLayout(new BorderLayout(10, 0));
        expandButton = new JButton(UIManager.getIcon("Tree.collapsedIcon"));
        labelPan.add(expandButton, BorderLayout.WEST);
        expandButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (expanded) unexpand();
                else expand();
            }
        });
        lastMessage = new JLabel();
        labelPan.add(lastMessage, BorderLayout.CENTER);
        alllogs = new AllLogs();
        infoPanel = labelPan;
        //labelPan.add(alllogs, BorderLayout.CENTER);
        add(labelPan);
        expandButton.setVisible(true);
        setVisible(true);
        lastMessage.setPreferredSize(new Dimension(Integer.MAX_VALUE, 20));
        lastMessage.setVisible(true);
    }

    private Icon minimizeIcons(Icon icon) {
        final BufferedImage img = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        final Graphics gfx = img.getGraphics();
        icon.paintIcon(this, gfx, 0, 0);
        final BufferedImage newImg = new BufferedImage(icon.getIconWidth()/2, icon.getIconHeight()/2, BufferedImage.TYPE_4BYTE_ABGR);
        final Graphics2D gfx2 = (Graphics2D)newImg.getGraphics();
        gfx2.drawImage(img, new AffineTransformOp(AffineTransform.getScaleInstance(0.5, 0.5), AffineTransformOp.TYPE_BICUBIC), 0, 0);
        gfx2.dispose();
        return new ImageIcon(newImg);
    }

    public synchronized void add(LogRecord log) {
        this.logs.add(log);
        if (expanded) {
            alllogs.printLine(log);
        } else {
            lastMessage.setIcon(icons.get(log.getLevel()));
            lastMessage.setText(log.getMessage());
        }
    }

    public synchronized  void expand() {
        if (expanded) return;
        expanded = true;
        expandButton.setIcon(UIManager.getIcon("Tree.expandedIcon"));
        lastMessage.setVisible(false);
        alllogs.clear();
        for (LogRecord l : logs) {
            alllogs.printLine(l);
        }
        infoPanel.remove(lastMessage);
        infoPanel.add(alllogs, BorderLayout.CENTER);
        alllogs.setVisible(true);
    }
    public synchronized  void unexpand() {
        if (!expanded) return;
        expanded = false;
        expandButton.setIcon(UIManager.getIcon("Tree.collapsedIcon"));
        alllogs.setVisible(false);
        final LogRecord log = logs.get(logs.size()-1);
        lastMessage.setIcon(icons.get(log.getLevel()));
        lastMessage.setText(log.getMessage());
        infoPanel.remove(alllogs);
        infoPanel.add(lastMessage, BorderLayout.CENTER);
        lastMessage.setVisible(true);
    }

    class AllLogs extends JScrollPane {
        JTextArea textpane;
        AllLogs() {
            super(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            setPreferredSize(new Dimension(Integer.MAX_VALUE, 80));
            textpane = new JTextArea();
            add(textpane);
            setViewportView(textpane);
            this.setVisible(false);
        }

        public void clear() {
            try {
                textpane.getDocument().remove(0, textpane.getDocument().getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }


        public void printLine(LogRecord log) {
            final Document doc = textpane.getDocument();
            try {
                doc.insertString(0, log.getMessage() + "\n", null);
            } catch (BadLocationException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

}
