/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.ms.gui.dialogs;

import de.unijena.bioinf.ms.gui.mainframe.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.net.URL;

/**
 * Used by the hidden {@code aot-train} installation run. The full GUI is rendered normally so the
 * Project Leyden AOT cache covers the real startup path, but the user must not mistake it for the
 * application launching, nor interact with the transient window. This:
 * <ol>
 *   <li>installs a transparent, event-swallowing glass pane so the fully rendered window stays
 *       visible ("watch it optimize") but cannot be interacted with, and</li>
 *   <li>shows a branded, always-on-top window that states this is a one-time installation step
 *       which closes by itself.</li>
 * </ol>
 * Both are torn down automatically when the training run shuts down (all windows are disposed on
 * context close), so no explicit teardown handle is required.
 */
public final class AotTrainingOverlay {

    private AotTrainingOverlay() {
    }

    public static void show(JFrame frame) {
        blockInteraction(frame);

        JWindow overlay = new JWindow(frame);
        overlay.setAlwaysOnTop(true);

        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        URL iconUrl = MainFrame.class.getResource("/icons/sirius_splash_scribble.gif");
        if (iconUrl != null) {
            // Scaled so the overlay is a clearly visible card (but still smaller than the full splash).
            Image scaled = new ImageIcon(iconUrl).getImage().getScaledInstance(460, -1, Image.SCALE_SMOOTH);
            content.add(new JLabel(new ImageIcon(scaled), SwingConstants.CENTER), BorderLayout.NORTH);
        }

        JLabel title = new JLabel("Finalizing SIRIUS installation");
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 6f));
        title.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel message = new JLabel("<html><div style='text-align:center;'>"
                + "Optimizing startup performance for your system.<br>"
                + "This one-time step runs automatically and closes when finished &ndash; no action needed."
                + "</div></html>");
        message.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel text = new JPanel(new BorderLayout(0, 6));
        text.setOpaque(false);
        text.add(title, BorderLayout.NORTH);
        text.add(message, BorderLayout.CENTER);
        content.add(text, BorderLayout.CENTER);

        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setStringPainted(true);
        bar.setString("Optimizing startup…");
        content.add(bar, BorderLayout.SOUTH);

        overlay.setContentPane(content);
        overlay.pack();
        overlay.setLocationRelativeTo(null);
        overlay.setVisible(true);
        overlay.toFront();
    }

    private static void blockInteraction(JFrame frame) {
        // A visible glass pane with empty listeners swallows mouse and keyboard events targeted at
        // the window beneath it, so the rendered UI remains visible but non-interactive.
        JComponent blocker = new JPanel();
        blocker.setOpaque(false);
        MouseAdapter swallow = new MouseAdapter() {
        };
        blocker.addMouseListener(swallow);
        blocker.addMouseMotionListener(swallow);
        blocker.addMouseWheelListener(e -> {
        });
        blocker.addKeyListener(new KeyAdapter() {
        });
        blocker.setFocusable(true);
        frame.setGlassPane(blocker);
        blocker.setVisible(true);

        // Keep keyboard focus away from the training window (it belongs to the overlay, if anywhere).
        frame.setFocusableWindowState(false);
    }
}
