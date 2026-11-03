package de.unijena.bioinf.ms.gui.utils.search;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The non-faded part of a truncated chip label must be rasterised at the device resolution: on a
 * scaled desktop (Windows 125% / 150%) an off-screen buffer in user-space pixels is stretched on the
 * way back and the text turns blurry.
 */
public class ChipLabelRenderingTest {
    private static final double SCALE = 1.5;
    private static final String TEXT = "(quality : DECENT OR quality : GOOD OR quality : BAD OR quality : LOWEST)";
    private static final int FADE_WIDTH = 22;
    /** The offset render() draws the component at, in user-space px (x is deliberately fractional at 1.5x). */
    private static final int OFFSET_X = 5;

    private static BufferedImage render(JComponent c, int w, int h) {
        BufferedImage img = new BufferedImage((int) Math.ceil((w + 6) * SCALE), (int) Math.ceil((h + 6) * SCALE),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.scale(SCALE, SCALE);
        g.translate(OFFSET_X, 3); // a fractional device offset (5 * 1.5 = 7.5), as real layouts produce
        c.setSize(w, h);
        c.paint(g);
        g.dispose();
        return img;
    }

    /** What the old implementation did: draw into a user-space buffer, blit it back scaled. */
    private static BufferedImage renderViaUserSpaceBuffer(JLabel label, int w, int h) {
        BufferedImage img = new BufferedImage((int) Math.ceil((w + 6) * SCALE), (int) Math.ceil((h + 6) * SCALE),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.scale(SCALE, SCALE);
        g.translate(5, 3);
        BufferedImage buffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D bg = buffer.createGraphics();
        bg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        bg.setFont(label.getFont());
        bg.setColor(label.getForeground());
        FontMetrics fm = bg.getFontMetrics();
        bg.drawString(label.getText(), 0, (h - fm.getHeight()) / 2 + fm.getAscent());
        bg.dispose();
        g.drawImage(buffer, 0, 0, null);
        g.dispose();
        return img;
    }

    /** Mean per-pixel alpha distance over the region that must be crisp. */
    private static double meanAlphaDiff(BufferedImage a, BufferedImage b, int wDev, int hDev) {
        long sum = 0;
        int n = 0;
        for (int y = 0; y < Math.min(hDev, Math.min(a.getHeight(), b.getHeight())); y++)
            for (int x = 0; x < Math.min(wDev, Math.min(a.getWidth(), b.getWidth())); x++) {
                sum += Math.abs((a.getRGB(x, y) >>> 24) - (b.getRGB(x, y) >>> 24));
                n++;
            }
        return n == 0 ? 0 : (double) sum / n;
    }

    @Test
    public void nonFadedPartIsRasterisedAtDeviceResolution() {
        ChipComponent chip = new ChipComponent(TEXT, null, ChipComponent.Style.USER, null, null);
        JComponent chipLabel = (JComponent) chip.getComponent(0);
        chipLabel.setForeground(Color.BLACK);
        Dimension size = chipLabel.getPreferredSize();

        // ground truth: the same text drawn by a plain JLabel, wide enough not to be truncated
        JLabel plain = new JLabel(TEXT);
        plain.setFont(chipLabel.getFont());
        plain.setForeground(Color.BLACK);
        Dimension plainSize = plain.getPreferredSize();

        int solidDev = (int) ((size.width - FADE_WIDTH - 2) * SCALE); // crisp part, minus a seam margin
        int hDev = (int) (size.height * SCALE);

        BufferedImage reference = render(plain, plainSize.width, plainSize.height);
        double current = meanAlphaDiff(render(chipLabel, size.width, size.height), reference, solidDev, hDev);
        double userSpaceBuffer = meanAlphaDiff(renderViaUserSpaceBuffer(plain, plainSize.width, plainSize.height),
                reference, solidDev, hDev);

        System.out.printf("mean alpha distance to a plain JLabel at %.0f%% scale: chip=%.2f, user-space buffer=%.2f%n",
                SCALE * 100, current, userSpaceBuffer);
        assertTrue(current < userSpaceBuffer / 3,
                "chip label should be far closer to a plain JLabel than a user-space buffer blit is (chip="
                        + current + ", buffer=" + userSpaceBuffer + ")");
    }

    /** Device column of a user-space x inside the painted component (render() offsets by OFFSET_X). */
    private static int devX(int x) {
        return (int) Math.round((x + OFFSET_X) * SCALE);
    }

    private static int maxAlpha(BufferedImage img, int fromX, int toX, int hDev) {
        int max = 0;
        for (int y = 0; y < Math.min(hDev, img.getHeight()); y++)
            for (int x = Math.max(0, fromX); x < Math.min(toX, img.getWidth()); x++)
                max = Math.max(max, img.getRGB(x, y) >>> 24);
        return max;
    }

    /** Sum of the alpha of all pixels in a column range - "how much ink is there". */
    private static long ink(BufferedImage img, int fromX, int toX, int hDev) {
        long sum = 0;
        for (int y = 0; y < Math.min(hDev, img.getHeight()); y++)
            for (int x = Math.max(0, fromX); x < Math.min(toX, img.getWidth()); x++)
                sum += (img.getRGB(x, y) >>> 24);
        return sum;
    }

    @Test
    public void overlongTextStillFadesOutTowardsTheRightEdge() {
        ChipComponent chip = new ChipComponent(TEXT, null, ChipComponent.Style.USER, null, null);
        JComponent chipLabel = (JComponent) chip.getComponent(0);
        chipLabel.setForeground(Color.BLACK);
        Dimension size = chipLabel.getPreferredSize();
        BufferedImage img = render(chipLabel, size.width, size.height);

        int hDev = (int) (size.height * SCALE);
        int stripStart = devX(size.width - FADE_WIDTH); // where the alpha gradient begins
        int stripMid = devX(size.width - FADE_WIDTH / 2);
        int rightEdge = devX(size.width);

        assertTrue(ink(img, 0, stripStart, hDev) > 0, "the label should have drawn some text");
        assertTrue(ink(img, stripStart, stripMid, hDev) > ink(img, stripMid, rightEdge, hDev),
                "the strip has to get lighter towards the right edge");
        // not "fully erased": the gradient only reaches full strength at the very edge, so the last
        // columns keep a few percent of whatever glyph happens to land there - which font is used
        // decides whether that is anything at all. Losing the fade would leave them at full opacity.
        int edge = maxAlpha(img, rightEdge - 2, rightEdge, hDev);
        System.out.printf("strongest pixel left in the last two device columns: %d/255%n", edge);
        assertTrue(edge < 64, "the right edge has to be nearly erased, but its strongest pixel is "
                + edge + "/255");
    }
}
