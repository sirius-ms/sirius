package de.unijena.bioinf.ms.gui.canopus.compound_classes;

import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.table.ActionList;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;

import javax.swing.*;
import java.util.List;

public class CompoundClassList extends ActionList<ClassyfirePropertyBean, FormulaResultBean> implements ActiveElementChangedListener<FormulaResultBean, InstanceBean> {

    public CompoundClassList(final FormulaList source) {
        super(ClassyfirePropertyBean.class, DataSelectionStrategy.FIRST_SELECTED);
        source.addActiveResultChangedListener(this);
    }

    @Override
    public void resultsChanged(InstanceBean experiment, FormulaResultBean sre, List<FormulaResultBean> resultElements, ListSelectionModel selections) {
        elementList.clear();
        if (sre != null) {
            sre.getCanopusResult().ifPresent(cr -> elementList.addAll(ClassyfirePropertyBean.fromCanopusResult(cr)));
        }
    }
}
