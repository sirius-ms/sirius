package de.unijena.bioinf.ms.persistence.model.core.run;

import jakarta.persistence.Id;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Each LC/MS run maps an index to a scan number and a retention time.
 *
 * It also contains other fields related to projecting the run onto the merged run. I don't
 * know a better name for this class, though.
 */
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class RetentionTimeAxis {

    @Id
    private long runId;
    private int[] scanIndizes;
    private int[] scanNumbers;
    private String[] scanIdentifiers;
    private double[] retentionTimes;
    /**
     * Estimated noise level per scan to compute a local SNR value.
     */
    private float[] noiseLevelPerScan;

    /**
     * The scaling used for normalizing intensities
     */
    private double normalizationFactor = 1d;

    /**
     * we do not store the recalibration function, because it might be any kind of function
     * (although it will be linear or spline in most cases). Instead, we just store
     * the mapping of the retention times. The recalibration function can be approximated from that.
     * Also, we can restore the ScanPointInterpolator knowing the recalibrated retention times!
     */
    private double[] recalbratedRetentionTimes;

}
