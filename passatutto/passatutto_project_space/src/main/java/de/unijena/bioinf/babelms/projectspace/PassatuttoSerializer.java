package de.unijena.bioinf.babelms.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.dot.FTDotWriter;
import de.unijena.bioinf.babelms.ms.AnnotatedSpectrumWriter;
import de.unijena.bioinf.passatutto.Decoy;
import de.unijena.bioinf.sirius.ExperimentResult;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Set;

public class PassatuttoSerializer implements MetaDataSerializer {
    @Override
    public void read(@NotNull ExperimentResult input, @NotNull DirectoryReader reader, @NotNull Set<String> names) throws IOException {
        ;
    }

    @Override
    public void write(@NotNull ExperimentResult input, @NotNull DirectoryWriter writer) throws IOException {
        writer.env.enterDirectory("decoys");
        try {
            input.getResults().stream().filter(r->r.getAnnotation(Decoy.class,()->null)!=null).forEach(result->{
                final Decoy decoy = result.getAnnotationOrThrow(Decoy.class);
                try (final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(writer.env.openFile(SiriusLocations.makeFileName(result) + ".tsv")))){
                    if (decoy.getDecoyTree()!=null) {
                        new AnnotatedSpectrumWriter(AnnotatedSpectrumWriter.Fields.MZ, AnnotatedSpectrumWriter.Fields.REL_INTENSITY, AnnotatedSpectrumWriter.Fields.FORMULA, AnnotatedSpectrumWriter.Fields.ION).write(bw, decoy.getDecoyTree());
                    } else {
                        final SimpleSpectrum spec = decoy.getDecoySpectrum();
                        bw.write(AnnotatedSpectrumWriter.Fields.MZ.name);
                        bw.write('\t');
                        bw.write(AnnotatedSpectrumWriter.Fields.REL_INTENSITY.name);
                        bw.newLine();
                        for (int k=0; k < spec.size(); ++k) {
                            bw.write(String.valueOf(spec.getMzAt(k)));
                            bw.write('\t');
                            bw.write(String.valueOf(spec.getIntensityAt(k)));
                            bw.newLine();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (decoy.getDecoyTree()!=null) {
                    try (final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(writer.env.openFile(SiriusLocations.makeFileName(result) + ".dot")))){
                        new FTDotWriter(true,true).writeTree(bw, decoy.getDecoyTree());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            });
        } finally {
            writer.env.leaveDirectory();
        }
    }
}
