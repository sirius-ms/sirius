package de.unijena.bioinf.sirius.gui.structure;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius_frontend
 * 18.01.17.
 */

import ca.odell.glazedlists.matchers.Matcher;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class ExperimentContainerMatcher implements Matcher<ExperimentContainer> {
    @Override
    public boolean matches(ExperimentContainer item) {
        return false;
    }
}
