package de.unijena.bioinf.ms.gui.utils.search;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private static BufferedImage render(JComponent c, int w, int h) {
        BufferedImage img = new BufferedImage((int) Math.ceil((w + 6) * SCALE), (int) Math.ceil((h + 6) * SCALE),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.scale(SCALE, SCALE);
        g.translate(5, 3); // a fractional device offset (5 * 1.5 = 7.5), as real layouts produce
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
        int rightDev = (int) (size.width * SCALE);
        int fadeDev = (int) (FADE_WIDTH * SCALE);

        assertTrue(ink(img, 0, rightDev - fadeDev, hDev) > 0, "the label should have drawn some text");
        assertTrue(ink(img, rightDev - fadeDev, rightDev - fadeDev / 2, hDev)
                        > ink(img, rightDev - fadeDev / 2, rightDev, hDev),
                "the strip has to get lighter towards the right edge");
        assertEquals(0, ink(img, rightDev - 2, rightDev, hDev), "the right edge has to be erased completely");
    }
}
