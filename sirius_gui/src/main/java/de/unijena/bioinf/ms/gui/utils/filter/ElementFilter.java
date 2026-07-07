package de.unijena.bioinf.ms.gui.utils.filter;

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ElementFilter {
    @NotNull
    final FormulaConstraints constraints;
    final boolean matchFormula;
    final boolean matchPrecursorFormula;

    public ElementFilter(@Nullable FormulaConstraints constraints) {
        this(constraints, true, true);
    }

    public ElementFilter(@Nullable String constraints) {
        this(constraints, true, true);
    }

    public ElementFilter(@Nullable String constraints, boolean matchFormula, boolean matchPrecursorFormula) {
        this((constraints != null && !constraints.isBlank())
                        ? FormulaConstraints.fromString(constraints)
                        : FormulaConstraints.empty(),
                matchFormula, matchPrecursorFormula);
    }

    public ElementFilter(FormulaConstraints constraints, boolean matchFormula, boolean matchPrecursorFormula) {
        this.constraints = constraints == null ? FormulaConstraints.empty() : constraints;
        this.matchFormula = matchFormula;
        this.matchPrecursorFormula = matchPrecursorFormula;
    }

    public boolean isActive() {
        return !constraints.equals(FormulaConstraints.empty()) && (matchFormula || matchPrecursorFormula);
    }

    public FormulaConstraints getConstraints() {
        return constraints;
    }

    public boolean isMatchFormula() {
        return matchFormula;
    }

    public boolean isMatchPrecursorFormula() {
        return matchPrecursorFormula;
    }

    public static final ElementFilter DISABLED = new ElementFilter(FormulaConstraints.empty(), true, true);

    public static ElementFilter disabled() {
        return DISABLED;
    }
}
