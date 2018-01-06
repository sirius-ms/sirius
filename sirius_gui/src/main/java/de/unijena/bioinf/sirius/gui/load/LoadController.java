package de.unijena.bioinf.sirius.gui.load;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.CloseableIterator;
import de.unijena.bioinf.babelms.MsExperimentParser;
import de.unijena.bioinf.fingerid.storage.ConfigStorage;
import de.unijena.bioinf.myxo.io.spectrum.CSVFormatReader;
import de.unijena.bioinf.sirius.gui.dialogs.ErrorListDialog;
import de.unijena.bioinf.sirius.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.sirius.gui.filefilter.SupportedDataFormatsFilter;
import de.unijena.bioinf.sirius.gui.io.DataFormat;
import de.unijena.bioinf.sirius.gui.io.DataFormatIdentifier;
import de.unijena.bioinf.sirius.gui.structure.CSVToSpectrumConverter;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;
import de.unijena.bioinf.sirius.gui.structure.SpectrumContainer;
import gnu.trove.list.array.TDoubleArrayList;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

public class LoadController implements LoadDialogListener {
    LoadDialog loadDialog;

    private ExperimentContainer expToModify;
    private URL source;
    private ConfigStorage config;
    private JFrame owner;

    public LoadController(JFrame owner, ExperimentContainer exp, ConfigStorage config) {
        this.owner = owner;

        this.config = config;
        expToModify = exp;
        loadDialog = new DefaultLoadDialog(owner);

        if (expToModify != null) {
            loadDialog.ionizationChanged(exp.getIonization() != null ? exp.getIonization() : PrecursorIonType.unknown(1));

            if (!Double.isNaN(exp.getIonMass()) && exp.getIonMass() > 0) {
                loadDialog.parentMassChanged(exp.getIonMass());
            }

            loadDialog.experimentNameChanged(exp.getName());

            for (Spectrum<? extends Peak> spectrum : expToModify.getMs1Spectra()) {
                addToSpectra(spectrum);
            }

            for (Spectrum<? extends Peak> spectrum : expToModify.getMs2Spectra()) {
                addToSpectra(spectrum);
            }
        } else {
            loadDialog.getSpectra().removeAllElements();
            loadDialog.parentMassChanged(-1);
            loadDialog.ionizationChanged(PrecursorIonType.unknown(1));
            loadDialog.experimentNameChanged("");
            source = null;
        }

        loadDialog.addLoadDialogListener(this);
    }

    public LoadController(JFrame owner, ConfigStorage config) {
        this(owner, null, config);
    }

    public void showDialog() {
        loadDialog.showDialog();
    }

    //todo maybe chache ms1 instead
    SpectrumContainer getMs1OrNull() {
        Enumeration<SpectrumContainer> el = loadDialog.getSpectra().elements();
        while (el.hasMoreElements()) {
            SpectrumContainer n = el.nextElement();
            if (n.getSpectrum().getMsLevel() == 1)
                return n;
        }
        return null;
    }

    private SpectrumContainer addToSpectra(Spectrum<?> sp) {
        if (sp.getMsLevel() == 1) {
            SpectrumContainer ms1 = getMs1OrNull();
            if (ms1 != null) {
                sp = new MutableMs2Spectrum(sp);
                ((MutableMs2Spectrum) sp).setMsLevel(2);
            }
        }

        SpectrumContainer container = new SpectrumContainer(sp);
        loadDialog.getSpectra().addElement(container);
        return container;
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
            importSpectra(files);
        }
    }

    private void importSpectra(File[] files) {

        DataFormatIdentifier dfi = new DataFormatIdentifier();
        List<File> csvFiles = new ArrayList<>();
        List<File> msFiles = new ArrayList<>();
        List<File> mgfFiles = new ArrayList<>();
        for (File file : files) {
            DataFormat df = dfi.identifyFormat(file);
            if (df == DataFormat.CSV) {
                csvFiles.add(file);
            } else if (df == DataFormat.JenaMS) {
                msFiles.add(file);
            } else if (df == DataFormat.MGF) {
                mgfFiles.add(file);
            }
        }

        importSpectra(csvFiles, msFiles, mgfFiles);
    }


    private void importSpectra(List<File> csvFiles, List<File> msFiles, List<File> mgfFiles) {

        List<String> errorStorage = new ArrayList<>();

        CSVDialogReturnContainer cont = null;
        CSVFormatReader csvReader = new CSVFormatReader();


        //csv import
        if (csvFiles.size() > 0) {

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


        final MsExperimentParser parser = new MsExperimentParser();

        //import ms files
        if (msFiles.size() > 0) {
            for (File file : msFiles) {
                try (CloseableIterator<Ms2Experiment> iter = parser.getParser(file).parseFromFileIterator(file)) {
                    while (iter.hasNext()) {
                        importExperiment(iter.next(), errorStorage);
                    }
                } catch (Exception e) {
                    errorStorage.add(file.getName() + ": Invalid file format.");
                    continue;
                }
            }
        }

        //import mgf files
        if (mgfFiles.size() > 0) {
            for (File file : mgfFiles) {
                try (CloseableIterator<Ms2Experiment> iter = parser.getParser(file).parseFromFileIterator(file)) {
                    while (iter.hasNext()) {
                        importExperiment(iter.next(), errorStorage);
                    }
                } catch (Exception e) {
                    String m = file.getName() + ": Invalid file format.";
                    LoggerFactory.getLogger(this.getClass()).error(m, e);
                    errorStorage.add(m);
                    continue;
                }
            }
        }

        if (errorStorage.size() > 1) {
            ErrorListDialog elDiag = new ErrorListDialog(this.owner, errorStorage);
        } else if (errorStorage.size() == 1) {
            ExceptionDialog eDiag = new ExceptionDialog(this.owner, errorStorage.get(0));
        }

    }

    //this imports an merges the experiments
    private void importExperiment(Ms2Experiment experiment, List<String> errorStorage) {
        source = experiment.getSource();

        if (loadDialog.getIonization().isIonizationUnknown() && experiment.getPrecursorIonType() != null && !experiment.getPrecursorIonType().isIonizationUnknown())
            loadDialog.ionizationChanged(experiment.getPrecursorIonType());

        final String name = loadDialog.getExperimentName();
        if (name == null || name.isEmpty())
            loadDialog.experimentNameChanged(experiment.getName());

        if (loadDialog.getParentMass() < 0 && experiment.getIonMass() > 0)
            loadDialog.parentMassChanged(experiment.getIonMass());

        if (experiment.getMs1Spectra().size() > 0) {
            Spectrum<Peak> ms1 = experiment.getMs1Spectra().get(0);
            if (ms1 != null) {
                addToSpectra(ms1);
            }
        }

        for (Ms2Spectrum<Peak> sp : experiment.getMs2Spectra()) {
            addToSpectra(sp);
        }
    }

    public ExperimentContainer getExperiment() {
        return expToModify;
    }


    @Override
    public void removeSpectrum(SpectrumContainer sp) {
        loadDialog.getSpectra().removeElement(sp);

        if (loadDialog.getSpectra().isEmpty()) {
            loadDialog.parentMassChanged(-1);
            loadDialog.ionizationChanged(PrecursorIonType.unknown(1));
            loadDialog.experimentNameChanged("");
        }
    }

    @Override
    public void completeProcess() {
        if (!loadDialog.getSpectra().isEmpty()) {
            if (expToModify == null) {
                expToModify = new ExperimentContainer(new MutableMs2Experiment());
            } else {
                expToModify.getMs2Experiment().getMs1Spectra().clear();
                expToModify.getMs2Experiment().getMs2Spectra().clear();
            }

            //add spectra
            Enumeration<SpectrumContainer> spectra = loadDialog.getSpectra().elements();
            while (spectra.hasMoreElements()) {
                final SpectrumContainer container = spectra.nextElement();

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
            expToModify.getMs2Experiment().setSource(source);
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

        if (msLevel == 1) {
            SpectrumContainer oldMS1 = getMs1OrNull();
            if (oldMS1 != null) {
                oldMS1.getModifiableSpectrum().setMsLevel(2);
                loadDialog.msLevelChanged(oldMS1);
            }
        }

        loadDialog.msLevelChanged(container);
    }

    @Override
    public void addSpectra(List<File> files) {
        File[] fileArr = new File[files.size()];
        importSpectra(files.toArray(fileArr));

    }

    public void addSpectra(List<File> csvFiles, List<File> msFiles, List<File> mgfFiles) {
        importSpectra(csvFiles, msFiles, mgfFiles);
    }

}
