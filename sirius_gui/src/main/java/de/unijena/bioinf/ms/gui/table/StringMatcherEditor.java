package de.unijena.bioinf.ms.gui.table;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 25.01.17.
 */

import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import javax.swing.text.JTextComponent;
import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class StringMatcherEditor<E,T extends SiriusTableFormat<E>> extends TextComponentMatcherEditor<E> {

    public StringMatcherEditor(final T format, final JTextComponent textComponent) {
        super(textComponent, new TextFilterator<E>() {
            @Override
            public void getFilterStrings(List<String> baseList, E element) {
                for (int i = 0; i < format.getColumnCount(); i++) {
                    baseList.add(format.getColumnValue(element, i).toString());
                }
            }
        });
    }

    // here we can now easily add new filtering options if needed


}
