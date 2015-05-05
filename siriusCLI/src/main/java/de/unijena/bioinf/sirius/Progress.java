package de.unijena.bioinf.sirius;

public interface Progress {

    public static class Quiet implements Progress {

        @Override
        public void init(double maxProgress) {

        }

        @Override
        public void update(double currentProgress, double maxProgress, String value) {

        }

        @Override
        public void finished() {

        }

        @Override
        public void info(String message) {

        }
    };

    public void init(double maxProgress);

    public void update(double currentProgress, double maxProgress, String value);

    public void finished();

    public void info(String message);

}
