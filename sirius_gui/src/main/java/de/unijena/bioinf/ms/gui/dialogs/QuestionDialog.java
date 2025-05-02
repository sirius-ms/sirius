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

import de.unijena.bioinf.ms.gui.utils.ReturnValue;
import de.unijena.bioinf.ms.properties.PropertyManager;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

public class QuestionDialog extends DoNotShowAgainDialog {

    protected ReturnValue rv;
    protected JButton ok;
    protected JButton cancel;

    public QuestionDialog(Window owner, String question) {
        this(owner, question, null);
    }

    /**
     * @param owner       see JDialog
     * @param question    Question that is asked with this dialog
     * @param propertyKey name of the property with which the 'don't ask' flag is saved persistently
     */
    public QuestionDialog(Window owner, String question, String propertyKey) {
        this(owner, question, propertyKey, (ReturnValue) null);
    }

    public QuestionDialog(Window owner, String question, String propertyKey, @Nullable ReturnValue dontShowAgainReturn) {
        this(owner, "", () -> question, propertyKey, dontShowAgainReturn);
    }

    /**
     * @param owner       see JDialog
     * @param question    Question that is asked with this dialog
     * @param propertyKey name of the property with which the 'don't ask' flag is saved persistently
     * @param title       Title of the dialog
     */
    public QuestionDialog(Window owner, String title, String question, String propertyKey) {
        this(owner, title, () -> question, propertyKey, null);
    }

    public QuestionDialog(Window owner, String title, String question, String propertyKey, @Nullable ReturnValue dontShowAgainReturn) {
        this(owner, title, () -> question, propertyKey, dontShowAgainReturn);
    }


    /**
     * @param owner            see JDialog
     * @param questionSupplier Supplier for Question that is asked with this dialog
     * @param propertyKey      name of the property with which the 'don't ask' flag is saved persistently
     * @param title            Title of the dialog
     */
    public QuestionDialog(Window owner, String title, Supplier<String> questionSupplier, String propertyKey) {
        this(owner, title, () -> questionSupplier.get(), propertyKey, null);
    }

    public QuestionDialog(Window owner, String title, Supplier<String> questionSupplier, String propertyKey, @Nullable ReturnValue dontShowAgainReturn) {
        this(owner, title, questionSupplier, propertyKey, dontShowAgainReturn, true);
    }
    public QuestionDialog(Window owner, String title, Supplier<String> questionSupplier, String propertyKey, @Nullable ReturnValue dontShowAgainReturn, boolean visible) {
        super(owner, title, questionSupplier, propertyKey);
        if (propertyKey != null) {
            rv = PropertyManager.getEnum(propertyKey, ReturnValue.class);
            if (rv != null) {
                if (dontShowAgainReturn != null)
                    rv = dontShowAgainReturn;
                return;
            }
        }
        rv = ReturnValue.Cancel;
        this.setVisible(visible);
    }

    @Override
    protected void decorateButtonPanel(JPanel boxedButtonPanel) {
       ok = new JButton("Yes");
        ok.addActionListener(e -> {
            rv = ReturnValue.Success;
            saveDoNotAskMeAgain();
            dispose();
        });


        cancel = new JButton("No");
        cancel.addActionListener(e -> {
            rv = ReturnValue.Cancel;
            saveDoNotAskMeAgain();
            dispose();
        });

        boxedButtonPanel.add(Box.createHorizontalGlue());
        boxedButtonPanel.add(ok);
        boxedButtonPanel.add(cancel);
    }

    @Override
    protected Icon makeDialogIcon() {
        return UIManager.getIcon("OptionPane.questionIcon");
    }

    @Override
    protected String getResult() {
        return getReturnValue().name();
    }

    public ReturnValue getReturnValue() {
        return rv;
    }

    public boolean isSuccess() {
        return rv.equals(ReturnValue.Success);
    }

    public boolean isCancel() {
        return rv.equals(ReturnValue.Cancel);
    }
}
