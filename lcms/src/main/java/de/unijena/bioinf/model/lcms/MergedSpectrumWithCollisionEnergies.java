package de.unijena.bioinf.model.lcms;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MergedSpectrumWithCollisionEnergies {

    List<MergedSpectrum> spectra;

    public MergedSpectrumWithCollisionEnergies(MergedSpectrum... spectra) {
        this.spectra = new ArrayList<>(Arrays.asList(spectra));
    }

    public MergedSpectrum spectrumAt(int k) {
        return spectra.get(k);
    }
    public CollisionEnergy energyAt(int k) {
        return spectra.get(k).getCollisionEnergy();
    }
    public int numberOfEnergies() {
        return spectra.size();
    }

    public List<CollisionEnergy> getCollisionEnergies() {
        return spectra.stream().map(MergedSpectrum::getCollisionEnergy).collect(Collectors.toList());
    }

    public List<Scan> getAllScans() {
        ArrayList<Scan> scans = new ArrayList<>();
        for (MergedSpectrum spec : spectra) {
            scans.addAll(spec.getScans());
        }
        return scans;
    }

    public List<MergedSpectrum> getSpectra() {
        return spectra;
    }

    public Optional<MergedSpectrum> spectrumFor(CollisionEnergy energy) {
        for (MergedSpectrum spec : spectra) {
            if (spec.getCollisionEnergy().equals(energy)) return Optional.of(spec);
        }
        return Optional.empty();
    }

    public Precursor getPrecursor() {
        return spectra.get(0).precursor;
    }

    public double totalTic() {
        double x = 0d;
        for(MergedSpectrum spec : spectra) x += spec.totalTic();
        return x;
    }
}
