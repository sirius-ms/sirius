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
package de.unijena.bioinf.fingerid;

import com.lexicalscope.jewel.cli.CliFactory;
import de.unijena.bioinf.fingerid.fingerprints.*;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.*;

import java.io.*;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class FingerprinterCLI {

   public static void main(String[] args) {
       if (args.length==0) {
           System.out.println("Usage: fingerprinter [compute] --help");
           System.exit(0);
       }

       final String command = args[0].toLowerCase();
       final String[] rest = new String[args.length-1];
       System.arraycopy(args, 1, rest, 0, args.length-1);

       if (command.equals("compute")) {
           compute(rest);
       } else if (command.equals("genmask")) {
           genmask(rest);
       } else if (command.equals("mask")) {
           mask(rest);
       } else if (command.equals("unmask")) {
           unmask(rest);
       } else if (command.equals("rmpubchem")) {
           rmpubchem(rest);
       }
   }

    private static void rmpubchem(String[] rest) {
        ComputeOpts opts = CliFactory.createCli(ComputeOpts.class).parseArguments(rest);
        final List<File> input = opts.getInput();
        final Reader reader;
        if (input.size() < 1) {
            reader = new InputStreamReader(System.in);
        } else try {
            reader = new FileReader(input.get(0));
        } catch (FileNotFoundException e) {
            System.err.println("Cannot find file '" + input.get(0) + "'");
            System.exit(1);
            return;
        }
        final Writer writer;
        if (input.size() < 2) {
            writer = new OutputStreamWriter(System.out);
        } else try {
            writer = new FileWriter(input.get(1));
        } catch (IOException e) {
            System.err.println("Cannot write into file '" + input.get(1) + "'");
            System.exit(1);
            return;
        }
        final TableUtils tab = new TableUtils(reader, writer);

        final List<IFingerprinter> fingerprinters = getFingerprintList(opts);
        int offset = 0;
        for (IFingerprinter f : fingerprinters) {
            if (f instanceof PubchemFingerprinter) {
                break;
            } else offset += f.getSize();
        }

        try {
            if (!tab.nextRow()) return;
            tab.findFingerprintColumn();
            do {
                tab.replaceFingerprint(removePubchemFormulaBits(offset, tab.getFingerprint()));
                tab.writeColumn();
            } while (tab.nextRow());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                tab.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String removePubchemFormulaBits(int offset, String fingerprint) {
        final StringBuilder buf = new StringBuilder(fingerprint.length());
        buf.append(fingerprint, 0, offset);
        for (int i = offset; i < offset + 114; ++i) {
            buf.append('0');
        }
        buf.append(fingerprint, offset + 114, fingerprint.length());
        assert buf.length() == fingerprint.length();
        return buf.toString();
    }

    private static void unmask(String[] rest) {
        if (rest.length == 0) {
            System.err.println("Usage: mask maskfile < in > out");
            System.exit(0);
        }

        final Mask m;
        try {
            final BufferedReader maskFile = new BufferedReader(new FileReader(rest[0]));
            final String mstr = maskFile.readLine();
            maskFile.close();
            m = Mask.fromString(mstr.split("\t"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
            return;
        }

        final Reader reader;
        if (rest.length < 2) {
            reader = new InputStreamReader(System.in);
        } else try {
            reader = new FileReader(rest[0]);
        } catch (FileNotFoundException e) {
            System.err.println("Cannot find file '" + rest[0] + "'");
            System.exit(1);
            return;
        }
        final Writer writer;
        if (rest.length < 3) {
            writer = new OutputStreamWriter(System.out);
        } else try {
            writer = new FileWriter(rest[1]);
        } catch (IOException e) {
            System.err.println("Cannot write into file '" + rest[1] + "'");
            System.exit(1);
            return;
        }
        final TableUtils tab = new TableUtils(reader, writer);
        try {
            if (!tab.nextRow()) return;
            tab.findFingerprintColumn();
            do {
                tab.replaceFingerprint(m.unapply(tab.getFingerprint()));
            } while (tab.nextRow());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                tab.closeRead();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void mask(String[] rest) {

        if (rest.length == 0) {
            System.err.println("Usage: mask maskfile < in > out");
            System.exit(0);
        }

        final Mask m;
        try {
            final BufferedReader maskFile = new BufferedReader(new FileReader(rest[0]));
            final String mstr = maskFile.readLine();
            maskFile.close();
            m = Mask.fromString(mstr.split("\t"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
            return;
        }

        final Reader reader;
        if (rest.length < 2) {
            reader = new InputStreamReader(System.in);
        } else try {
            reader = new FileReader(rest[1]);
        } catch (FileNotFoundException e) {
            System.err.println("Cannot find file '" + rest[0] + "'");
            System.exit(1);
            return;
        }
        final Writer writer;
        if (rest.length < 3) {
            writer = new OutputStreamWriter(System.out);
        } else try {
            writer = new FileWriter(rest[2]);
        } catch (IOException e) {
            System.err.println("Cannot write into file '" + rest[1] + "'");
            System.exit(1);
            return;
        }
        final TableUtils tab = new TableUtils(reader, writer);
        try {
            if (!tab.nextRow()) return;
            tab.findFingerprintColumn();
            do {
                tab.replaceFingerprint(m.apply(tab.getFingerprint()));
                tab.writeColumn();
            } while (tab.nextRow());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                tab.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private static void genmask(String[] rest) {
        final Reader reader;
        if (rest.length == 0) {
            reader = new InputStreamReader(System.in);
        } else try {
            reader = new FileReader(rest[0]);
        } catch (FileNotFoundException e) {
            System.err.println("Cannot find file '" + rest[0] + "'");
            System.exit(1);
            return;
        }

        final TableUtils tab = new TableUtils(reader, null);
        final ArrayList<boolean[]> fps = new ArrayList<boolean[]>();
        try {
            if (!tab.nextRow()) return;
            tab.findFingerprintColumn();
            do {
                fps.add(tab.getFingerprintAsBoolean());
            } while (tab.nextRow());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                tab.closeRead();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        final Mask m = Mask.compute(fps.toArray(new boolean[fps.size()][]));

        final Writer writer;
        if (rest.length < 2) {
            writer = new OutputStreamWriter(System.out);
        } else try {
            writer = new FileWriter(rest[1]);
        } catch (IOException e) {
            System.err.println("Cannot write into file '" + rest[1] + "'");
            System.exit(1);
            return;
        }
        try {
            writer.write(m.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void compute(String[] args) {
        final ComputeOpts opts = CliFactory.createCli(ComputeOpts.class).parseArguments(args);
        final boolean is3d = opts.is3D();
        final boolean isStdout = opts.isStdout();
        if (opts.getInput()==null || opts.getInput().size()==0) {
            final Reader reader = new InputStreamReader(System.in);
            final Writer writer = new OutputStreamWriter(System.out);
            try {
                computeFingerprintsInTable(reader, writer, opts);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CDKException e) {
                e.printStackTrace();
            }
        } else {
            for (File csv : opts.getInput()) {
                try {
                    final FileReader reader = new FileReader(csv);
                    try {
                        Writer writer = null;
                        try {
                            if (isStdout) {
                                writer = new OutputStreamWriter(System.out);
                            } else {
                                writer = new FileWriter(changeName(csv));
                            }
                            try {
                                computeFingerprintsInTable(reader, writer, opts);
                            } catch (CDKException e) {
                                e.printStackTrace();
                            }
                        } finally {
                            if (!isStdout && writer!=null)
                                writer.close();
                        }
                    } catch (IOException e) {
                        System.err.println("Error while reading/writing:\n" + e.getMessage());
                    } finally {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (FileNotFoundException e) {
                    System.err.println("Cannot find file '" + csv + "'");
                }
            }
        }
    }

    private static List<IFingerprinter> getFingerprintList(ComputeOpts opts) {
        final List<IFingerprinter> list = new ArrayList<IFingerprinter>();
        if (opts.isAll()) return Fingerprinter.createListOfAllFingerprints();
        if (opts.isExtended()) return Fingerprinter.createExtendedListOfFingerprints();
        if (opts.isOpenbabel()) {
            list.add(new OpenBabelFingerprinter());
            list.add(new SubstructureFingerprinter());
        }
        if (opts.isMaccs()) list.add(new MACCSFingerprinter());
        if (opts.isPubchem()) list.add(new PubchemFingerprinter(DefaultChemObjectBuilder.getInstance()));
        if (opts.isKlekotha()) list.add(new KlekotaRothFingerprinter());
        if (opts.isPath()) list.add(new MarcusPathFingerprinter());
        if (opts.isNeighbourhood()) list.add(new NeighbourhoodFingerprinter());
        if (opts.isSpherical()) list.add(new SphericalFingerprint());
        if (list.size() > 0) {
            return list;
        } else {
            return Fingerprinter.createListOfFingerprints();
        }
    }

    private static void computeFingerprintsInTable(Reader reader, Writer writer, ComputeOpts opts) throws IOException, CDKException {
        final Fingerprinter fingerprinter = new Fingerprinter(getFingerprintList(opts));
        final TableUtils tab = new TableUtils(reader, writer);
        if (!tab.nextRow()) return;
        if (tab.findInchiColumn() < 0) throw new IOException("Cannot find InChI column in table");
        do {
            String inchi = tab.getInchi();
            if (!opts.is3D()) {
                inchi = fingerprinter.convert3Dto2DInchi(inchi);
                tab.replaceInchi(inchi);
            }
            try {
                final BitSet[] bitsets = fingerprinter.computeFingerprints(fingerprinter.convertInchi2Mol(inchi));
                tab.addColumn(fingerprinter.fingerprintsToString(bitsets));
            } catch (Throwable e) {
                System.err.println("Error while computing InChI: '" + inchi + "'");
                e.printStackTrace();
            }
            tab.writeColumn();
        } while (tab.nextRow());
        tab.flush();
    }

    public Fingerprinter getFingerprinter(ComputeOpts opts) throws CDKException {
        if (opts.isCfm() || opts.isKlekotha() || opts.isMaccs() || opts.isOpenbabel() || opts.isPubchem()) {
            ArrayList<IFingerprinter> fingerprinters = new ArrayList<IFingerprinter>();
            if (opts.isOpenbabel()) fingerprinters.add(new OpenBabelFingerprinter());
            if (opts.isMaccs()) fingerprinters.add(new MACCSFingerprinter());
            if (opts.isPubchem()) fingerprinters.add(new PubchemFingerprinter(DefaultChemObjectBuilder.getInstance()));
            if (opts.isKlekotha()) fingerprinters.add(new KlekotaRothFingerprinter());
            if (opts.isPath()) fingerprinters.add(new MarcusPathFingerprinter());
            if (opts.isCfm()) fingerprinters.add(new CFMFingerprinter());
            if (opts.isNeighbourhood()) fingerprinters.add(new NeighbourhoodFingerprinter());
            return new Fingerprinter(fingerprinters);

        } else return new Fingerprinter();
    }

    private static File changeName(File csv) {
        final String n = csv.getName();
        final int i=n.lastIndexOf('.');
        return new File(csv.getParent(), n.substring(0, i) + ".fpt.csv");
    }

}
