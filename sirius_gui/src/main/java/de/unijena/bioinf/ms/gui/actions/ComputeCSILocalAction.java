package de.unijena.bioinf.ms.gui.actions;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 30.01.17.
 */

import ca.odell.glazedlists.EventList;
import de.unijena.bioinf.ms.gui.fingerid.FingerIdDialog;
import de.unijena.bioinf.ms.gui.fingerid.FingerIdTask;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.sirius.ExperimentResultBean;
import de.unijena.bioinf.ms.gui.sirius.SiriusResultElement;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ComputeCSILocalAction extends ComputeCSIAction {

    public ComputeCSILocalAction() {
        super();
        putValue(LARGE_ICON_KEY, Icons.FINGER_64);
        putValue(SHORT_DESCRIPTION, "Search molecular formulas with CSI:FingerID");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (CheckConnectionAction.isNotConnectedAndLoad())
            return;
        
        //calculate csi
        final FingerIdDialog dialog = new FingerIdDialog(MF, MF.getCsiFingerId(), true, true);
        final int returnState = dialog.run();

        if (returnState != FingerIdDialog.CANCELED) {
            ExperimentResultBean ec = null;
            EventList<ExperimentResultBean> l = MF.getCompoundListSelectionModel().getSelected();
            if (l != null && !l.isEmpty()) {
                ec = l.get(0);
            }

            if (returnState == FingerIdDialog.COMPUTE_ALL) {
                MF.getCsiFingerId().compute(ec, dialog.getSearchDb());
            } else {
                java.util.List<SiriusResultElement> selected = MF.getFormulaList().getSelectedValues();
                java.util.List<FingerIdTask> tasks = new ArrayList<>(selected.size());
                for (SiriusResultElement element : selected) {
                    if (element.getCharge() > 0 || element.getResult().getResolvedTree().numberOfEdges() > 0)
                        tasks.add(new FingerIdTask(dialog.getSearchDb(), ec, element));
                }
                MF.getCsiFingerId().computeAll(tasks);
            }
        }
    }
}
