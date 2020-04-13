package de.unijena.bioinf.ms.gui.compute;

import java.util.List;

public interface ParameterProvider {
    default List<String> asParameterList() {
        return getParameterBinding().asParameterList();
    }

    ParameterBinding getParameterBinding();
}
