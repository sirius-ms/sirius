package de.unijena.bioinf.babelms.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.ms.AnnotatedSpectrumWriter;
import de.unijena.bioinf.passatutto.Decoy;
import de.unijena.bioinf.projectspace.ComponentSerializer;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.ProjectReader;
import de.unijena.bioinf.projectspace.ProjectWriter;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;

import java.io.IOException;
import java.util.Optional;

public class PassatuttoSerializer implements ComponentSerializer<FormulaResultId, FormulaResult,Decoy> {

    @Override
    public Decoy read(ProjectReader reader, FormulaResultId id, FormulaResult container) throws IOException {
        return null; // not supported yet
    }

    @Override
    public void write(ProjectWriter writer, FormulaResultId id, FormulaResult container, Optional<Decoy> optDecoy) throws IOException {
        final Decoy decoy = optDecoy.orElseThrow(() -> new RuntimeException("No decoy data found to write for ID: " + id));
        writer.textFile("decoys/" + id.fileName(".tsv"), (bw)-> {
            if (decoy.getDecoyTree() != null) {
                new AnnotatedSpectrumWriter(AnnotatedSpectrumWriter.Fields.MZ, AnnotatedSpectrumWriter.Fields.REL_INTENSITY, AnnotatedSpectrumWriter.Fields.FORMULA, AnnotatedSpectrumWriter.Fields.ION).write(bw, decoy.getDecoyTree());
            } else {
                final SimpleSpectrum spec = decoy.getDecoySpectrum();
                bw.write(AnnotatedSpectrumWriter.Fields.MZ.name);
                bw.write('\t');
                bw.write(AnnotatedSpectrumWriter.Fields.REL_INTENSITY.name);
                bw.newLine();
                for (int k = 0; k < spec.size(); ++k) {
                    bw.write(String.valueOf(spec.getMzAt(k)));
                    bw.write('\t');
                    bw.write(String.valueOf(spec.getIntensityAt(k)));
                    bw.newLine();
                }
            }
        });
    }

    @Override
    public void delete(ProjectWriter writer, FormulaResultId id) throws IOException {
        writer.delete("decoys/" + id.fileName(".tsv"));
    }
}
