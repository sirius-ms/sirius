package de.isas.mztab2.io;

import de.isas.lipidomics.mztab2.validation.Validator;
import de.isas.mztab2.model.MzTab;
import de.isas.mztab2.model.ValidationMessage;
import uk.ac.ebi.pride.jmztab2.utils.errors.MZTabErrorType;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SiriusWorkspaceMzTabValidatingWriter extends MzTabValidatingWriter {

    //needed because this stupid variables are private in super class :-/
    // why making a final variable private????
    protected final Validator<MzTab> validator;
    protected final MzTabWriterDefaults writerDefaults;
    protected final boolean skipWriteOnValidationFailure;


    public SiriusWorkspaceMzTabValidatingWriter() {
        this(new WriteAndParseValidator(System.out, MZTabErrorType.Level.Info, 100),
                new MzTabWriterDefaults(), true);
    }

    public SiriusWorkspaceMzTabValidatingWriter(Validator<MzTab> validator, boolean skipWriteOnValidationFailure) {
        this(validator, new MzTabWriterDefaults(), skipWriteOnValidationFailure);
    }

    public SiriusWorkspaceMzTabValidatingWriter(Validator<MzTab> validator, MzTabWriterDefaults writerDefaults, boolean skipWriteOnValidationFailure) {
        super(validator, writerDefaults, skipWriteOnValidationFailure);
        this.validator = validator;
        this.writerDefaults = writerDefaults;
        this.skipWriteOnValidationFailure = skipWriteOnValidationFailure;
    }

    public Optional<List<ValidationMessage>> write(Writer writer, MzTab mzTab) throws IOException {
        final List<ValidationMessage> validationMessages = Optional.ofNullable(validator.validate(mzTab)).
                orElse(Collections.emptyList());

        if (skipWriteOnValidationFailure && !validationMessages.isEmpty()) {
            return Optional.of(validationMessages);
        }

        new SiriusWorkspaceMzTabNonValidatingWriter(writerDefaults).write(writer, mzTab);
        return Optional.of(validationMessages);
    }
}
