package de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 30.01.17.
 */

import javax.swing.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class DefaultResultElementListenerPanel extends FormulaSelectionResultPanel {
    protected JComponent centerView;
    protected JComponent southView;
    protected JComponent eastView;
    protected JComponent westView;


    public DefaultResultElementListenerPanel(FormulaTable resultElementTable, JComponent centerView, JComponent southView, JComponent eastView, JComponent westView, boolean clone) {
        super(resultElementTable, clone);
        this.centerView = centerView;
        this.southView = southView;
        this.eastView = eastView;
        this.westView = westView;

        buildPanel();
    }

    public DefaultResultElementListenerPanel(FormulaTable resultElementTable, JComponent centerView, boolean clone) {
        this(resultElementTable, centerView, null, null, null, clone);
    }


    @Override
    protected JComponent createSouth() {
        return southView;
    }

    @Override
    protected JComponent createEast() {
        return eastView;
    }

    @Override
    protected JComponent createWest() {
        return westView;
    }

    @Override
    protected JComponent createCenter() {
        return centerView;
    }


}
