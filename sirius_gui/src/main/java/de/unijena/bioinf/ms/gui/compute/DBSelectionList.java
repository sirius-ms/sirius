package de.unijena.bioinf.ms.gui.compute;

import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.chemdb.SearchableDatabase;
import de.unijena.bioinf.chemdb.SearchableDatabases;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckBoxList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DBSelectionList extends JCheckBoxList<SearchableDatabase> {
    public DBSelectionList() {
        this(null);
    }

    public DBSelectionList(@Nullable String descriptionKey) {
        this(descriptionKey, SearchableDatabases.getAvailableDatabases());
    }

    protected DBSelectionList(@Nullable String descKey, @NotNull DataSource... values) {
        this(descKey, Stream.of(values).map(DataSource::name).toArray(String[]::new));
    }

    protected DBSelectionList(@Nullable String descKey, @NotNull String... dbNameOrPath) {
        this(descKey, Stream.of(dbNameOrPath).map(SearchableDatabases::getDatabase).flatMap(Optional::stream).
                collect(Collectors.toList()));
    }

    public DBSelectionList(@Nullable String descKey, @NotNull List<SearchableDatabase> values) {
        super(values);
        if (descKey != null)
            GuiUtils.assignParameterToolTip(this, descKey);
    }


}
