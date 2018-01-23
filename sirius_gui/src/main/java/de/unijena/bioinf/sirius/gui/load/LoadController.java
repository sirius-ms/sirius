package de.unijena.bioinf.sirius.gui.load;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.fingerid.storage.ConfigStorage;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.myxo.io.spectrum.CSVFormatReader;
import de.unijena.bioinf.sirius.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.sirius.gui.dialogs.ErrorListDialog;
import de.unijena.bioinf.sirius.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.sirius.gui.filefilter.SupportedDataFormatsFilter;
import de.unijena.bioinf.sirius.gui.mainframe.BatchImportDialog;
import de.unijena.bioinf.sirius.gui.mainframe.FileImportDialog;
import de.unijena.bioinf.sirius.gui.mainframe.Workspace;
import de.unijena.bioinf.sirius.gui.structure.CSVToSpectrumConverter;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;
import de.unijena.bioinf.sirius.gui.structure.SpectrumContainer;
import gnu.trove.list.array.TDoubleArrayList;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class LoadController implements LoadDialogListener {
    private final ConfigStorage config;
    private final JFrame owner;
    private DefaultLoadDialog loadDialog;

    private final ExperimentContainer expToModify;

    private final EventList<SpectrumContainer> spectra;


    public LoadController(JFrame owner, ExperimentContainer exp, ConfigStorage config) {
        this.owner = owner;
        this.config = config;


        if (exp != null) {
            expToModify = exp;
            spectra = GlazedListsSwing.swingThreadProxyList(new BasicEventList<>(expToModify.getMs1Spectra().size() + expToModify.getMs2Spectra().size()));
            loadDialog = new DefaultLoadDialog(owner, spectra);


            loadDialog.ionizationChanged(exp.getIonization() != null ? exp.getIonization() : PrecursorIonType.unknown(1));

            loadDialog.editPanel.setMolecularFomula(exp.getMs2Experiment());

            loadDialog.experimentNameChanged(exp.getName());

            for (Spectrum<? extends Peak> spectrum : expToModify.getMs1Spectra()) {
                addToSpectra(spectrum);
            }

            for (Spectrum<? extends Peak> spectrum : expToModify.getMs2Spectra()) {
                addToSpectra(spectrum);
            }

            loadDialog.setParentMass(expToModify.getIonMass());
        } else {
            expToModify = new ExperimentContainer(new MutableMs2Experiment());
            spectra = GlazedListsSwing.swingThreadProxyList(new BasicEventList<>());
            loadDialog = new DefaultLoadDialog(owner, spectra);
            loadDialog.ionizationChanged(PrecursorIonType.unknown(1));
            loadDialog.experimentNameChanged("");
            loadDialog.editPanel.formulaTF.setText("");
        }

        loadDialog.addLoadDialogListener(this);
    }

    public LoadController(JFrame owner, ConfigStorage config) {
        this(owner, null, config);
    }

    public void showDialog() {
        loadDialog.showDialog();
    }

    private void addToSpectra(Spectrum<?>... sps) {
        spectra.addAll(Arrays.stream(sps).map(SpectrumContainer::new).collect(Collectors.toList()));
    }

    @Override
    public void addSpectra() {
        JFileChooser chooser = new JFileChooser(config.getDefaultLoadDialogPath());
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        chooser.addChoosableFileFilter(new SupportedDataFormatsFilter());
        chooser.setAcceptAllFileFilterUsed(false);
        int returnVal = chooser.showOpenDialog((JDialog) loadDialog);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File[] files = chooser.getSelectedFiles();
            //setzt Pfad
            config.setDefaultLoadDialogPath(files[0].getParentFile());

            //untersuche die Dateitypen und schaue ob CSV vorhanden, wenn vorhanden behandelte alle CSVs auf
            //gleiche Weise
            importSpectra(Arrays.asList(files));
        }
    }

    private void importSpectra(List<File> files) {
        FileImportDialog idi = new FileImportDialog(owner, files);
        importSpectra(idi.getCSVFiles(), idi.getMSFiles(), idi.getMGFFiles());
    }


    private void importSpectra(List<File> csvFiles, List<File> msFiles, List<File> mgfFiles) {
        List<String> errorStorage = new ArrayList<>();

        //csv import
        if (csvFiles.size() > 0) {
            CSVDialogReturnContainer cont = null;
            CSVFormatReader csvReader = new CSVFormatReader();

            HashMap<Integer, List<List<TDoubleArrayList>>> columnNumberToData = new HashMap<>();

            for (File file : csvFiles) {
                try {
                    List<TDoubleArrayList> data = csvReader.readCSV(file);
                    Integer key = data.get(0).size();
                    if (columnNumberToData.containsKey(key)) {
                        columnNumberToData.get(key).add(data);
                    } else {
                        List<List<TDoubleArrayList>> list = new ArrayList<>();
                        list.add(data);
                        columnNumberToData.put(key, list);
                    }
                } catch (Exception e) {
                    errorStorage.add(file.getName() + ": Invalid file format.");
                }
            }

            if (columnNumberToData.size() > 0) {
                for (Integer key : columnNumberToData.keySet()) {
                    List<List<TDoubleArrayList>> list = columnNumberToData.get(key);
                    if (list.size() == 1) {
                        CSVDialog diag = new CSVDialog((JDialog) loadDialog, list.get(0), false);
                        if (diag.getReturnValue() == ReturnValue.Success) {
                            cont = diag.getResults();
                            CSVToSpectrumConverter conv = new CSVToSpectrumConverter();
                            Spectrum<?> sp = conv.convertCSVToSpectrum(list.get(0), cont);
                            addToSpectra(sp);
                        } else {
                            return; //breche ab
                        }
                    } else {
                        CSVDialog diag = new CSVDialog((JDialog) loadDialog, list.get(0), true);
                        if (diag.getReturnValue() == ReturnValue.Success) {
                            cont = diag.getResults();
                            cont.setMaxEnergy(-1);
                            cont.setMinEnergy(-1);
                            cont.setMsLevel(2);

                            for (List<TDoubleArrayList> data : list) {
                                CSVToSpectrumConverter conv = new CSVToSpectrumConverter();
                                Spectrum<?> sp = conv.convertCSVToSpectrum(data, cont);
                                addToSpectra(sp);
                            }
                        }
                    }
                }
            }
        }

        BatchImportDialog batchImportDialog = new BatchImportDialog(loadDialog);
        batchImportDialog.start(msFiles, mgfFiles);
        errorStorage.addAll(batchImportDialog.getErrors());

        Jobs.runInBackroundAndLoad(loadDialog, "Importing Compounds", new TinyBackgroundJJob() {
            @Override
            protected Object compute() {
                List<Ms2Experiment> r = batchImportDialog.getResults();
                int i = 0;
                updateProgress(0, r.size(), i);
                for (Ms2Experiment exp : r) {
                    importExperiment(exp);
                    updateProgress(0, r.size(), ++i);
                }
                return true;
            }
        });


        if (errorStorage.size() > 1) {
            new ErrorListDialog(this.owner, errorStorage);
        } else if (errorStorage.size() == 1) {
            new ExceptionDialog(this.owner, errorStorage.get(0));
        }

    }

    //this imports an merges the experiments
    private void importExperiment(Ms2Experiment experiment) {
        expToModify.getMs2Experiment().addAnnotationsFrom(experiment);

        if (expToModify.getMs2Experiment().getSource() == null)
            expToModify.getMs2Experiment().setSource(experiment.getSource());

        if (loadDialog.getIonization().isIonizationUnknown() && experiment.getPrecursorIonType() != null && !experiment.getPrecursorIonType().isIonizationUnknown())
            addIonToPeriodicTableAndFireChange(experiment.getPrecursorIonType());

        final String formula = loadDialog.editPanel.formulaTF.getText();
        if (formula == null || formula.isEmpty())
            loadDialog.editPanel.setMolecularFomula(experiment);

        final String name = loadDialog.getExperimentName();
        if (name == null || name.isEmpty())
            loadDialog.experimentNameChanged(experiment.getName());

        for (Spectrum<Peak> sp : experiment.getMs1Spectra()) {
            addToSpectra(sp);
        }

        for (Ms2Spectrum<Peak> sp : experiment.getMs2Spectra()) {
            addToSpectra(sp);
        }

        if (expToModify.getIonMass() <= 0)
            loadDialog.setParentMass(experiment.getIonMass());
    }

    public ExperimentContainer getExperiment() {
        if (expToModify.getMs1Spectra().isEmpty() && expToModify.getMs2Spectra().isEmpty())
            return null;
        return expToModify;
    }

    private void addIonToPeriodicTableAndFireChange(PrecursorIonType ionization) {
        if (Workspace.addIonToPeriodicTable(ionization))
            loadDialog.editPanel.ionizationCB.refresh();
        loadDialog.ionizationChanged(ionization);
    }

    @Override
    public void removeSpectra(List<SpectrumContainer> sps) {
        spectra.removeAll(sps);
        if (spectra.isEmpty()) {
            loadDialog.ionizationChanged(PrecursorIonType.unknown(1));
            loadDialog.experimentNameChanged("");
            loadDialog.editPanel.formulaTF.setText("");
        }
    }


    @Override
    public void completeProcess() {
        if (!spectra.isEmpty()) {
            expToModify.getMs2Experiment().getMs1Spectra().clear();
            expToModify.getMs2Experiment().getMs2Spectra().clear();

            //add spectra
            for (SpectrumContainer container : spectra) {
                Spectrum<?> spectrum = container.getSpectrum(); // this return already the modified version if one exists
                if (spectrum.getMsLevel() == 1) {
                    if (container.isModified())
                        expToModify.getMs2Experiment().getMs1Spectra().add(new SimpleSpectrum(spectrum));
                    else
                        expToModify.getMs2Experiment().getMs1Spectra().add((SimpleSpectrum) spectrum);
                } else {
                    expToModify.getMs2Experiment().getMs2Spectra().add((MutableMs2Spectrum) spectrum);
                }
            }

            expToModify.setIonization(loadDialog.getIonization());
            expToModify.setIonMass(loadDialog.getParentMass());
            expToModify.setName(loadDialog.getExperimentName());

            if (loadDialog.editPanel.validateFormula()) {
                expToModify.getMs2Experiment().setMolecularFormula(loadDialog.editPanel.getMolecularFormula());
            }
        }
    }

    @Override
    public void changeCollisionEnergy(SpectrumContainer container) {
        Spectrum sp = container.getSpectrum();
        double oldMin, oldMax;
        if (sp.getCollisionEnergy() == null) {
            oldMin = 0;
            oldMax = 0;
        } else {
            oldMin = sp.getCollisionEnergy().getMinEnergy();
            oldMax = sp.getCollisionEnergy().getMaxEnergy();
        }

        CollisionEnergyDialog ced = new CollisionEnergyDialog((JDialog) loadDialog, oldMin, oldMax);
        if (ced.getReturnValue() == ReturnValue.Success) {
            double newMin = ced.getMinCollisionEnergy();
            double newMax = ced.getMaxCollisionEnergy();
            if (oldMin != newMin || oldMax != newMax) {
                container.getModifiableSpectrum().setCollisionEnergy(new CollisionEnergy(newMin, newMax));
                loadDialog.newCollisionEnergy(container);
            }
        }
    }

    @Override
    public void changeMSLevel(final SpectrumContainer container, int msLevel) {
        //indentity chekc already done before listener call
        MutableMs2Spectrum mod = container.getModifiableSpectrum();
        mod.setMsLevel(msLevel);
        loadDialog.msLevelChanged(container);
    }

    @Override
    public void addSpectra(List<File> files) {
        importSpectra(files);
    }

    public void addSpectra(List<File> csvFiles, List<File> msFiles, List<File> mgfFiles) {
        importSpectra(csvFiles, msFiles, mgfFiles);
    }

}
