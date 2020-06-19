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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DBSelectionList extends JCheckBoxList<SearchableDatabase> {
    public final static Set<String> BLACK_LIST = Set.of(DataSource.ADDITIONAL.realName, DataSource.TRAIN.realName,
            DataSource.PUBCHEMANNOTATIONBIO.realName, DataSource.PUBCHEMANNOTATIONDRUG.realName, DataSource.PUBCHEMANNOTATIONFOOD.realName, DataSource.PUBCHEMANNOTATIONSAFETYANDTOXIC.realName,
            DataSource.SUPERNATURAL.realName
    );

    public DBSelectionList() {
        this((String) null);
    }

    public DBSelectionList(@Nullable String descriptionKey) {
        this(descriptionKey, SearchableDatabases.getAvailableDatabases().stream().
                filter(db -> !BLACK_LIST.contains(db.name())).
                collect(Collectors.toList()));
    }

    protected DBSelectionList(@Nullable String descKey, @NotNull DataSource... values) {
        this(descKey, Stream.of(values).map(DataSource::name).toArray(String[]::new));
    }

    protected DBSelectionList(@Nullable String descKey, @NotNull String... dbNameOrPath) {
        this(descKey, Stream.of(dbNameOrPath).map(SearchableDatabases::getDatabase).flatMap(Optional::stream).
                collect(Collectors.toList()));
    }

    public DBSelectionList(@NotNull List<SearchableDatabase> values) {
        this(null, values);
    }

    public DBSelectionList(@Nullable String descKey, @NotNull List<SearchableDatabase> values) {
        super(values, (a,b) -> a.name().equals(b.name()));
        if (descKey != null)
            GuiUtils.assignParameterToolTip(this, descKey);
    }
}
