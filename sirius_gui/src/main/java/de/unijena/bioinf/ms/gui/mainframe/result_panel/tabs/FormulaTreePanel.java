package de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs;

import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.ms_viewer.SpectraViewContainer;
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
        if (elementsParent != null) {
            String alignedFeatureId = elementsParent.getFeatureId();
            System.out.println("featureID: " + alignedFeatureId);
            if (selectedElement != null){
                String formulaId = selectedElement.getFormulaId();
                System.out.println("formulaID: " + formulaId);
                updateSelectedFormulaCandidate(alignedFeatureId, formulaId);
            } else {
                updateSelectedFeature(alignedFeatureId);
            }
        } else {
            //todo clear view
        }
    }

    public void updateSpectrumSettings(int spectrumIndex, String spectrumType, Normalization normalizationMode, SpectraViewContainer.IntensityTransform intensityMode) {
        // todo add norm, int box handling
        executeJavaScript(String.format("window.urlUtils.updateSelectedSpectrum(spectrumIndex='%d', spectrumType=%s)", spectrumIndex, spectrumType));
    }
}
