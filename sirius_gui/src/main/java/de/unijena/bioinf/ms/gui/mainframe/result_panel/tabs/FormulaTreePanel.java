package de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs;

import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.ms.gui.webView.JCefBrowserPanel;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.net.URI;
import java.util.List;

public class FormulaTreePanel extends JCefBrowserPanel implements ActiveElementChangedListener<FormulaResultBean, InstanceBean> {

    //todo make loadable by using swing based spinner
    public FormulaTreePanel(@NotNull FormulaList formulaList, SiriusGui siriusGui) {
        super(URI.create(siriusGui.getSiriusClient().getApiClient().getBasePath()).resolve("/formulaTreeView")
                + "?pid=" + siriusGui.getProjectManager().getProjectId(), siriusGui);
        formulaList.addActiveResultChangedListener(this);
    }


    @Override
    public void resultsChanged(InstanceBean elementsParent, FormulaResultBean selectedElement, List<FormulaResultBean> resultElements, ListSelectionModel selections) {
        String alignedFeatureId = elementsParent != null ? elementsParent.getFeatureId() : null;
        String formulaId = selectedElement != null ? selectedElement.getFormulaId() : null;
        updateSelectedFormulaCandidate(alignedFeatureId, formulaId);
    }
}
