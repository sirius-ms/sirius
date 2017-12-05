/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.ms.cli;

import de.unijena.bioinf.sirius.Feedback;
import de.unijena.bioinf.sirius.Progress;

import java.io.PrintStream;
import java.util.Locale;

/**
 * Created by kaidu on 20.04.2015.
 */
public class ShellProgress implements Progress {

    private boolean shellMode;
    private PrintStream writer;

    private static char FRAME = '|';
    private static char UNREACHED = '.';
    private static char REACHED = '=';
    private static int width=20;

    private double max;

    private StringBuilder buffer;

    private int fillEmptySpace=0;
    private int prevPerc=0;

    public ShellProgress(PrintStream writer, boolean shellMode) {
        this.shellMode = shellMode;
        this.writer = writer;
        this.buffer = new StringBuilder(160);

    }

    @Override
    public void init(double maxProgress) {
        if (!shellMode) return;
        clearBuffer();
        draw(0, maxProgress, "start computing");
        this.max = maxProgress;
        prevPerc=0;
    }

    @Override
    public void update(double currentProgress, double maxProgress, String value, Feedback feedback) {
        if (!shellMode) {
            writer.println(value);
        } else {
            clearBuffer();
            draw(currentProgress, maxProgress, value);
        }
    }

    private void draw(double currentProgress, double maxProgress, String value) {

        final double percentage = width * currentProgress / maxProgress;
        final int perc100 = (int)Math.max(0,Math.min(100, Math.round(percentage*5)));

        buffer.append(FRAME);
        for (int k=0; k < width/2; ++k) buffer.append(k >= percentage ? UNREACHED : REACHED);

        buffer.append(String.format(Locale.US, "%3d %%", perc100));
        for (int k=width/2; k < width; ++k) buffer.append(k >= percentage ? UNREACHED : REACHED);
        buffer.append(FRAME);
        buffer.append('\t');
        if (value.length() < buffer.length()) {
            final int indent = (buffer.length()-1-value.length())/2;
            for (int i=0; i < indent; ++i) buffer.append(' ');
        }
        buffer.append(value);
        writer.print(buffer);
        for (int k=buffer.length(); k < fillEmptySpace; ++k) {
            writer.print(' ');
        }
        fillEmptySpace=0;
         writer.flush();
    }

    private void clearBuffer() {
        writer.print('\r');
        fillEmptySpace=buffer.length();
        buffer.delete(0, buffer.length());
    }

    @Override
    public void finished() {
        if (!shellMode) return;
        clearBuffer();
        draw(max, max, "computation finished");
        writer.print("\n");
    }

    @Override
    public void info(String message) {
        writer.println();
        writer.println(message);
        buffer.delete(0, buffer.length());
    }
}
