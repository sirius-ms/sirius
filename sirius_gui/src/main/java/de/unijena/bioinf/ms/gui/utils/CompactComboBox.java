package de.unijena.bioinf.ms.gui.utils;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.plaf.synth.SynthComboBoxUI;
import java.awt.*;
import java.util.function.Function;

/**
 * A compact combo box is a combobox with a simple label as field that extends to
 * a JList with custom cell renderer when clicking on it.
 *
 * This class is a really dirty hack and might make trouble as soon as we change
 * anything in the look and feel
 */
public class CompactComboBox<T> extends JComboBox<T> {

    private Function<T,String> labeling;
    private ListCellRenderer<T> detailedRenderer;
    private Dimension popupSize;

    public CompactComboBox(Dimension popupSize) {
        this(null, popupSize);
    }

    public CompactComboBox(ListCellRenderer<T> detailedRenderer, Dimension popupSize) {
        this(detailedRenderer, popupSize, Object::toString);
    }

    public CompactComboBox(ListCellRenderer<T> detailedRenderer, Dimension popupSize, Function<T,String> labeling) {
        super();
        setDetailedRenderer(detailedRenderer);
        setPopupSize(popupSize);
        setLabeling(labeling);
    }

    public Function<T, String> getLabeling() {
        return labeling;
    }

    public void setLabeling(Function<T, String> labeling) {
        this.labeling = labeling;
    }

    public ListCellRenderer<T> getDetailedRenderer() {
        return detailedRenderer;
    }

    public void setDetailedRenderer(ListCellRenderer<T> detailedRenderer) {
        this.detailedRenderer = detailedRenderer==null ? new DefaultRendererWrapped() : detailedRenderer;
        setRenderer(new DefaultRendererWrapped());
        evilUIHack();
    }

    public Dimension getPopupSize() {
        return popupSize;
    }

    public void setPopupSize(Dimension popupSize) {
        this.popupSize = popupSize;
        evilUIHack();
    }

    private void evilUIHack() {
        final CompactComboBox<T> myself = this;
        setUI(new SynthComboBoxUI(){
            @Override
            protected ComboPopup createPopup() {
                return new BasicComboPopup(this.comboBox){
                    @Override
                    protected Rectangle computePopupBounds(int px, int py, int pw, int ph) {
                        final Rectangle r = super.computePopupBounds(px, py, pw, ph);
                        r.setSize((int)popupSize.getWidth() + 20 /*scrollbar*/, (int)popupSize.getHeight());
                        return r;
                    }

                    @Override
                    protected void configureList() {
                        super.configureList();
                        list.setCellRenderer((ListCellRenderer<Object>)myself.detailedRenderer);
                    }
                };
            }
        });
    }

    protected class DefaultRendererWrapped implements ListCellRenderer<T> {
        private final DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();
        @Override
        public Component getListCellRendererComponent(JList<? extends T> list, T value, int index, boolean isSelected, boolean cellHasFocus) {
            return defaultRenderer.getListCellRendererComponent(list,labeling.apply(value),index,isSelected,cellHasFocus);
        }
    }

}
