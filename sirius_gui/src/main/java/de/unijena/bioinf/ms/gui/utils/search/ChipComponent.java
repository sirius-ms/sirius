/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.utils.search;

import de.unijena.bioinf.ms.gui.configs.Colors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * A rounded tag/chip: label text, optional close button, optional click action. Filled chips are
 * committed query clauses and operators; outlined chips are the filter-dialog state rendered into
 * the bar.
 */
public class ChipComponent extends JPanel {

    public enum Style {
        /**
         * A committed clause or operator of the user's query - filled.
         */
        USER,
        /**
         * A structured filter from the filter dialog - outlined, so the two sources are
         * distinguishable at a glance.
         */
        MODEL
    }

    private static final int ARC = 14;
    /** Chips wider than this (text only) are faded out on the right instead of widening the row. */
    private static final int MAX_TEXT_WIDTH = 240;
    /** Width of the right-edge alpha fade applied to truncated chip text. */
    private static final int FADE_WIDTH = 22;

    private final Style style;
    /** The chip's accent colour (fill for USER, outline+wash for MODEL). Swapped to the inverted accent
     *  to tint a whole inverted query; defaults to the normal filter accent. */
    private Color accent = Colors.Menu.FILTER_BUTTON;

    public ChipComponent(@NotNull String text, @Nullable String tooltip, @NotNull Style style,
                         @Nullable Runnable onClick, @Nullable Runnable onClose) {
        super(new FlowLayout(FlowLayout.LEFT, 4, 2));
        this.style = style;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(1, 6, 1, onClose != null ? 2 : 6));

        FadingLabel label = new FadingLabel(text);
        label.setForeground(style == Style.USER ? Colors.Menu.FILTER_BUTTON_TEXT : Colors.FOREGROUND_DATA);
        add(label);
        // the tooltip (the fully-qualified lucene, or a full-text hint incl. the phrase) reveals the
        // full content on hover - important now that long chips fade out; keep whatever the caller set
        if (tooltip != null) {
            setToolTipText(tooltip);
            label.setToolTipText(tooltip);
        }

        if (onClick != null) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter clickListener = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    onClick.run();
                }
            };
            addMouseListener(clickListener);
            label.addMouseListener(clickListener);
        }

        if (onClose != null)
            add(closeLabel(label.getForeground(), "Remove", onClose));
    }

    /** Tints this chip to {@code accent} (e.g. the inverted accent for a whole inverted query). */
    public ChipComponent withAccent(@NotNull Color accent) {
        this.accent = accent;
        repaint();
        return this;
    }

    /**
     * A dimmed, italic {@code AND} label marking an implicit conjunction between chips that the user
     * cannot change (the filter-dialog filters among themselves, and the whole dialog state with the
     * search query). Visually distinct from the editable AND/OR operators of the user's own query.
     */
    public static JLabel implicitAndLabel() {
        JLabel label = new JLabel("AND");
        label.setFont(label.getFont().deriveFont(Font.ITALIC, label.getFont().getSize2D() - 1f));
        Color fg = UIManager.getColor("Label.disabledForeground");
        label.setForeground(fg != null ? fg : Color.GRAY);
        label.setToolTipText("Combined with AND (filter-panel filters always narrow the search)");
        label.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        return label;
    }

    /**
     * A small, unobtrusive close affordance for chips and groups: the light "×" glyph (U+00D7, not
     * the heavy "✕"), dimmed at rest and brightening on hover. Shares the chip's foreground colour
     * so it reads correctly on both the filled user chips and the outlined model chips.
     */
    public static JLabel closeLabel(@NotNull Color foreground, @Nullable String tooltip, @NotNull Runnable onClose) {
        JLabel close = new JLabel("×");
        close.setFont(close.getFont().deriveFont(Font.BOLD, close.getFont().getSize2D() + 3f));
        Color dimmed = new Color(foreground.getRed(), foreground.getGreen(), foreground.getBlue(), 150);
        close.setForeground(dimmed);
        close.setToolTipText(tooltip);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
        close.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onClose.run();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                close.setForeground(foreground);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                close.setForeground(dimmed);
            }
        });
        return close;
    }

    /**
     * A single-line text label capped at {@link #MAX_TEXT_WIDTH}: instead of hard-clipping or an
     * {@code ...} ellipsis, the right edge of an over-long label is erased with a horizontal alpha
     * gradient, so the text fades softly into the chip fill (background-independent, since we erase
     * the text's own alpha rather than paint over it). The full text stays available as the chip's
     * hover tooltip.
     * <p>
     * Erasing alpha needs an off-screen buffer, and an off-screen buffer costs sharpness on a scaled
     * desktop (see {@link #paintFadedStrip}). So only the {@link #FADE_WIDTH} wide strip that
     * actually fades goes through one; the rest of the text is drawn straight onto the surface.
     */
    private static final class FadingLabel extends JLabel {
        private static final Color FADE_FROM = new Color(0, 0, 0, 0);   // erase nothing
        private static final Color FADE_TO = new Color(0, 0, 0, 255);   // erase fully

        FadingLabel(@NotNull String text) {
            super(text);
        }

        /** The unconstrained text width (what the label would want without the cap). */
        private int naturalWidth() {
            return super.getPreferredSize().width;
        }

        boolean isTruncated() {
            return naturalWidth() > MAX_TEXT_WIDTH;
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return d.width > MAX_TEXT_WIDTH ? new Dimension(MAX_TEXT_WIDTH, d.height) : d;
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (!isTruncated()) {
                super.paintComponent(g);
                return;
            }
            int w = Math.max(1, getWidth());
            int h = Math.max(1, getHeight());
            int fade = Math.min(FADE_WIDTH, w);
            int solid = Math.max(0, w - fade); // everything left of this is drawn at full opacity

            Insets in = getInsets();
            FontMetrics fm = getFontMetrics(getFont());
            int baseline = in.top + (h - in.top - in.bottom - fm.getHeight()) / 2 + fm.getAscent();
            // cut on a character boundary: both halves draw the same glyphs at the same baseline, and
            // a glyph split across the cut could disagree about its sub-pixel position and show a seam
            int cut = charsFitting(fm, solid - in.left);
            if (cut > 0)
                paintSolidPart(g, getText().substring(0, cut), solid, h, in.left, baseline);
            paintFadedStrip(g, solid, fade, h, in.left, baseline);
        }

        /** Number of leading characters that fit fully into {@code limit} px. */
        private int charsFitting(FontMetrics fm, int limit) {
            String text = getText();
            int width = 0;
            for (int i = 0; i < text.length(); i++) {
                width += fm.charWidth(text.charAt(i));
                if (width > limit)
                    return i;
            }
            return text.length();
        }

        /**
         * The part of the text that is not faded, painted straight onto the given surface - so it
         * is rasterised at the device resolution and reads exactly like every other label.
         */
        private void paintSolidPart(Graphics g, String text, int solid, int h, int x, int baseline) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                applyTextHints(g2);
                g2.setFont(getFont());
                g2.setColor(getForeground());
                g2.clipRect(0, 0, solid, h); // safety net: nothing may bleed into the faded strip
                g2.drawString(text, x, baseline);
            } finally {
                g2.dispose();
            }
        }

        /**
         * The faded strip: the text is drawn into an off-screen buffer whose right edge is then
         * erased with an alpha gradient (which is why it cannot be painted directly - erasing on the
         * surface would take the chip fill with it).
         * <p>
         * The buffer is allocated at <em>device</em> resolution and blitted back one image pixel per
         * device pixel. A buffer in user-space pixels would be stretched by the desktop scale factor
         * on the way back (Windows 125% / 150%), which is what made these labels look blurry.
         */
        private void paintFadedStrip(Graphics g, int solid, int fade, int h, int x, int baseline) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                AffineTransform tx = g2.getTransform();
                // a rotated/sheared surface has no meaningful device grid to align to: fall back to
                // a plain user-space buffer there (it is only ever the identity or a scale in Swing)
                boolean scaleOnly = tx.getShearX() == 0 && tx.getShearY() == 0
                        && tx.getScaleX() > 0 && tx.getScaleY() > 0;
                double sx = scaleOnly ? tx.getScaleX() : 1;
                double sy = scaleOnly ? tx.getScaleY() : 1;

                BufferedImage img = new BufferedImage(
                        Math.max(1, (int) Math.ceil(fade * sx)),
                        Math.max(1, (int) Math.ceil(h * sy)),
                        BufferedImage.TYPE_INT_ARGB);
                Graphics2D ig = img.createGraphics();
                try {
                    ig.scale(sx, sy); // everything below stays in user-space coordinates
                    applyTextHints(ig);
                    ig.setFont(getFont());
                    ig.setColor(getForeground());
                    ig.drawString(getText(), x - solid, baseline);
                    // erase the text's own alpha towards the right edge, so it fades into whatever
                    // the chip is filled with rather than into a colour we would have to know
                    ig.setComposite(AlphaComposite.DstOut);
                    ig.setPaint(new GradientPaint(0, 0, FADE_FROM, fade, 0, FADE_TO));
                    ig.fillRect(0, 0, fade, h);
                } finally {
                    ig.dispose();
                }

                if (scaleOnly) {
                    // drop to device coordinates for the blit: at a fractional device offset Java2D
                    // would resample the strip instead of copying it
                    g2.setTransform(new AffineTransform(1, 0, 0, 1,
                            Math.round(tx.getTranslateX() + solid * sx), Math.round(tx.getTranslateY())));
                    g2.drawImage(img, 0, 0, null);
                } else {
                    g2.drawImage(img, solid, 0, fade, h, null);
                }
            } finally {
                g2.dispose();
            }
        }

        /**
         * Renders text the way the desktop asks for it (LCD sub-pixel antialiasing where available)
         * instead of the plain grey antialiasing a bare {@code Graphics2D} would use.
         */
        private static void applyTextHints(Graphics2D g) {
            Map<?, ?> hints = (Map<?, ?>) Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
            if (hints != null)
                g.addRenderingHints(hints);
            else
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (style == Style.USER) {
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
            } else {
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 36));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
                g2.setColor(accent);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
            }
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
