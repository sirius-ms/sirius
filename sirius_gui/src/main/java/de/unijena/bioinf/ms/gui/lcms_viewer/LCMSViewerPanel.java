package de.unijena.bioinf.ms.gui.lcms_viewer;

import de.unijena.bioinf.ChemistryBase.ms.lcms.CoelutingTraceSet;
import de.unijena.bioinf.ChemistryBase.ms.lcms.LCMSPeakInformation;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.ms.gui.utils.ToggableSidePanel;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.InstanceBean;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Optional;

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

    private Order order = Order.ALPHABETICALLY;

    public LCMSViewerPanel(FormulaList siriusResultElements) {
        // set content
        this.toolbar = new JToolBar(JToolBar.HORIZONTAL);
        setLayout(new BorderLayout());
        add(toolbar, BorderLayout.NORTH);
        this.lcmsWebview = new LCMSWebview();
        this.add(lcmsWebview, BorderLayout.CENTER);

        summaryPanel = new LCMSCompoundSummaryPanel();
        this.add(new ToggableSidePanel("quality report", summaryPanel), BorderLayout.EAST);

        JLabel label = new JLabel("Order samples ");
        toolbar.add(label);
        ButtonGroup group = new ButtonGroup();
        for (Order o : Order.values()) {
            JRadioButton button = new JRadioButton(new SetOrder(o));
            if(o==order) button.setSelected(true);
            group.add(button);
            toolbar.add(button);
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
        lcmsWebview.setInstance(currentInstance.getClient().features().getTraces1(currentInstance.getProjectManager().projectId,currentInstance.getFeatureId()),
                order
                );
        updateInfo();
    }

    public void setActiveIndex(int id) {
        if (id != activeIndex) {
            activeIndex = id;
            //lcmsWebview.setSampleIndex(activeIndex);
            updateInfo();
            invalidate();
        }
    }

    private void updateInfo() {
        //todo nightsky: fill with new LCMS data
        //final Optional<CoelutingTraceSet> trace = activeIndex < currentInfo.length() ? currentInfo.getTracesFor(activeIndex) : Optional.empty();
//        if (trace.isPresent())
//            summaryPanel.set(trace.get(), currentInstance.getExperiment());
//        else summaryPanel.reset();
    }
}
