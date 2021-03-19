/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

import groovy.sql.Sql

import java.nio.file.Paths
import java.sql.Driver


//this script was not used for conversion. but is shows nicely how easy we can manipulate our db and use out java
//classes with groovy
@GrabConfig(systemClassLoader=true)
@Grab(group='org.postgresql', module='postgresql', version='9.4-1205-jdbc42')

File propertiesFile = Paths.get(System.properties['user.home'], 'gradle.properties').toFile()
Properties props = new Properties()

propertiesFile.withReader("UTF8", {
    props.load(it)
})


def driver = Class.forName('org.postgresql.Driver').newInstance() as Driver

props.setProperty("DB_user", props['de.unijena.bioinf.build.artifactory.username'])
props.setProperty("DB_password", props['de.unijena.bioinf.build.artifactory.password'])


//println(props['DB_user'])
//println(props['DB_password'])

def conn = driver.connect("jdbc:postgresql://fingerid1.bioinf.uni-jena.de:5432/pubchem", props)
def sql = new Sql(conn)

/*
 * Display All table
 */
println "All Records:"
def query1 = "select inchi_key_1, formula from public.structures" //where formula='C24H34O10'
sql.eachRow(query1, {structure ->
    println(structure.inchi_key_1)
    def query2 = "select compound_id from ref.pubchem where inchi_key_1=${structure.inchi_key_1}"
    def pmids = []
    sql.eachRow(query2, {cid ->
        def query3 =  "select pubmed_id from pubchem.pmid where compound_id=${cid.compound_id}"
        sql.eachRow(query3,{pmid ->
            pmids += [pmid.pubmed_id]
        })
    })
    def finalQuery = "${structure.inchi_key_1} - ${structure.formula} - ${pmids}"
    println(finalQuery)
})