package de.unijena.bioinf.ms.gui.dialogs.filter;

import de.unijena.bioinf.ms.gui.utils.filter.QualityFilter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

class QualityFilterPanel extends JPanel {
    JCheckBox[] qualityBoxes;

    public QualityFilterPanel(@NotNull QualityFilter qualityFilterModel) {
        super();
        final BoxLayout groupLayout = new BoxLayout(this, BoxLayout.X_AXIS);
        setLayout(groupLayout);

        qualityBoxes = qualityFilterModel.getPossibleQualities().stream().map(JCheckBox::new).toArray(JCheckBox[]::new);
        for (int i = 0; i < qualityBoxes.length; ++i) {
            add(Box.createHorizontalGlue());
            add(qualityBoxes[i]);
            qualityBoxes[i].setSelected(qualityFilterModel.isQualitySelected(i));
        }
        add(Box.createHorizontalStrut(10));
    }

    public void reset() {
        for (JCheckBox jCheckBox : qualityBoxes)
            jCheckBox.setSelected(true);
    }

    /** Sets the checkboxes from the given quality filter's selection (mirrors the constructor). */
    public void setFromModel(@NotNull QualityFilter qualityFilter) {
        for (int i = 0; i < qualityBoxes.length; ++i)
            qualityBoxes[i].setSelected(qualityFilter.isQualitySelected(i));
    }

    public void updateModel(QualityFilter qualityFilter) {
        for (int k = 0; k < qualityBoxes.length; ++k)
            qualityFilter.setQualitySelected(k, qualityBoxes[k].isSelected());
    }

    /** Runs {@code onChange} whenever any of the quality check boxes is toggled. */
    public void onChange(@NotNull Runnable onChange) {
        for (JCheckBox box : qualityBoxes)
            box.addActionListener(e -> onChange.run());
    }
}
