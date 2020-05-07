package de.unijena.bioinf.fingerid.fingerprints;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.*;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.isomorphism.matchers.smarts.SmartsMatchers;
import org.openscience.cdk.smiles.smarts.SMARTSQueryTool;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Map;

public class NeighbourhoodFingerprinter extends AbstractFingerprinter implements IFingerprinter {

    private static SMARTSQueryTool[] QUERIES1, QUERIES2;
    private SMARTSQueryTool[] queries;

    private static SMARTSQueryTool[] loadQueries(boolean subset) {
        synchronized (NeighbourhoodFingerprinter.class) {
            if (subset && QUERIES1!=null) return QUERIES1;
            if (!subset && QUERIES2!=null) return QUERIES2;
            final BufferedReader reader = new BufferedReader(new InputStreamReader(NeighbourhoodFingerprinter.class.getResourceAsStream(subset ? "/nsmarts.txt" : "/neighbourhood.txt")));
            String line=null;
            final ArrayList<SMARTSQueryTool> tools = new ArrayList<>();
            final IChemObjectBuilder def = DefaultChemObjectBuilder.getInstance();
            try {
                while ((line=reader.readLine())!=null) {
                    tools.add(new SMARTSQueryTool(line, def));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (subset) {
                QUERIES1 = tools.toArray(new SMARTSQueryTool[tools.size()]);
                return QUERIES1;
            } else {
                QUERIES2 = tools.toArray(new SMARTSQueryTool[tools.size()]);
                return QUERIES2;
            }
        }
    }

    public NeighbourhoodFingerprinter(boolean subset) {
        this.queries = loadQueries(subset);
    }

    public NeighbourhoodFingerprinter() {
        this(false);
    }

    @Override
    public IBitFingerprint getBitFingerprint(IAtomContainer atomContainer) throws CDKException {
        int bitsetLength = queries.length;
        BitSet fingerPrint = new BitSet(bitsetLength);
        {
            SmartsMatchers.prepare(atomContainer, true);
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(atomContainer);
            Aromaticity aroma = new Aromaticity(ElectronDonation.cdk(), Cycles.cdkAromaticSet());
            aroma.apply(atomContainer);
        }
        for (int i = 0; i < queries.length; i++) {
            boolean status = queries[i].matches(atomContainer);
            if (status) fingerPrint.set(i, true);
        }
        return new BitSetFingerprint(fingerPrint);
    }

    @Override
    public ICountFingerprint getCountFingerprint(IAtomContainer container) throws CDKException {
        return null;
    }

    @Override
    public Map<String, Integer> getRawFingerprint(IAtomContainer container) throws CDKException {
        return null;
    }

    @Override
    public int getSize() {
        return queries.length;
    }
}
