package de.unijena.bioinf.ms.io.projectspace;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.sirius.IdentificationResult;

public interface SiriusLocations {
    //experiment level
    Location SIRIUS_TREES_JSON = new Location("trees", null, ".json");
    Location SIRIUS_TREES_DOT = new Location("trees", null, ".dot");
    Location SIRIUS_ANNOTATED_SPECTRA = new Location("spectra", null, ".ms");
    Location SIRIUS_SPECTRA = new Location(null, "spectrum", ".ms");
    Location SIRIUS_SUMMARY = new Location(null, "summary_sirius", ".csv");
    Location SIRIUS_EXP_INFO_FILE = new Location(null, "experiment", ".info");
    //project-space level
    Location SIRIUS_VERSION_FILE = new Location(null, "version", ".txt");
    Location SIRIUS_CITATION_FILE = new Location(null, "cite", ".txt");
    Location SIRIUS_FORMATTER_FILE = new Location(null, ".format","");

    static String makeFileName(IdentificationResult result) {
        return result.getRank() + "_" + result.getMolecularFormula() + "_" + simplify(result.getPrecursorIonType());
    }

    static String simplify(PrecursorIonType precursorIonType) {
        return precursorIonType.toString().replaceAll("[\\[\\] _]", "");
    }

    class Location {
        public final String directory;
        private final String fileName;
        public final String fileExtension;

        public Location(String directory, String fileName, String fileExtension) {
            this.directory = directory;
            this.fileExtension = fileExtension;
            this.fileName = fileName;
        }

        public String fileName() {
            if (fileName == null)
                throw new UnsupportedOperationException("This location name ist IdentificationResult  dependent");

            return fileName + fileExtension;
        }

        public String fileName(IdentificationResult result) {
            if (fileName != null || result == null)
                return fileName();

            return makeFileName(result) + fileExtension;
        }

        public String toAbsolutePath(ExperimentDirectory ex, IdentificationResult result) {
            StringBuilder location = new StringBuilder();
            if (ex != null)
                location.append(ex.getDirectoryName()).append("/");

            if (directory != null && !directory.isEmpty())
                location.append(directory).append("/");

            location.append(fileName(result));

            return location.toString();
        }
    }
}
