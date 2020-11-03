/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

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

    //needed because the variables are private in super class :-/
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
