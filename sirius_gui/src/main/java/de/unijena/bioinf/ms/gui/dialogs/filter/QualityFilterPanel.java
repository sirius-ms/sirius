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

    public void updateModel(QualityFilter qualityFilter) {
        for (int k = 0; k < qualityBoxes.length; ++k)
            qualityFilter.setQualitySelected(k, qualityBoxes[k].isSelected());
    }
}
