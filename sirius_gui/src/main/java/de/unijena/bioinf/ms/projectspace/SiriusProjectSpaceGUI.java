package de.unijena.bioinf.ms.projectspace;

import ca.odell.glazedlists.BasicEventList;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.sirius.core.SiriusProperties;
import de.unijena.bioinf.sirius.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.sirius.gui.mainframe.Workspace;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class SiriusProjectSpaceGUI {
    public final BasicEventList<ExperimentContainer> COMPOUNT_LIST = new BasicEventList<>();
    private final HashSet<String> NAMES = new HashSet<>();
    private SiriusProjectSpace projectSpace;

    public SiriusProjectSpaceGUI(@Nullable FilenameFormatter filenameFormatter, @NotNull final File projectSpaceRoot, MetaDataSerializer... metaDataSerializers) {

    }


    public void importCompounds(List<MutableMs2Experiment> exs) {
        if (exs != null) {
            for (MutableMs2Experiment ex : exs) {
                if (ex == null) {
                    continue;
                } else {
                    importCompound(ex);
                }
            }
        }
    }

    public void importCompound(final MutableMs2Experiment ex) {
        SwingUtilities.invokeLater(() -> {
            //search for a missing molecular formula before cleaning the annotations
            if (ex.getMolecularFormula() == null) {
                String f = extractMolecularFormulaString(ex);
                if (f != null && !f.isEmpty())
                    ex.setMolecularFormula(MolecularFormula.parse(f));
            }

            if (ex.getPrecursorIonType() == null) {
                LoggerFactory.getLogger(Workspace.class).warn("Input experiment with name '" + ex.getName() + "' does not have a charge nor an ion type annotation.");
                ex.setPrecursorIonType(PeriodicTable.getInstance().getUnknownPrecursorIonType(1));
            }
            clearExperimentAnotations(ex);
            addIonToPeriodicTable(ex.getPrecursorIonType());

            //adding experiment to gui
            final ExperimentContainer ec = new ExperimentContainer(ex);
            resolveCompoundNameConflict(ec);
            COMPOUNT_LIST.add(ec);

            //write experiment to workspace
            Jobs.runInBackround(new TinyBackgroundJJob<ExperimentDirectory>() {
                @Override
                protected ExperimentDirectory compute() throws Exception {
                    projectSpace.writeExperiment(ec.getExperimentResult());
                    return ec.getIdentifier();
                }
            });

            //todo handle compute state in ExperimentContainer
//            if (ec.getResults().size() > 0) ec.setSiriusComputeState(ComputingStatus.COMPUTED);
        });
    }

    public void resolveCompoundNameConflict(ExperimentContainer ec) {
        while (true) {
            if (ec.getGUIName() != null && !ec.getGUIName().isEmpty()) {
                if (NAMES.contains(ec.getGUIName())) {
                    ec.setNameCounter(ec.getNameCounter() + 1);
                } else {
                    NAMES.add(ec.getGUIName());
                    break;
                }
            } else {
                ec.setName("Unknown");
            }
        }
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
    /*public static List<ExperimentContainer> toExperimentContainer(Ms2Experiment... exp) {
        return toExperimentContainer(Arrays.asList(exp));
    }

    public static List<ExperimentContainer> toExperimentContainer(List<Ms2Experiment> exp) {
        ArrayList<ExperimentContainer> ecs = new ArrayList<>(exp.size());
        for (Ms2Experiment ms2Experiment : exp) {
            ecs.add(new ExperimentContainer(ms2Experiment));
        }
        return ecs;
    }*/

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
