package de.unijena.bioinf.ms.projectspace;

import ca.odell.glazedlists.BasicEventList;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.sirius.ExperimentResult;
import de.unijena.bioinf.sirius.core.SiriusProperties;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class GuiProjectSpace {
    public static final GuiProjectSpace PS;

    static {
        //todo load workspace form Property
        PropertyManager.getProperty("path/to/space");
        PS = new GuiProjectSpace(null);
    }

    public final BasicEventList<ExperimentContainer> COMPOUNT_LIST = new BasicEventList<>();
    private final Map<String, TIntSet> NAMES = new ConcurrentHashMap<>();
    private final SiriusProjectSpace projectSpace;

    public GuiProjectSpace(SiriusProjectSpace space) {
        projectSpace = space;
    }

    public void importCompound(@NotNull final ExperimentContainer ec) {
        SwingUtilities.invokeLater(() -> {
            cleanExperiment(ec.getMs2Experiment());

            //adding experiment to gui
            addToCompoundList(ec);

            //write experiment to project-space
            writeToProjectSpace(ec.getExperimentResult());

        });
    }

    public void importCompound(@NotNull final MutableMs2Experiment ex) {
        importCompound(new ExperimentContainer(ex));
    }

    private void addToCompoundList(@NotNull final ExperimentContainer ec) {
        addName(ec);
        COMPOUNT_LIST.add(ec);
    }

    private void writeToProjectSpace(@NotNull final ExperimentResult exResult) {
        SiriusJobs.getGlobalJobManager().submitJob(new BasicJJob<ExperimentDirectory>(JJob.JobType.IO) {
            @Override
            protected ExperimentDirectory compute() throws Exception {
                projectSpace.writeExperiment(exResult);
                return exResult.getAnnotation(ExperimentDirectory.class);
            }
        });
    }

    private void cleanExperiment(@NotNull final MutableMs2Experiment ex) {
        //search for a missing molecular formula before cleaning the annotations
        if (ex.getMolecularFormula() == null) {
            String f = extractMolecularFormulaString(ex);
            if (f != null && !f.isEmpty())
                ex.setMolecularFormula(MolecularFormula.parse(f));
        }

        if (ex.getPrecursorIonType() == null) {
            LoggerFactory.getLogger(getClass()).warn("Input experiment with name '" + ex.getName() + "' does not have a charge nor an ion type annotation.");
            ex.setPrecursorIonType(PeriodicTable.getInstance().getUnknownPrecursorIonType(1));
        }
        clearExperimentAnotations(ex);
        addIonToPeriodicTable(ex.getPrecursorIonType());
    }

    public void remove(ExperimentContainer... containers) {
        remove(Arrays.asList(containers));
    }

    public void remove(List<ExperimentContainer> containers) {
        for (ExperimentContainer ec : containers) {
            NAMES.remove(ec.getGUIName());
            try {
                projectSpace.deleteExperiment(ec.getIdentifier());
            } catch (IOException e) {
                LoggerFactory.getLogger(getClass()).warn("Could not delete Compound: " + ec.getGUIName(), e);
            }
        }

        COMPOUNT_LIST.removeAll(containers);
    }

    public void clear() {
        remove(new ArrayList<>(COMPOUNT_LIST));
    }

    // region static import helper methods
    public void changeName(ExperimentContainer ec, String old) {
        if (NAMES.containsKey(old)) {
            TIntSet indeces = NAMES.get(old);
            indeces.remove(ec.getNameIndex());

            if (indeces.isEmpty())
                NAMES.remove(old);
        }
        addName(ec);
    }

    public void addName(ExperimentContainer ec) {
        if (ec.getName() == null || ec.getName().isEmpty()) {
            ec.setName("Unknown");
        } else {
            final TIntSet indeces = NAMES.putIfAbsent(ec.getName(), new TIntHashSet());
            assert indeces != null;

            int counter = 1;
            while (indeces.contains(counter))
                counter++;
            indeces.add(counter);

            ec.setNameIndex(counter);
        }
    }

    //todo check why this is located here
    public static String extractMolecularFormulaString(Ms2Experiment exp) {
        MolecularFormula formula = exp.getMolecularFormula();
        if (formula != null) {
            return formula.toString();
        }

        if (exp.hasAnnotation(InChI.class)) {
            InChI inChI = exp.getAnnotation(InChI.class);
            formula = inChI.extractFormula();
            return formula.toString();

        }

        if (exp.hasAnnotation(Smiles.class)) {
            Smiles smiles = exp.getAnnotation(Smiles.class);
            try {
                final IAtomContainer mol = new SmilesParser(DefaultChemObjectBuilder.getInstance()).parseSmiles(smiles.smiles);
                String formulaString = MolecularFormulaManipulator.getString(MolecularFormulaManipulator.getMolecularFormula(mol));
                return formulaString;
            } catch (CDKException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static void clearExperimentAnotations(Ms2Experiment exp) {
        exp.clearAnnotation(Smiles.class);
        exp.clearAnnotation(InChI.class);
    }


    public static boolean addIonToPeriodicTable(PrecursorIonType ionization) {
        if (ionization != null) {
            String name = ionization.toString();
            if (name != null) {
                if (!PeriodicTable.getInstance().hasIon(name)) {
                    final PeriodicTable i = PeriodicTable.getInstance();
                    i.addCommonIonType(name);
                    if (ionization.getCharge() > 0)
                        SiriusProperties.SIRIUS_PROPERTIES_FILE().setProperty("de.unijena.bioinf.sirius.chem.adducts.positive",
                                String.join(",", i.getPositiveAdducts().stream().map(PrecursorIonType::toString).collect(Collectors.toList())));
                    else if (ionization.getCharge() < 0)
                        SiriusProperties.SIRIUS_PROPERTIES_FILE().setProperty("de.unijena.bioinf.sirius.chem.adducts.negative",
                                String.join(",", i.getNegativeAdducts().stream().map(PrecursorIonType::toString).collect(Collectors.toList())));
                    return true;
                }
            }
        }
        return false;
    }
    //endregion
}
