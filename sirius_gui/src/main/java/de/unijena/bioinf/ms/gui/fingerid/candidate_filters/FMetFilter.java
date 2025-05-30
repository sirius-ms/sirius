package de.unijena.bioinf.ms.gui.fingerid.candidate_filters;

import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;
import de.unijena.bioinf.ChemistryBase.ms.FunctionalMetabolomics;
import de.unijena.bioinf.ms.gui.fingerid.FingerprintCandidateBean;
import de.unijena.bioinf.ms.gui.utils.ToolbarToggleButton;

public class FMetFilter extends AbstractMatcherEditor<FingerprintCandidateBean>  {


    public FMetFilter(ToolbarToggleButton fmetFilter) {
        fmetFilter.addActionListener(propertyChangeEvent -> fireChanged(new FMetMatcher(fmetFilter.isSelected())));
    }

    public static class FMetMatcher implements Matcher<FingerprintCandidateBean> {
        boolean filterIsActiv;

        public FMetMatcher(boolean filterIsActiv) {
            this.filterIsActiv = filterIsActiv;
        }

        @Override
        public boolean matches(FingerprintCandidateBean candidate) {
            if (filterIsActiv) {
                return candidate.getCandidate().getDbLinks().stream().noneMatch(x->x.getName().equals(FunctionalMetabolomics.REJECT));
            } else {
                return true;
            }
        }
    }

}
