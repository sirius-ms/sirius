package de.unijena.bioinf.babelms.ms;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Peak;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An Importer for FSU Jena MS file format.
 *
 * This Importer retrieves information contained in a *.ms file about a
 * molecule and its spectra.
 *
 * author: j helmuth
 * date: Mar 3, 2011
 *
 */
public class MsParser {
	/**
	 * The file to be read
	 */
	File f;
    /**
     * contains all information of the file;
     */
    MSInfo msInfo;

    PrintStream errorStream;

    public MsParser() {
        this(true);
    }

    public MsParser(boolean warnings) {
        if (warnings) errorStream = System.err;
        else errorStream = null;
    }

	/**
	 * @return
	 * 	the file format for this parser
	 */
	public String getFormat() {
		return "MS";
	}

    public PrintStream getErrorStream() {
        return errorStream;
    }

    public void setErrorStream(PrintStream errorStream) {
        this.errorStream = errorStream;
    }

    public MSInfo getData(File file) throws IOException {
		if (file == null)
            throw new NullPointerException("File for molecule import must not be null!");
        f = file;
        readData();
        return msInfo;
	}

    private static Pattern cEPattern = Pattern.compile("(-?\\d+(?:\\.\\d+)?)(?:\\s*-\\s*(-?\\d+(?:\\.\\d+)?))?");

	/**
	 * Parsing method.
	 *
	 * @throws Exception
	 */
	private void readData() throws IOException {
		/*
		 * open Scanner
		 */
		Scanner s = new Scanner(f);

		/*
		 * read first lines of meta information on molecule
		 */
		String parentMassString = null;
		String chargeString = null;
		Peak parentPeak = null;
		double distFocusedMass = Double.POSITIVE_INFINITY;

        msInfo = new MSInfo();


		while (s.hasNextLine()) {
			String line = s.nextLine();
			if (line.length() == 0)
				break; //parsing meta is finished
			else if (line.contains("compound"))
				msInfo.setMoleculeNameString(line.replace(">", "").replace("compound","").trim());
			else if (line.contains("formula"))
				msInfo.setMolecularFormulaString(line.split("\\s+")[1].trim());
			else if (line.contains("parentmass"))
				parentMassString = line.split("\\s+")[1].trim();
			else if (line.contains("charge"))
				chargeString = line.split("\\s+")[1].trim();
		}
		if (parentMassString != null){
			try {
                msInfo.setParentMass(Double.parseDouble(parentMassString));
			} catch (NumberFormatException e) {
				if (errorStream != null) errorStream.println("File " + f + " does not contain a proper parent mass value for the molecule.");
			}
		}
        if (chargeString != null){
			try {
                msInfo.setCharge(Double.parseDouble(chargeString));
			} catch (NumberFormatException e) {
				if (errorStream != null) errorStream.println("File " + f + " does not contain a proper charge value for the molecule.");
			}
		}

		// finished parsing meta



		/*
		 * read spectra
		 */
		List<MsSpectrum> spectra = new ArrayList<MsSpectrum>();
		int specCount = 1;
        boolean lastLineMetaInfo = true;
        String specLine = null;
		while (s.hasNextLine()) {
            String line = null;
            if (lastLineMetaInfo) line = s.nextLine().trim();

            if ((line!=null) && (line.startsWith("#") || line.length() == 0)) //comment
                continue;

            else { // parse spectrum
				/*
				 * parse information this spectrum
				 */
				String collisionEnergyString = null;
				String totalIonCurrentString = null;
				String retentionTimeString = null;
				int msLevel = 2; //default is MS2
                MsSpectrum propSpectrum = new MsSpectrum();

				boolean firstLoop = true;
				if (lastLineMetaInfo) specLine = line;
                lastLineMetaInfo = true;
				while (s.hasNextLine()) {
					if (!firstLoop)
						specLine = s.nextLine().trim();
					else
						firstLoop = false;

                    if (specLine.startsWith("#")) continue;
					if (specLine.length() == 0){ // break condition when peaks have ended
						lastLineMetaInfo = true;
                        break;

                    }
                    if (specLine.startsWith(">") && !lastLineMetaInfo){  // break condition without blank line
                        break;
                    }
                    else if (specLine.contains("ms1peaks")){
                        msLevel = 1;
                        lastLineMetaInfo = true;
                    }

					else if (specLine.contains("retention")){
                        retentionTimeString = specLine.split("\\s")[1].trim();
                        lastLineMetaInfo = true;
                    }
					else if (specLine.contains("collision")){
					    collisionEnergyString = specLine.split("\\s+", 2)[1].trim();
                        lastLineMetaInfo = true;
                    }
					else if (specLine.contains("tic")){
                        totalIonCurrentString = specLine.split("\\s+")[1].trim();
                        lastLineMetaInfo = true;
                    }
					else  {
						String[] peakPair = specLine.split("\\s+");

						try {
							double mass = Double.parseDouble(peakPair[0]);
							double intensity = Double.parseDouble(peakPair[1]);
							Peak peak = new Peak(mass, intensity);

                            lastLineMetaInfo = false;

							propSpectrum.addPeak(peak);
							if (Math.abs(peak.getMass()-msInfo.getParentMass()) < distFocusedMass){
								parentPeak = peak;
								distFocusedMass = Math.abs(peak.getMass()-msInfo.getParentMass());
							}
						} catch (Exception e) {	}
					}
				}
                propSpectrum.setMsLevel(msLevel);
				// finished parsing information this spectrum



				if (msLevel > 1 && collisionEnergyString != null) {
					try {
                        final Matcher m = cEPattern.matcher(collisionEnergyString);
                        if (m.find()) {
                            final double mz1 = Double.parseDouble(m.group(1));
                            final double mz2 = m.group(2) != null ? Double.parseDouble(m.group(2)) : mz1;
                            propSpectrum.setCollisionEnergy(new CollisionEnergy(mz1, mz2));
                        } else if (errorStream != null) {
                            errorStream.println("File " + f + " does not contain a proper collision energy value of the " + specCount + "th spectrum:\nCan't parse '" + collisionEnergyString + "'");
                        }
					} catch (NumberFormatException e) {
						if (errorStream != null) errorStream.println("File " + f + " does not contain a proper collision energy value of the " + specCount + "th spectrum.");
					}
				}
				if (totalIonCurrentString != null) {
					try {
                        propSpectrum.setTotalIonCurrent(Double.parseDouble(totalIonCurrentString));
					} catch (NumberFormatException e) {
						if (errorStream != null) errorStream.println("File " + f + " does not contain a proper total ion current value of the " + specCount + "th spectrum.");
					}
				}
				if (retentionTimeString != null) {
					try {
                        propSpectrum.setRetentionTime(Double.parseDouble(retentionTimeString));
					} catch (NumberFormatException e) {
						if (errorStream != null) errorStream.println("File " + f + " does not contain a proper retention time value of the " + specCount + "th spectrum.");
					}
				}
				//finished constructing spectrum


				/*
				 * add spectrum and increase spectrum count
				 */
                spectra.add(propSpectrum);
				specCount++;
			}
		}
		// finished parsing spectra & now add spectra to Molecule

        // set parent peak
        if (parentPeak != null && Math.abs(parentPeak.getMass() - msInfo.getParentMass()) < 1) {
            msInfo.setParentPeak(parentPeak.clone());
        } else {
            if (errorStream != null) errorStream.println("No proper parent peak found! (" + f.getName() + ")");
        }

        /*
         * add spectra to msInfo
         */
        msInfo.setSpectra(spectra.toArray(new MsSpectrum[0]));

		/*
		 * close Scanner
		 */
		s.close();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Jena MS Molecule/Peak Importer (ms)";
	}
}




