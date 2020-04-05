package de.unijena.bioinf.ms.gui.compute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

public class ParameterBinding extends HashMap<String, Supplier<String>> {

    public ParameterBinding(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    public ParameterBinding(int initialCapacity) {
        super(initialCapacity);
    }

    public ParameterBinding() {
        super();
    }

    public List<String> asParameterList() {
        final List<String> out = new ArrayList<>(size() * 2);

        forEach((k, v) -> {
            out.add("--" + k);
            out.add(v.get());
        });

        return out;
    }
}
