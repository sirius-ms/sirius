package de.unijena.bioinf.projectspace.summaries;

import com.google.common.base.Joiner;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.ArrayFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.ClassyFireFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.ClassyfireProperty;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.ProjectWriter;
import de.unijena.bioinf.projectspace.Summarizer;
import de.unijena.bioinf.projectspace.sirius.CompoundContainer;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class CanopusSummaryWriter implements Summarizer, Iterable<String[]> {

    private List<ArrayFingerprint> classifications = new ArrayList<>();
    private List<MolecularFormula> molecularFormulas = new ArrayList<>();
    private List<PrecursorIonType> ionTypes = new ArrayList<>();
    private List<String> ids = new ArrayList<>();

    @Override
    public List<Class<? extends DataAnnotation>> requiredFormulaResultAnnotations() {
        return Arrays.asList(CanopusResult.class);
    }

    @Override
    public void addWriteCompoundSummary(ProjectWriter writer, @NotNull CompoundContainer exp, List<? extends SScored<FormulaResult, ? extends FormulaScore>> results) throws IOException {
        if (results.size()>0) {
            final SScored<FormulaResult, ? extends FormulaScore> hit = results.get(0);
            final FormulaResultId id = hit.getCandidate().getId();
            final Optional<CanopusResult> cr = hit.getCandidate().getAnnotation(CanopusResult.class);
            if (cr.isPresent()) {
                ionTypes.add(id.getIonType());
                molecularFormulas.add(id.getMolecularFormula());
                ids.add(id.getParentId().getDirectoryName());
                classifications.add(cr.get().getCanopusFingerprint().asDeterministic().asArray());

            }

        }
    }

    @Override
    public void writeProjectSpaceSummary(ProjectWriter writer) throws IOException {
        writer.table("canopus_summary.csv", HEADER, this);
    }

    private final static String[] HEADER = new String[]{ "name","molecularFormula", "adduct", "most specific class", "level 5", "subclass", "class",
            "superclass", "all classifications"};

    @NotNull
    @Override
    public Iterator<String[]> iterator() {
        return new Iterator<String[]>() {
            int k=0;
            String[] row = new String[HEADER.length];

            @Override
            public boolean hasNext() {
                return k < ids.size();
            }

            @Override
            public String[] next() {
                try {
                    final ArrayFingerprint canopusFingerprint = classifications.get(k);

                    final ClassyFireFingerprintVersion cf;
                    if (canopusFingerprint.getFingerprintVersion() instanceof MaskedFingerprintVersion) cf = (ClassyFireFingerprintVersion)(((MaskedFingerprintVersion) canopusFingerprint.getFingerprintVersion()).getMaskedFingerprintVersion());
                    else cf = (ClassyFireFingerprintVersion)canopusFingerprint.getFingerprintVersion();
                    final ClassyfireProperty primaryClass = cf.getPrimaryClass(canopusFingerprint);
                    final ClassyfireProperty[] lineage = primaryClass.getLineage();

                    row[0] = ids.get(k);
                    row[1] = molecularFormulas.get(k).toString();
                    row[2] = ionTypes.get(k).toString();
                    row[3] = primaryClass.getName();

                    row[7] = lineage.length>2 ? lineage[2].getName() : "";
                    row[6] = lineage.length>3 ? lineage[3].getName() : "";
                    row[5] = lineage.length>4 ? lineage[4].getName() : "";
                    row[4] = lineage.length>5 ? lineage[5].getName() : "";

                    row[8] = Joiner.on("; ").join(canopusFingerprint.presentFingerprints().asMolecularPropertyIterator());

                    ++k;
                    return row;

                } catch (ClassCastException e) {
                    LoggerFactory.getLogger(CanopusSummaryWriter.class).error("Cannot cast CANOPUS fingerprint to ClassyFireFingerprintVersion.");
                    ++k;
                    return new String[0];
                }
            }
        };
    }
}
