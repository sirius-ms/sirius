package de.unijena.bioinf.ms.gui.fingerid.candidate_filters;

import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.Matcher;
import de.unijena.bioinf.ms.gui.fingerid.FingerprintCandidateBean;
import de.unijena.bioinf.ms.gui.utils.ToolbarToggleButton;

import java.util.Optional;

import static de.unijena.bioinf.fingerid.AddExternalStructureJJob.SKETCHED_DB_NAME;

public class SketchedFilter extends AbstractMatcherEditor<FingerprintCandidateBean> {

    public SketchedFilter(ToolbarToggleButton button) {
        button.addActionListener(e -> fireChanged(new SketchedMatcher(button.isSelected())));
    }

    public static class SketchedMatcher implements Matcher<FingerprintCandidateBean> {
        boolean filter;

        public SketchedMatcher(boolean filter) {
            this.filter = filter;
        }

        @Override
        public boolean matches(FingerprintCandidateBean candidate) {
            if (filter) {
                return Optional.ofNullable(candidate.getCandidate().getDbLinks())
                        .map(links -> links.stream().anyMatch(link -> SKETCHED_DB_NAME.equals(link.getName())))
                        .orElse(false);
            } else {
                return true;
            }
        }
    }
}
