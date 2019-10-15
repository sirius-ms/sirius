package de.unijena.bioinf.ms.gui.fingerid.candidate_filters;

import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;
import de.unijena.bioinf.ms.gui.fingerid.FingerprintCandidatePropertyChangeSupport;
import de.unijena.bioinf.ms.gui.fingerid.StructureSearcher;
import de.unijena.bioinf.ms.gui.utils.ToolbarToggleButton;

/**
 * Created by tkoehl on 19.07.18.
 */
public class MolecularPropertyMatcherEditor extends AbstractMatcherEditor<FingerprintCandidatePropertyChangeSupport> {
    StructureSearcher structureSearcher;

    public MolecularPropertyMatcherEditor(ToolbarToggleButton filterByMolecularPropertyButton) {
        super();
        filterByMolecularPropertyButton.addActionListener(propertyChangeEvent -> fireChanged(new MolecularPropertyMatcher(filterByMolecularPropertyButton.isSelected(), this.structureSearcher)));
    }

    public void setStructureSearcher(StructureSearcher structureSearcher) {
        this.structureSearcher = structureSearcher;
    }

    public void highlightChanged(boolean buttonIsSelected) {
        fireChanged(new MolecularPropertyMatcher(buttonIsSelected, this.structureSearcher));

    }

    public static class MolecularPropertyMatcher implements Matcher<FingerprintCandidatePropertyChangeSupport> {
        boolean filterIsActiv;
        int id;

        public MolecularPropertyMatcher(boolean buttonIsSelected, StructureSearcher structureSearcher) {
            if (structureSearcher != null) {
                id = structureSearcher.highlight;
            } else {
                id = 0;
            }
            if (buttonIsSelected && id > 0) {
                filterIsActiv = true;
            } else {
                filterIsActiv = false;
            }
        }

        @Override
        public boolean matches(FingerprintCandidatePropertyChangeSupport candidate) {
            if (filterIsActiv) {
                return candidate.hasFingerprintIndex(id);
            } else {
                return true;
            }
        }
    }
}
