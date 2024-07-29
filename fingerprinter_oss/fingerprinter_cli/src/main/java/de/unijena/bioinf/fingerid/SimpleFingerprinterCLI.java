package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.ArrayFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.FPIter;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.fingerid.fingerprints.FixedFingerprinter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;

public class SimpleFingerprinterCLI {

    public static void main(String[] args) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line = null;
        final FixedFingerprinter fingerprinter = new FixedFingerprinter(CdkFingerprintVersion.getDefault());
        try {
            MaskedFingerprintVersion V = MaskedFingerprintVersion.fromString(new BufferedReader(new InputStreamReader(SimpleFingerprinterCLI.class.getResourceAsStream("/fingerprints.mask"))).readLine());
            while (reader.ready() && (line = reader.readLine()) != null) {
                if (line.isBlank()) return;
                ArrayFingerprint fp = fingerprinter.computeFingerprintFromSMILES(line);

                System.out.println(V.mask(fp).toTabSeparatedString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
