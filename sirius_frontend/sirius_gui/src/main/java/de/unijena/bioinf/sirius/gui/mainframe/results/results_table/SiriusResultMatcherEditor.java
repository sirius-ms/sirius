package de.unijena.bioinf.sirius.gui.mainframe.results.results_table;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 25.01.17.
 */

import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;

import javax.swing.text.JTextComponent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class SiriusResultMatcherEditor extends TextComponentMatcherEditor<SiriusResultElement> {

    public SiriusResultMatcherEditor(JTextComponent textComponent) {
        super(textComponent, new TextFilterator<SiriusResultElement>() {
            @Override
            public void getFilterStrings(List<String> baseList, SiriusResultElement element) {
                for (int i = 0; i < SiriusResultTableFormat.COL_COUNT; i++) {
                    baseList.add(SiriusResultTableFormat.getValue(element, i).toString());
                }
            }
        });
    }

    // here we can now easily add new filtering options if needed


}
