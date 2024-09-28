package de.unijena.bioinf.ms.gui.lcms_viewer;

import de.unijena.bioinf.ChemistryBase.ms.lcms.CoelutingTraceSet;
import de.unijena.bioinf.ChemistryBase.ms.lcms.LCMSPeakInformation;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.ms.gui.utils.ToggableSidePanel;
import io.sirius.ms.sdk.model.AlignedFeatureQuality;
import io.sirius.ms.sdk.model.TraceSet;
import de.unijena.bioinf.ms.persistence.model.core.QualityReport;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class LCMSViewerPanel extends JPanel implements ActiveElementChangedListener<FormulaResultBean, InstanceBean> {

    private InstanceBean currentInstance;

    private LCMSWebview lcmsWebview;
    private JToolBar toolbar;
    private LCMSCompoundSummaryPanel summaryPanel;
    private int activeIndex;

    enum Order {
        ALPHABETICALLY("alphabetically"), BY_INTENSITY("by intensity");
        private final String label;
        Order(String label) {
            this.label = label;
        }
    }

    enum ViewType {
        ALIGNMENT("feature alignment"), COMPOUND("adduct/isotope assignment");
        private final String label;
        ViewType(String label) {
            this.label = label;
        }
    }

    private Order order = Order.ALPHABETICALLY;
    private ViewType viewType = ViewType.ALIGNMENT;

    public LCMSViewerPanel(FormulaList siriusResultElements) {
        // set content
        this.toolbar = new JToolBar(JToolBar.HORIZONTAL);
        setLayout(new BorderLayout());
        add(toolbar, BorderLayout.NORTH);
        this.lcmsWebview = new LCMSWebview();
        this.add(lcmsWebview, BorderLayout.CENTER);

        summaryPanel = new LCMSCompoundSummaryPanel();
        JScrollPane scrollpanel = new JScrollPane(summaryPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollpanel.setPreferredSize(new Dimension(400, 320));
        scrollpanel.setMaximumSize(new Dimension(400, Integer.MAX_VALUE));
        this.add(new ToggableSidePanel("quality report", scrollpanel), BorderLayout.EAST);

        {
            JLabel label = new JLabel("Show ");
            toolbar.add(label);
            ButtonGroup group = new ButtonGroup();
            for (ViewType o : ViewType.values()) {
                JRadioButton button = new JRadioButton(new SetViewType(o));
                if(o==viewType) button.setSelected(true);
                group.add(button);
                toolbar.add(button);
            }
        }
        toolbar.add(Box.createHorizontalStrut(18));
        {
            JLabel label = new JLabel("Order samples ");
            toolbar.add(label);
            ButtonGroup group = new ButtonGroup();
            for (Order o : Order.values()) {
                JRadioButton button = new JRadioButton(new SetOrder(o));
                if(o==order) button.setSelected(true);
                group.add(button);
                toolbar.add(button);
            }
        }
        // add listeners
        siriusResultElements.addActiveResultChangedListener(this);
    }

    private class SetOrder extends AbstractAction {

        Order value;

        public SetOrder(Order order) {
            super(order.label);
            this.value = order;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (order != value) {
                order = value;
                updateContent();
            }
        }
    }

    private class SetViewType extends AbstractAction {

        ViewType value;

        public SetViewType(ViewType order) {
            super(order.label);
            this.value = order;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (viewType != value) {
                viewType = value;
                updateContent();
            }
        }
    }

    public void reset() {
        lcmsWebview.reset();
        summaryPanel.reset();
    }

    public String getDescription() {
        return "<html>"
                +"<b>LC-MS and Data Quality Viewer</b>"
                +"<br>"
                + "Shows the chromatographic peak of the ion in LC-MS (left panel)"
                +"<br>"
                + "Shows data quality information (right panel)"
                +"<br>"
                + "Note: Only available if feature finding was performed by SIRIUS (mzml/mzXML)"
                + "</html>";
    }

    @Override
    public void resultsChanged(InstanceBean elementsParent, FormulaResultBean selectedElement, List<FormulaResultBean> resultElements, ListSelectionModel selections) {
        // we are only interested in changes of the experiment
        if (currentInstance!= elementsParent) {
            currentInstance = elementsParent;
            activeIndex = 0;
            updateContent();
        }
    }

    private void updateContent() {
        if (currentInstance==null) {
            reset();
            return;
        }

        CompletableFuture<AlignedFeatureQuality> future = currentInstance.getClient().experimental().getAlignedFeaturesQualityWithResponseSpec(currentInstance.getProjectManager().projectId, currentInstance.getFeatureId())
                .bodyToMono(AlignedFeatureQuality.class).onErrorComplete().toFuture();

        TraceSet spec;
        if (viewType==ViewType.ALIGNMENT) {
            spec = currentInstance.getClient().features().getTracesWithResponseSpec(currentInstance.getProjectManager().projectId, currentInstance.getFeatureId(), true).bodyToMono(TraceSet.class).onErrorComplete().block();
        } else {
            spec = currentInstance.getSourceFeature().getCompoundId()==null ? null : currentInstance.getClient().compounds().getCompoundTracesWithResponseSpec(currentInstance.getProjectManager().projectId, currentInstance.getSourceFeature().getCompoundId(), currentInstance.getFeatureId()).bodyToMono(TraceSet.class).onErrorComplete().block();
        }

        try {
            AlignedFeatureQuality alignedFeatureQuality = future.get();
            summaryPanel.setReport(alignedFeatureQuality);
        } catch (InterruptedException | ExecutionException e) {
            summaryPanel.setReport(null);
            throw new RuntimeException(e);
        }

        if (spec == null){
            reset();
            return;
        }

        lcmsWebview.setInstance(spec, order, viewType, currentInstance.getFeatureId());
    }

    public void setActiveIndex(int id) {
        if (id != activeIndex) {
            activeIndex = id;
            //lcmsWebview.setSampleIndex(activeIndex);
            invalidate();
        }
    }
}
