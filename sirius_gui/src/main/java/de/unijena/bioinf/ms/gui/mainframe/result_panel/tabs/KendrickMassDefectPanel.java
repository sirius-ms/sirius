package de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.CompoundList;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.ExperimentListChangeListener;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.PanelDescription;
import de.unijena.bioinf.ms.gui.webView.JCefBrowserPanel;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.List;

public class KendrickMassDefectPanel extends JCefBrowserPanel implements ExperimentListChangeListener, PanelDescription {


    public KendrickMassDefectPanel(@NotNull CompoundList compoundList, SiriusGui siriusGui) {
        super(URI.create(siriusGui.getSiriusClient().getApiClient().getBasePath()).resolve("/KMD")
                + THEME_REST_PARA + "&pid=" + siriusGui.getProjectManager().getProjectId(), siriusGui);
        compoundList.addChangeListener(this);
    }

    @Override
    public void listChanged(ListEvent<InstanceBean> event, DefaultEventSelectionModel<InstanceBean> selection, int fullSize) {
        //selection change will also happen if list change affects selection
    }

    @Override
    public void listSelectionChanged(DefaultEventSelectionModel<InstanceBean> selection, List<InstanceBean> selected, List<InstanceBean> deselected, int fullSize) {
        updateSelectedFeature(selected == null || selected.isEmpty() ? null
                : selected.getFirst().getFeatureId());
    }

    @Override
    public String getDescription() {
        return getDescriptionString();
    }

    public static String getDescriptionString() {
        //todo write useful description
        return "Kendrick mass defect plot for homologue series.";
    }
}
