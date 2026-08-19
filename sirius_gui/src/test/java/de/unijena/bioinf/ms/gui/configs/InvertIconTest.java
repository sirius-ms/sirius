package de.unijena.bioinf.ms.gui.configs;

import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which half of the invert glyph is filled.
 * <p>
 * Rendered rather than reasoned about: the filled half is an {@link java.awt.geom.Arc2D} whose extent runs
 * counter-clockwise, so the side it lands on is decided by the sign of an angle - the kind of thing that is
 * equally easy to get wrong and to not notice, since either way the icon looks like a half-filled circle.
 */
public class InvertIconTest {

    private static final int SIZE = 16;
    // The glyph as the icon authors it: a circle of this radius about this centre, in a 16-unit box that is
    // rendered here at 1:1, outlined with a 1.5-wide stroke.
    private static final double CENTRE = 8, RADIUS = 5.6;
    /** Well inside the outline, so the ring itself is never counted as fill. */
    private static final double INSIDE = RADIUS - 1.6;
    /** Ignores the antialiased pixels straddling the vertical diameter, which belong to neither half. */
    private static final double SEAM = 1.5;

    /**
     * How much ink the icon puts on each side of its vertical centre line.
     */
    private record Ink(int left, int right) {
    }

    private static Ink inkOf(InvertIcon icon) {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            icon.paintIcon(new JPanel(), g, 0, 0);
        } finally {
            g.dispose();
        }

        int left = 0, right = 0;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if ((image.getRGB(x, y) >>> 24) < 200) // only what is painted close to solid
                    continue;
                double dx = x + 0.5 - CENTRE, dy = y + 0.5 - CENTRE;
                if (Math.hypot(dx, dy) > INSIDE || Math.abs(dx) < SEAM)
                    continue; // the outline, and the seam along the diameter, belong to neither half
                if (dx < 0)
                    left++;
                else
                    right++;
            }
        }
        return new Ink(left, right);
    }

    /**
     * Counting only what lies inside the outline makes this absolute rather than a ratio: the filled half is
     * solid, and the other half is empty because there is nothing in a half circle but its fill.
     */
    private static void assertFilledOn(String side, int filled, int empty) {
        assertTrue(filled > 8, "the " + side + " half should be filled, but only " + filled
                + " pixels inside the outline were painted there");
        assertEquals(0, empty, "the other half should be empty inside the outline");
    }

    @Test
    public void testTheLeftHalfIsFilledWhileNotInverted() {
        Ink ink = inkOf(new InvertIcon(SIZE, Color.BLACK));

        assertFilledOn("left", ink.left(), ink.right());
    }

    @Test
    public void testTheFilledHalfSwitchesSidesWhenInverted() {
        Ink ink = inkOf(new InvertIcon(SIZE, Color.BLACK, true));

        assertFilledOn("right", ink.right(), ink.left());
    }

    /**
     * The state is set after construction in practice - the panel pushes it in when the query is inverted,
     * exactly as it pushes in the colour.
     */
    @Test
    public void testTheFilledHalfFollowsTheStateItIsGiven() {
        InvertIcon icon = new InvertIcon(SIZE, Color.BLACK);

        icon.setInverted(true);
        Ink afterInverting = inkOf(icon);
        assertFilledOn("right", afterInverting.right(), afterInverting.left());

        icon.setInverted(false);
        Ink afterUninverting = inkOf(icon);
        assertFilledOn("left", afterUninverting.left(), afterUninverting.right());
    }
}
