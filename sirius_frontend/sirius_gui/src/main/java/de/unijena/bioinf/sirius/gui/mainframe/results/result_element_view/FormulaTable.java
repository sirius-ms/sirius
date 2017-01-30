package de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 30.01.17.
 */

import de.unijena.bioinf.sirius.gui.mainframe.results.*;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class FormulaTable extends JScrollPane implements ActiveResults {
    protected java.util.List<ActiveResultChangedListener> listeners = new ArrayList<>();
    private ResultTreeListModel listModel;
    private JList<SiriusResultElement> resultsJList;

    public FormulaTable(JList<ExperimentContainer> compundList) {
        super(VERTICAL_SCROLLBAR_NEVER, HORIZONTAL_SCROLLBAR_ALWAYS);
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(1, 5, 0, 0), "Molecular formulas"));
        ExperimentContainer ec = compundList.getSelectedValue();
        if (ec != null) this.listModel = new ResultTreeListModel(ec.getResults());
        else this.listModel = new ResultTreeListModel();
        this.resultsJList = new ResultsTreeList(this.listModel);
        this.listModel.setJList(this.resultsJList);

        ResultTreeListTextCellRenderer cellRenderer = new ResultTreeListTextCellRenderer();
        resultsJList.setCellRenderer(cellRenderer);
        resultsJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        resultsJList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        resultsJList.setVisibleRowCount(1);
        resultsJList.setMinimumSize(new Dimension(0, 45));
        resultsJList.setPreferredSize(new Dimension(0, 45));

        //todo merge that with the nice nu model
        //listen to them


    }

    private void listen(){
        int i = Math.max(resultsJList.getSelectedIndex(), 0);
        //todo marry with nu model
        /*if (ec != null && ec.getResults() != null && !ec.getResults().isEmpty()) {
            this.listModel.setData(ec.getResults());
            if (this.listModel.getSize() > 0) {
                if (this.ec != ec) {
                    this.ec = ec;
                    this.resultsJList.setSelectedIndex(0);
                } else {
                    resultsJList.setSelectedIndex(i);
                }
            }
        } else {
            this.listModel.setData(new ArrayList<SiriusResultElement>());
        }*/
    }

    public ResultTreeListModel getListModel() {
        return listModel;
    }

    public JList<SiriusResultElement> getResultsJList() {
        return resultsJList;
    }

    public void addActiveResultChangedListener(ActiveResultChangedListener listener) {
        listeners.add(listener);
    }

    public void removeActiveResultChangedListener(ActiveResultChangedListener listener) {
        listeners.remove(listener);
    }

    protected void notifyListeners(ExperimentContainer ec, SiriusResultElement sre) {
        for (ActiveResultChangedListener listener : listeners) {
            listener.resultsChanged(ec, sre);
        }
    }
}
