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
import java.awt.image.BufferedImage;

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
     * {@code ...} ellipsis, an over-long label is drawn in full into an off-screen image whose right
     * edge is then erased with a horizontal alpha gradient, so the text fades softly into the chip
     * fill (background-independent, since we erase the text's own alpha rather than paint over it).
     * The full text stays available as the chip's hover tooltip.
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
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D ig = img.createGraphics();
            try {
                ig.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                ig.setFont(getFont());
                ig.setColor(getForeground());
                FontMetrics fm = ig.getFontMetrics();
                Insets in = getInsets();
                int baseline = in.top + (h - in.top - in.bottom - fm.getHeight()) / 2 + fm.getAscent();
                ig.drawString(getText(), in.left, baseline);

                int fade = Math.min(FADE_WIDTH, w);
                ig.setComposite(AlphaComposite.DstOut);
                ig.setPaint(new GradientPaint(w - fade, 0, FADE_FROM, w, 0, FADE_TO));
                ig.fillRect(w - fade, 0, fade, h);
            } finally {
                ig.dispose();
            }
            g.drawImage(img, 0, 0, null);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (style == Style.USER) {
                g2.setColor(Colors.Menu.FILTER_BUTTON);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
            } else {
                Color base = Colors.Menu.FILTER_BUTTON;
                g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 36));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
                g2.setColor(base);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
            }
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
