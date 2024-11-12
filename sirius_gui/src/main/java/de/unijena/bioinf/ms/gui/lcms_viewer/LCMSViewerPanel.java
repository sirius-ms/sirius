package de.unijena.bioinf.ms.gui.lcms_viewer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.ms.gui.utils.ToggableSidePanel;
import de.unijena.bioinf.ms.gui.utils.loading.Loadable;
import de.unijena.bioinf.ms.gui.utils.loading.LoadablePanel;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;
import io.sirius.ms.sdk.model.AlignedFeatureQuality;
import io.sirius.ms.sdk.model.Trace;
import io.sirius.ms.sdk.model.TraceSet;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LCMSViewerPanel extends JPanel implements ActiveElementChangedListener<FormulaResultBean, InstanceBean>, Loadable {

    private InstanceBean currentInstance;

    private LCMSWebview lcmsWebview;
    private JToolBar toolbar;
    private LCMSCompoundSummaryPanel summaryPanel;
    private int activeIndex;

    interface Order {
        public String name();
    }
    enum FeatureOrder implements Order {
        ALPHABETICALLY("alphabetically"), BY_INTENSITY("by intensity");
        private final String label;
        FeatureOrder(String label) {
            this.label = label;
        }
    }

    enum AdductOrder implements Order {
        ALPHABETICALLY_MAIN_FIRST;
    }

    enum ViewType {
        ALIGNMENT("feature alignment"), COMPOUND("adduct/isotope assignment");
        private final String label;
        ViewType(String label) {
            this.label = label;
        }
    }

    private Order order = FeatureOrder.ALPHABETICALLY;
    private ViewType viewType = ViewType.ALIGNMENT;
    private final LoadablePanel loadable;

    private final ButtonGroup orderSelectionGroup;
    private final JLabel orderLabel;

    public LCMSViewerPanel(FormulaList siriusResultElements) {
        // set content
        this.toolbar = new JToolBar(JToolBar.HORIZONTAL);
        setLayout(new BorderLayout());
        add(toolbar, BorderLayout.NORTH);
        this.lcmsWebview = new LCMSWebview();
        this.loadable = new LoadablePanel(lcmsWebview);
        this.add(loadable, BorderLayout.CENTER);

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
            orderLabel = new JLabel("Order samples ");
            toolbar.add(orderLabel);
            orderSelectionGroup = new ButtonGroup();
            for (FeatureOrder o : FeatureOrder.values()) {
                JRadioButton button = new JRadioButton(new SetOrder(o));
                if(o==order) button.setSelected(true);
                orderSelectionGroup.add(button);
                toolbar.add(button);
            }
        }
        // add listeners
        siriusResultElements.addActiveResultChangedListener(this);
    }

    @Override
    public boolean setLoading(boolean loading, boolean absolute) {
        return loadable.setLoading(loading, absolute);
    }

    private class SetOrder extends AbstractAction {

        Order value;

        public SetOrder(FeatureOrder order) {
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
                orderLabel.setVisible(viewType != ViewType.COMPOUND);
                Collections.list(orderSelectionGroup.getElements()).forEach(b -> b.setVisible(viewType != ViewType.COMPOUND));
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
        increaseLoading();
        try {
            // we are only interested in changes of the experiment
            if (currentInstance!= elementsParent) {
                currentInstance = elementsParent;
                activeIndex = 0;
                updateContent();
            }
        } finally {
            disableLoading();
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

        spec = ColoredTraceSet.buildTrace(spec, viewType);
        lcmsWebview.setInstance(spec, viewType == ViewType.ALIGNMENT ? order : AdductOrder.ALPHABETICALLY_MAIN_FIRST, viewType, currentInstance.getFeatureId());
    }

    public void setActiveIndex(int id) {
        if (id != activeIndex) {
            activeIndex = id;
            //lcmsWebview.setSampleIndex(activeIndex);
            invalidate();
        }
    }

    //-----------------------------------------------------------------------------------------------------------
    //-----------------------------------------------------------------------------------------------------------
    //-------- HELPER CLASSES TO ALLOW TRACE COLORING //todo coloring: this still gives colors on-the-fly and does not use stored colors.
    //-----------------------------------------------------------------------------------------------------------
    //-----------------------------------------------------------------------------------------------------------

    private static class ColoredTraceSet extends TraceSet {

        public static ColoredTraceSet buildTrace(TraceSet traceSet, ViewType viewType) {
            if (viewType == ViewType.ALIGNMENT) {
                return ((ColoredTraceSet)new ColoredTraceSet()
                        .sampleId(traceSet.getSampleId())
                        .sampleName(traceSet.getSampleName())
                        .axes(traceSet.getAxes())
                        .traces(IntStream.range(0, traceSet.getTraces().size()).mapToObj(i -> ColoredTrace.buildTrace(traceSet.getTraces().get(i), i)).collect(Collectors.toUnmodifiableList())));
            } else {
                return ((ColoredTraceSet)new ColoredTraceSet()
                        .sampleId(traceSet.getSampleId())
                        .sampleName(traceSet.getSampleName())
                        .axes(traceSet.getAxes())
                        .traces(traceSet.getTraces().stream().map(t -> {
                            String label = t.getLabel().toUpperCase();
                            boolean isIsotope = label.contains("ISOTOPE");
                            boolean isAdduct = label.contains("[CORRELATED]");
                            return ColoredTrace.buildTrace(t,
                                    isAdduct ? Colors.LCMSVIEW.ADDUCT_FEATURE_COLOR : Colors.LCMSVIEW.MAIN_FEATURE_COLOR,
                                    isIsotope ? Colors.LCMSVIEW.ISOTOPE_DASH_STYLE : null);
                        }).collect(Collectors.toUnmodifiableList())));
            }

        }
    }

    @JsonPropertyOrder({
            ColoredTrace.JSON_PROPERTY_ID,
            ColoredTrace.JSON_PROPERTY_SAMPLE_ID,
            ColoredTrace.JSON_PROPERTY_SAMPLE_NAME,
            ColoredTrace.JSON_PROPERTY_LABEL,
            ColoredTrace.JSON_PROPERTY_COLOR,
            ColoredTrace.JSON_PROPERTY_DASH_STYLE,
            ColoredTrace.JSON_PROPERTY_INTENSITIES,
            ColoredTrace.JSON_PROPERTY_ANNOTATIONS,
            ColoredTrace.JSON_PROPERTY_MZ,
            ColoredTrace.JSON_PROPERTY_MERGED,
            ColoredTrace.JSON_PROPERTY_NORMALIZATION_FACTOR,
            ColoredTrace.JSON_PROPERTY_NOISE_LEVEL
    })

    private static class ColoredTrace extends Trace {
        public static final String JSON_PROPERTY_COLOR = "color";
        private String color;

        public static final String JSON_PROPERTY_DASH_STYLE = "dashStyle";
        private String dashStyle;

        public static ColoredTrace buildTrace(Trace trace, int index) {
            return buildTrace(trace, Colors.LCMSVIEW.getFeatureTraceColor(index), "none");
        }

        public static ColoredTrace buildTrace(Trace trace, Color color, String dashStyle) {
            return ((ColoredTrace) new ColoredTrace()
                    .id(trace.getId())
                    .sampleId(trace.getSampleId())
                    .sampleName(trace.getSampleName())
                    .label(trace.getLabel())
                    .intensities(trace.getIntensities())
                    .annotations(trace.getAnnotations())
                    .mz(trace.getMz())
                    .merged(trace.isMerged())
                    .normalizationFactor(trace.getNormalizationFactor())
                    .noiseLevel(trace.getNoiseLevel()))
                    .color(color)
                    .dashStyle(dashStyle != null ? dashStyle : "none");
        }

        public ColoredTrace color(Color color) {
            this.color = Colors.asHex(color);
            return this;
        }

        public ColoredTrace dashStyle(String dashStyle) {
            this.dashStyle = dashStyle;
            return this;
        }

        /**
         * Get color
         * @return color
         **/
        @jakarta.annotation.Nullable
        @JsonProperty(JSON_PROPERTY_COLOR)
        @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

        public String getColor() {
            return color;
        }

        /**
         * Get dash style
         * @return dash style
         **/
        @jakarta.annotation.Nullable
        @JsonProperty(JSON_PROPERTY_DASH_STYLE)
        @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

        public String getDashStyle() {
            return dashStyle;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("class Trace {\n");
            sb.append("    id: ").append(toIndentedString(getId())).append("\n");
            sb.append("    sampleId: ").append(toIndentedString(getSampleId())).append("\n");
            sb.append("    sampleName: ").append(toIndentedString(getSampleName())).append("\n");
            sb.append("    label: ").append(toIndentedString(getLabel())).append("\n");
            sb.append("    color: ").append(toIndentedString(getColor())).append("\n");
            sb.append("    dashStyle: ").append(toIndentedString(getDashStyle())).append("\n");
            sb.append("    intensities: ").append(toIndentedString(getIntensities())).append("\n");
            sb.append("    annotations: ").append(toIndentedString(getAnnotations())).append("\n");
            sb.append("    mz: ").append(toIndentedString(getMz())).append("\n");
            sb.append("    merged: ").append(toIndentedString(isMerged())).append("\n");
            sb.append("    normalizationFactor: ").append(toIndentedString(getNormalizationFactor())).append("\n");
            sb.append("    noiseLevel: ").append(toIndentedString(getNoiseLevel())).append("\n");
            sb.append("}");
            return sb.toString();
        }
    }

}
