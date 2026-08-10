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

    private final Style style;

    public ChipComponent(@NotNull String text, @Nullable String tooltip, @NotNull Style style,
                         @Nullable Runnable onClick, @Nullable Runnable onClose) {
        super(new FlowLayout(FlowLayout.LEFT, 4, 2));
        this.style = style;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(1, 6, 1, onClose != null ? 2 : 6));

        JLabel label = new JLabel(text);
        label.setForeground(style == Style.USER ? Colors.Menu.FILTER_BUTTON_TEXT : Colors.FOREGROUND_DATA);
        add(label);
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
