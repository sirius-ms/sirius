/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.chemdb;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jdbi.v3.core.ConnectionFactory;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SqlIndex implements ChemicalBlobDatabase.Index {

    protected Jdbi jdbi;

    public SqlIndex(ConnectionFactory sqlDb) {
        this.jdbi = Jdbi.create(sqlDb).installPlugin(new SqlObjectPlugin());
        this.jdbi.registerArrayType(Long.class, "BIGINT");
    }

    public SqlIndex(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public long size() {
        return jdbi.withExtension(Dao.class, Dao::count);
    }

    @Override
    public List<MolecularFormula> getFormulas() {
        return jdbi.withExtension(Dao.class, Dao::getAll)
                .stream().map(Entry::asFormula).toList();
    }

    @Override
    public List<MolecularFormula> getFormulasByExactMass(double fromMass, double toMass) {
        return getFormulasByExactMassRaw(fromMass, toMass).stream().map(Entry::asFormula).toList();
    }

    private List<Entry> getFormulasByExactMassRaw(double fromMass, double toMass) {
        return jdbi.withExtension(Dao.class, db -> db.getByExactMass(fromMass, toMass));
    }

    @Override
    public boolean containsFormula(MolecularFormula formula) {
        return jdbi.withExtension(Dao.class, db -> db.containsFormula(formula));
    }

    @Override
    public List<FormulaCandidate> lookupMolecularFormulas(double ionMass, Deviation deviation, PrecursorIonType ionType) {
        final double mass = ionType.precursorMassToNeutralMass(ionMass);
        return getFormulasByExactMassRaw(mass - deviation.absoluteFor(ionMass), mass + deviation.absoluteFor(ionMass))
                .stream().map(e -> e.asFormulaCandidate(ionType)).toList();
    }

    public interface Dao {

        @SqlQuery("SELECT count(formula) FROM `COMPOUNDS` WHERE formula = :formula LIMIT :limit")
        long count(@NotNull @Bind("formula") String formula, @Bind("limit") long limit);

        @SqlQuery("SELECT count(formula) FROM `COMPOUNDS`")
        long count();

        default boolean containsFormula(@NotNull MolecularFormula formula) {
            return count(formula.toString(), 1) > 0;
        }

        @SqlQuery("SELECT formula, bits FROM `COMPOUNDS` WHERE formula = :formula")
        @RegisterBeanMapper(Entry.class)
        Entry getById(@NotNull @Bind("formula") String formula);

        default Entry getByFormula(@NotNull MolecularFormula formula) {
            return getById(formula.toString());
        }

        @SqlQuery("SELECT formula, bits FROM `COMPOUNDS` WHERE formula = :formula AND bits & :bits !=0")
        @RegisterBeanMapper(Entry.class)
        Entry getByIdAndFlags(@NotNull @Bind("formula") String formula, @Bind("bits") long bits);

        default Entry getByFormulaAndFlag(MolecularFormula formula, long bits) {
            return getByIdAndFlags(formula.toString(), bits);
        }

        @SqlQuery("SELECT formula, bits FROM `COMPOUNDS` WHERE exactMass <= :fromMass AND exactMass <= :toMass")
        @RegisterBeanMapper(Entry.class)
        List<Entry> getByExactMass(@Bind("fromMass") double fromMass, @Bind("toMass") double toMass);

        @SqlQuery("SELECT formula, bits FROM `COMPOUNDS` WHERE exactMass <= :fromMass AND exactMass <= :toMass AND bits & :bits !=0")
        @RegisterBeanMapper(Entry.class)
        List<Entry> getByMassAndFlags(@Bind("fromMass") double fromMass, @Bind("toMass") double toMass, @Bind("bits") long bits);

        @SqlQuery("SELECT formula, bits FROM `COMPOUNDS` WHERE bits & :bits !=0")
        @RegisterBeanMapper(Entry.class)
        List<Entry> getByFlags(@Bind("bits") long bits);

        @SqlQuery("SELECT formula, bits FROM `COMPOUNDS`")
        @RegisterBeanMapper(Entry.class)
        List<Entry> getAll();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    private static class Entry {
        private String formula;
        private long bits;

        public MolecularFormula asFormula() {
            return MolecularFormula.parseOrThrow(formula);
        }

        public FormulaCandidate asFormulaCandidate(@NotNull PrecursorIonType ionType) {
            return new FormulaCandidate(MolecularFormula.parseOrThrow(formula), ionType, bits);
        }
    }
}
