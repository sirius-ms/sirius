package de.unijena.bioinf.sirius.gui.structure;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.sirius.gui.load.CSVDialogReturnContainer;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.List;

public class CSVToSpectrumConverter {

    public Spectrum<?> convertCSVToSpectrum(List<TDoubleArrayList> data, CSVDialogReturnContainer cont) {
        return convertCSVToSpectrum(data, cont.getMassIndex(), cont.getIntIndex(), cont.getMinEnergy(), cont.getMaxEnergy(), cont.getMsLevel());
    }

    public Spectrum<?> convertCSVToSpectrum(List<TDoubleArrayList> data, int massIndex, int absIntIndex, double minEnergy, double maxEnergy, int msLevel) {
        int rowNumber = data.size();
        double[] masses = new double[rowNumber];
        double[] ints = new double[rowNumber];

        for (int i = 0; i < rowNumber; i++) {
            masses[i] = data.get(i).get(massIndex);
            ints[i] = data.get(i).get(absIntIndex);
        }
        
        Spectrum<?> sp = new SimpleSpectrum(masses, ints);

        if (msLevel > 1) {
            sp = new MutableMs2Spectrum(sp, 0d, (minEnergy > 0 && maxEnergy > 0 && minEnergy <= maxEnergy) ? new CollisionEnergy(minEnergy, maxEnergy) : CollisionEnergy.none(), msLevel);
        }

        return sp;
    }

}
