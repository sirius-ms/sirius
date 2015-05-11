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
package de.unijena.bioinf.siriuscli;


import java.util.Arrays;
import java.util.regex.Pattern;

public class CLI {

    public final static Command[] COMMANDS = new Command[]{
        new TreeCommand(), new LearnCommand(), new CleanupCommand(), new DecompCommand(), new IsogenCommand(), new EvalCommand(), new AlignCommand(), new GraphBuilderCommand(),
            new QueryCommand()
    };

    public static void main(String[] args) {
        final Pattern helpPattern = Pattern.compile("-h|--help");
        for (int i=0; i < args.length; ++i ) {
            final String arg = args[i];
            if (helpPattern.matcher(arg).find()) {
                printHelp();
            } else {
                for (Command c : COMMANDS) {
                    if (c.getName().equalsIgnoreCase(arg)) {
                        final String[] commandParameters = Arrays.copyOfRange(args, i+1, args.length);
                        c.run(commandParameters);
                        return;
                    }
                }
                System.err.println("Unknown command '" + arg + "'");
                System.err.flush();
                printHelp();
            }
        }
        if (args.length==0) printHelp();
    }

    private static void printHelp() {
        System.out.println("SIRIUS command line tool\n" +
                "Available commands:");
        for (Command c : COMMANDS) {
            System.out.println("\t" + c.getName() + "\t=>" + c.getDescription());
        }
        System.out.println("\nWrite\nsirius <command> --help\nfor details");
    }

}
