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
package de.unijena.bioinf.fteval;

import java.io.Console;

public class Shell implements Interact {

    private final Console console = System.console();

    public static Interact withAlternative() {
        if (System.console() != null) return new Shell();
        else return new StdOut();
    }

    @Override
    public void say(String s) {
        console.printf(s);
    }

    @Override
    public void sayln(String s) {
        console.printf(s + "\n");
    }

    @Override
    public boolean ask(String question) {
        console.printf(question + "\n");
        final String answer = console.readLine("Y(es)/N(o): ");
        return (answer.matches("^\\s*[Yy](?:es)?\\s*$"));
    }

    @Override
    public int choice(String question, String... choices) {
        while (true) {
            sayln(question);
            for (int i = 0; i < choices.length; ++i) {
                sayln((i + 1) + ".) " + choices[i]);
            }
            final String answer = console.readLine("Your choice (1 - " + choices.length + "): ");
            try {
                final int choice = Integer.parseInt(answer);
                if (choice >= 1 && choice <= choices.length) return choice - 1;
                else sayln("Invalid input: Please enter number from 1 to " + choices.length);
            } catch (NumberFormatException e) {
                sayln("Invalid input: Please enter number from 1 to " + choices.length);
            }
        }
    }
}
