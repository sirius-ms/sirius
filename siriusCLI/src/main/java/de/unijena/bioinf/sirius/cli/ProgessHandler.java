package de.unijena.bioinf.sirius.cli;

public interface ProgessHandler {

    public void init(int max);
    public void increase(int val);
    public void finished();

    public static final ProgessHandler Noop = new ProgessHandler() {

        @Override
        public void init(int max) {

        }

        @Override
        public void increase(int val) {

        }

        @Override
        public void finished() {

        }
    };

}
