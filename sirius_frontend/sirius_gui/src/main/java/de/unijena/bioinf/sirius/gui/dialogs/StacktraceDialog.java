/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.bioinf.sirius.gui.dialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.PrintWriter;
import java.io.StringWriter;

public class StacktraceDialog extends JDialog implements ActionListener, KeyListener {

    private JButton ok, copy;
    private String fullmsg;

    public StacktraceDialog(Frame owner, String message, String stackTrace ) {
        super(owner,true);
        initDialog(message, stackTrace);
    }

    public StacktraceDialog(Frame owner, String message, Throwable exception) {
        super(owner,true);
        initDialog(message, getStacktrace(exception));
    }
    public StacktraceDialog(Dialog owner, String message, String stackTrace ) {
        super(owner,true);
        initDialog(message, stackTrace);
    }

    public StacktraceDialog(Dialog owner, String message, Throwable exception) {
        super(owner,true);
        initDialog(message, getStacktrace(exception));
    }


    private String getStacktrace(Throwable exception) {
        final StringWriter sw = new StringWriter();
        final PrintWriter w = new PrintWriter(sw);
        exception.printStackTrace(w);
        w.close();
        return sw.toString();
    }

    public void initDialog(String message, String stacktrace) {
        this.fullmsg = message+"\n"+stacktrace;
        this.setLayout(new BorderLayout());
        JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.CENTER,20,10));
        Icon icon = UIManager.getIcon("OptionPane.errorIcon");
        final JPanel stackedPanel = new JPanel();
        stackedPanel.setLayout(new BoxLayout(stackedPanel, BoxLayout.Y_AXIS));
        northPanel.add(new JLabel(icon));
        northPanel.add(new JLabel(message));
        final JTextArea textarea = new JTextArea(stacktrace);
        textarea.setEditable(false);
        textarea.addKeyListener(this);
        final JScrollPane sc = new JScrollPane(textarea, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        stackedPanel.add(sc);
        stackedPanel.setPreferredSize(new Dimension(400, 600));
        this.add(northPanel,BorderLayout.NORTH);
        this.add(stackedPanel, BorderLayout.CENTER);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER,5,5));
        ok = new JButton("Ok");
        ok.addActionListener(this);
        copy = new JButton("Copy to clipboard");
        south.add(ok);
        copy.addActionListener(this);
        south.add(copy);
        this.add(south,BorderLayout.SOUTH);
        this.pack();
        setLocationRelativeTo(getParent());
        this.setVisible(true);
        addKeyListener(this);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource()==copy) {
            copyPlainText(fullmsg);
        } else {
            this.dispose();
        }
    }

    private void copyPlainText(String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        final StringSelection sel = new StringSelection(text);
        clipboard.setContents(sel, null);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode()==KeyEvent.VK_ESCAPE) dispose();
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}
