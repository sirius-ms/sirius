package de.unijena.bioinf.ms.gui.fingerid;

import de.unijena.bioinf.ms.gui.configs.Colors;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;

import javax.swing.*;
import java.awt.*;

/**
 * Created by fleisch on 24.05.17.
 */
public class CandidateStructureCellRenderer implements ListCellRenderer<FingerprintCandidatePropertyChangeSupport> {
    private CompoundStructureImage image = new CompoundStructureImage(StandardGenerator.HighlightStyle.None);

    @Override
    public Component getListCellRendererComponent(JList<? extends FingerprintCandidatePropertyChangeSupport> list, FingerprintCandidatePropertyChangeSupport value, int index, boolean isSelected, boolean cellHasFocus) {
        JPanel nu =  new JPanel();
        image.molecule = value;
        image.backgroundColor = (index % 2 == 0 ? Colors.LIST_EVEN_BACKGROUND : Colors.LIST_UNEVEN_BACKGROUND);
        nu.setBackground(image.backgroundColor);
        nu.add(image);
        return nu;
    }
}
