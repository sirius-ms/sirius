package de.unijena.bioinf.FragmentationTreeConstruction.computation.inputValidator;

/**
 * Can be implemented by logger, PrintStreams or others to track warnings in the validation process
 */
public interface Warning {

    public static class Noop implements Warning{

        @Override
        public void warn(String message) {
            // dead code!
        }
    }

    public void warn(String message);

}
