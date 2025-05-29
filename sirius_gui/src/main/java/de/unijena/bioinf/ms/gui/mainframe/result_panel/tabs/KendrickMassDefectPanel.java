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
    @NotNull private final CompoundList compoundList;

    public KendrickMassDefectPanel(@NotNull CompoundList compoundList, SiriusGui siriusGui) {
        super(makeUrl(siriusGui, compoundList), siriusGui);
        this.compoundList = compoundList;
        compoundList.addChangeListener(this);
    }

    private static String makeUrl(SiriusGui siriusGui, @NotNull CompoundList compoundList){
        String fid = compoundList.getCompoundListSelectionModel().getSelected().stream().findFirst().map(InstanceBean::getFeatureId).orElse(null);
        return URI.create(siriusGui.getSiriusClient().getApiClient().getBasePath()).resolve("/KMD")
                + makeParameters(siriusGui.getProjectManager().getProjectId(), fid, null, null, null);
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
    public void removeNotify() {
        // Call the superclass implementation to complete normal component removal
        super.removeNotify();
        compoundList.removeChangeListener(this);
    }

    @Override
    public String getDescription() {
        return getDescriptionString();
    }

    public static String getDescriptionString() {
        return "Kendrick mass defect plot for homologue series.";
    }
}
