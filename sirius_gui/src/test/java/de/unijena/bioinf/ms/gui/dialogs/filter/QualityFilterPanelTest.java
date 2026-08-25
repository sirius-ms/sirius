package de.unijena.bioinf.ms.gui.dialogs.filter;

import de.unijena.bioinf.ms.gui.utils.filter.FeatureFilterModel;
import de.unijena.bioinf.ms.gui.utils.filter.QualityFilter;
import io.sirius.ms.sdk.model.DataQuality;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * The quality slider has to show exactly the selection it was given. Getting this wrong is easy to miss, because
 * a wrong range still looks plausible - it just quietly widens the filter.
 */
public class QualityFilterPanelTest {

    @BeforeEach
    public void requiresADisplay() {
        // RangeSliderUI renders its thumb in its constructor, which JSlider triggers from its own constructor
        // via updateUI(), so the component cannot even be instantiated without a screen device. These tests
        // therefore only run where there is a display and are skipped on a headless build agent.
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display: RangeSlider cannot be built headless");
    }

    private static QualityFilter filterWith(DataQuality... selected) {
        QualityFilter filter = new QualityFilter("quality", "Quality", new FeatureFilterModel());
        Set<DataQuality> keep = selected.length == 0 ? EnumSet.noneOf(DataQuality.class) : EnumSet.copyOf(List.of(selected));
        for (DataQuality quality : DataQuality.values())
            if (quality != DataQuality.NOT_APPLICABLE)
                filter.setQualitySelected(quality.ordinal() - 1, keep.contains(quality));
        return filter;
    }

    /** Round trip: build a panel from a selection, read it back, and expect the same selection. */
    private static Set<DataQuality> roundTrip(DataQuality... selected) {
        QualityFilter source = filterWith(selected);
        QualityFilterPanel panel = new QualityFilterPanel(source);
        QualityFilter target = filterWith();
        panel.updateModel(target);
        return target.getDataQualities();
    }

    @Test
    public void theTopOfTheScaleRoundTrips() {
        assertEquals(EnumSet.of(DataQuality.DECENT, DataQuality.GOOD),
                roundTrip(DataQuality.DECENT, DataQuality.GOOD));
    }

    @Test
    public void theBottomOfTheScaleRoundTrips() {
        assertEquals(EnumSet.of(DataQuality.LOWEST, DataQuality.BAD),
                roundTrip(DataQuality.LOWEST, DataQuality.BAD));
    }

    @Test
    public void aSingleQualityRoundTrips() {
        assertEquals(EnumSet.of(DataQuality.GOOD), roundTrip(DataQuality.GOOD));
        assertEquals(EnumSet.of(DataQuality.LOWEST), roundTrip(DataQuality.LOWEST));
        assertEquals(EnumSet.of(DataQuality.DECENT), roundTrip(DataQuality.DECENT));
    }

    @Test
    public void theFullScaleRoundTrips() {
        assertEquals(EnumSet.of(DataQuality.LOWEST, DataQuality.BAD, DataQuality.DECENT, DataQuality.GOOD),
                roundTrip(DataQuality.LOWEST, DataQuality.BAD, DataQuality.DECENT, DataQuality.GOOD));
    }

    @Test
    public void setFromModelAppliesLaterChangesToo() {
        // the dialog reuses one panel: it is built once and re-filled whenever the model changes
        QualityFilterPanel panel = new QualityFilterPanel(filterWith(DataQuality.LOWEST, DataQuality.BAD));
        panel.setFromModel(filterWith(DataQuality.DECENT, DataQuality.GOOD));
        QualityFilter target = filterWith();
        panel.updateModel(target);
        assertEquals(EnumSet.of(DataQuality.DECENT, DataQuality.GOOD), target.getDataQualities());
    }

    @Test
    public void resetSelectsTheWholeScale() {
        QualityFilterPanel panel = new QualityFilterPanel(filterWith(DataQuality.GOOD));
        panel.reset();
        QualityFilter target = filterWith();
        panel.updateModel(target);
        assertEquals(EnumSet.of(DataQuality.LOWEST, DataQuality.BAD, DataQuality.DECENT, DataQuality.GOOD),
                target.getDataQualities());
    }

    @Test
    public void aNonContiguousSelectionIsWidenedToItsSpan() {
        // a slider cannot express a gap; widening is the safe direction, dropping the ends is not
        assertEquals(EnumSet.of(DataQuality.LOWEST, DataQuality.BAD, DataQuality.DECENT, DataQuality.GOOD),
                roundTrip(DataQuality.LOWEST, DataQuality.GOOD));
    }
}
