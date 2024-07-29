/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.babelms.gnps;

import lombok.Data;

@Data
public class GnpsRecord {
    private String spectrum_id;
    private String source_file;
    private String task;
    private String scan;
    private String ms_level;
    private String library_membership;
    private String spectrum_status;
    private String peaks_json;
    private String splash;
    private String submit_user;
    private String Compound_Name;
    private String Ion_Source;
    private String Compound_Source;
    private String Instrument;
    private String PI;
    private String Data_Collector;
    private String Adduct;
    private String Scan;
    private String Precursor_MZ;
    private String ExactMass;
    private String Charge;
    private String CAS_Number;
    private String Pubmed_ID;
    private String Smiles;
    private String INCHI;
    private String INCHI_AUX;
    private String Library_Class;
    private String SpectrumID;
    private String Ion_Mode;
    private String create_time;
    private String task_id;
    private String user_id;
    private String InChIKey_smiles;
    private String InChIKey_inchi;
    private String Formula_smiles;
    private String Formula_inchi;
    private String url;
}
