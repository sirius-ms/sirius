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