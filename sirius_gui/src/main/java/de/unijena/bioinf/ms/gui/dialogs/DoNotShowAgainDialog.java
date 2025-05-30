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

import de.unijena.bioinf.ms.frontend.core.SiriusProperties;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.util.function.Supplier;

public abstract class DoNotShowAgainDialog extends JDialog {

    protected JTextPane textPane;
    protected TwoColumnPanel textContainer;
    protected JCheckBox dontAsk;
    protected String property;


    public DoNotShowAgainDialog(Dialog owner, String title, String text, String propertyKey) {
        this(owner, title, () -> text, propertyKey);
    }

    public DoNotShowAgainDialog(Dialog owner, String title, Supplier<String> messageSupplier, String propertyKey) {
        super(owner, title, JDialog.DEFAULT_MODALITY_TYPE);
        decorate(messageSupplier, propertyKey);
    }

    public DoNotShowAgainDialog(Window owner, String title, String text, String propertyKey) {
        this(owner, title, () -> text, propertyKey);
    }

    public DoNotShowAgainDialog(Window owner, String title, Supplier<String> messageSupplier, String propertyKey) {
        super(owner, title, JDialog.DEFAULT_MODALITY_TYPE);
        decorate(messageSupplier, propertyKey);
    }

    private void decorate(Supplier<String> messageSupplier, String propertyKey) {
        this.property = propertyKey;
        if (propertyKey != null) {
            dontAsk = new JCheckBox();
            dontAsk.setText("Do not show dialog again.");
        }

        this.setLayout(new BorderLayout());
        JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        Icon dialogIcon = makeDialogIcon();
        if (dialogIcon != null) northPanel.add(new JLabel(dialogIcon));
        {
            textPane = new JTextPane();
            textPane.setCursor(null);
            textPane.setEditable(false); // as before
            textPane.setContentType("text/html"); // let the text pane know this is what you want
            textPane.setFocusable(false);
            textPane.setText(messageSupplier.get());
            textPane.setBorder(null);
            textPane.setOpaque(false);
            textPane.setBackground(new Color(0, 0, 0, 0));

            textPane.addHyperlinkListener(new HyperlinkListener() {
                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        try {
                            GuiUtils.openURLInSystemBrowserOrError(e.getURL().toURI());
                        } catch (Exception error) {
                            LoggerFactory.getLogger(this.getClass()).error(error.getMessage(), error);
                        }
                    }
                }
            });
        }

        textContainer = new TwoColumnPanel();
        textContainer.add(textPane);
        northPanel.add(textContainer);
        TwoColumnPanel bodyPanel = new TwoColumnPanel();
        bodyPanel.add(northPanel);
        decorateBodyPanel(bodyPanel);
        add(bodyPanel, BorderLayout.CENTER);
        JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.X_AXIS));
        south.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        if (dontAsk != null && dontAsk.getParent() == null) { //if not added elsewhere, add to south panel
            south.add(dontAsk);
        }

        south.add(Box.createHorizontalGlue());
        decorateButtonPanel(south);

        this.add(south, BorderLayout.SOUTH);
        this.pack();
        this.setResizable(false);
        setLocationRelativeTo(getParent());
    }

    protected void saveDoNotAskMeAgain() {
        if (dontAsk != null && property != null && !property.isBlank() && dontAsk.isSelected())
            SiriusProperties.setAndStoreInBackground(property, getResult());
    }

    protected abstract String getResult();

    protected void decorateBodyPanel(TwoColumnPanel boxedButtonPanel){}

    protected abstract void decorateButtonPanel(JPanel boxedButtonPanel);

    protected abstract Icon makeDialogIcon();
}
