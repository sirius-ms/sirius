package de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs;

import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.ms.gui.webView.BrowserPanel;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class SpectraTreePanel extends JPanel implements ActiveElementChangedListener<FormulaResultBean, InstanceBean> {

    private final BrowserPanel browserPanel;

    //todo make loadable by using swing based spinner
    public SpectraTreePanel(@NotNull FormulaList formulaList, SiriusGui siriusGui) {
        super(new BorderLayout());
        browserPanel = siriusGui.getBrowserPanelProvider().makeReactPanel("/formulaTreeView", siriusGui.getProjectManager().getProjectId());
        formulaList.addActiveResultChangedListener(this);
        add(browserPanel, BorderLayout.CENTER);
    }


    @Override
    public void resultsChanged(InstanceBean elementsParent, FormulaResultBean selectedElement, List<FormulaResultBean> resultElements, ListSelectionModel selections) {
        String alignedFeatureId = elementsParent != null ? elementsParent.getFeatureId() : null;
        String formulaId = selectedElement != null ? selectedElement.getFormulaId() : null;
        browserPanel.updateSelectedFormulaCandidate(alignedFeatureId, formulaId);
    }
}
