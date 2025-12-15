package de.unijena.bioinf.ms.middleware.service.search.mappers;

import lombok.Getter;

public class IndexMapperNotFoundException extends RuntimeException {
    private static final String message = "FieldMapper '%s' not found and could not be instantiated. Either define the mapper manually or provide no argument constructor.";
    @Getter
    private final Class<? extends FieldMapper> missingMapperClass;

    public IndexMapperNotFoundException(Class<? extends FieldMapper> missingMapperClass) {
        this(missingMapperClass, null);
    }

    public IndexMapperNotFoundException(Class<? extends FieldMapper> missingMapperClass, Throwable cause) {
        super(String.format(message, missingMapperClass.getSimpleName()), cause);
        this.missingMapperClass = missingMapperClass;
    }

}
