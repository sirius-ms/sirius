package de.unijena.bioinf.ms.middleware.projectspace;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;

public final class ProjectSpaceId {

    public final @NotNull  String name;
    public final @NotNull  File path;

    public ProjectSpaceId(@NotNull  String name, @NotNull  File path) {
        this.name = name;
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectSpaceId that = (ProjectSpaceId) o;
        return name.equals(that.name) &&
                path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, path);
    }
}
