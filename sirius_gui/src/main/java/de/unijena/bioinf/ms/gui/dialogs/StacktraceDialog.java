

/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.dialogs;

import de.unijena.bioinf.ms.gui.utils.GuiUtils;

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

    private JButton ok, copy, showStacktrace;
    private JScrollPane sc;
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
        northPanel.add(new JLabel(icon));
        northPanel.add(new JLabel(GuiUtils.formatToolTip(message)));
        this.add(northPanel,BorderLayout.NORTH);

        final JPanel stackedPanel = new JPanel();
        stackedPanel.setLayout(new BoxLayout(stackedPanel, BoxLayout.Y_AXIS));
        final JTextArea textarea = new JTextArea(stacktrace);
        textarea.setEditable(false);
        textarea.addKeyListener(this);
        sc = new JScrollPane(textarea, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sc.setPreferredSize(new Dimension(400, 400));
        stackedPanel.add(sc);
        this.add(stackedPanel, BorderLayout.CENTER);
        sc.setVisible(false);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER,5,5));
        ok = new JButton("Ok");
        ok.addActionListener(this);
        south.add(ok);
        copy = new JButton("Copy to clipboard");
        copy.addActionListener(this);
        copy.setVisible(false);
        south.add(copy);
        showStacktrace = new JButton("Show stack trace");
        showStacktrace.addActionListener(this);
        south.add(showStacktrace);

        this.add(south,BorderLayout.SOUTH);
        this.pack();
        setLocationRelativeTo(getParent());
        this.setVisible(true);
        addKeyListener(this);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == showStacktrace) {
            showStacktrace.setVisible(false);
            sc.setVisible(true);
            copy.setVisible(true);
            this.pack();
        } else if (e.getSource() == copy) {
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
