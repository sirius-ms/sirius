package de.unijena.bioinf.ms.middleware.model.common;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A generic placeholder class used to tell OpenAPI that a field
 * can be any primitive type (String, Number, Boolean) or Object.
 */
@Schema(
        name = "AnyValue", // This becomes the key in #/components/schemas/
        description = "Can be a String, Number or Boolean",
        anyOf = {
                String.class,
                Integer.class,
                Double.class,
                Boolean.class
        }
)
public class AnyValue {
    // This class remains empty.
    // It is never instantiated, only referenced by annotations.
}
