package de.unijena.bioinf.ms.persistence.model.properties;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@Getter
public class ProjectSourceFormats {
    public static final String GENERIC_DIRECT_IMPORT = "GENERIC";
    public static final String EXPLORER_DIRECT_IMPORT = "EXPLORER";
    private final LinkedHashSet<String> formats;
    private final LinkedHashSet<String> directImports;

    public ProjectSourceFormats() {
        this(new LinkedHashSet<>(), new LinkedHashSet<>());
    }

    public ProjectSourceFormats(LinkedHashSet<String> formats, LinkedHashSet<String> directImports) {
        this.formats = formats;
        this.directImports = directImports;
    }

    public void addDirectImport(@NotNull String sourceTag) {
        directImports.add(sourceTag);
    }

    public void addDirectImports(@NotNull Collection<String> sourceTags) {
        sourceTags.stream().filter(Objects::nonNull).forEach(this.directImports::add);
    }

    public void addFormat(@NotNull String format) {
        formats.add(format);
    }

    public void addFormats(@NotNull Collection<String> formats) {
        formats.stream().filter(Objects::nonNull).forEach(this.formats::add);
    }

   public static ProjectSourceFormats fromFormats(String... formats) {
        return fromFormats(List.of(formats));
   }

   public static ProjectSourceFormats fromFormats(Collection<String> formats) {
       ProjectSourceFormats it = new ProjectSourceFormats();
       it.addFormats(formats);
       return it;
   }

   public static ProjectSourceFormats fromDirectImports(String... sources) {
       return fromDirectImports(List.of(sources));
   }

   public static ProjectSourceFormats fromDirectImports(Collection<String> sources) {
       ProjectSourceFormats it = new ProjectSourceFormats();
       it.addDirectImports(sources);
       return it;
   }
}
