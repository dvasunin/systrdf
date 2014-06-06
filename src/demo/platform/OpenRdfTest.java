package demo.platform;

/**
 * User: dvasunin
 * Date: 05.06.2014
 * Time: 12:55
 * To change this template use File | Settings | File Templates.
 */

import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.http.HTTPRepository;

public class OpenRdfTest {

    static String spql1 = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX dbpedia-owl: <http://dbpedia.org/ontology/> SELECT * WHERE { ?companyURL rdf:type               dbpedia-owl:Company      ; dbpprop:companyName   ?corporation              ; dbpedia-owl:foundedBy ?founderURL}  LIMIT 2";


    public static void main(String[] args) {
        OpenRdfTest m = new OpenRdfTest();
        m.publishStatement();
        System.exit(0);
        try {
            String endpointURL = "http://dbpedia.org/sparql";
            HTTPRepository dbpediaEndpoint =
                    new HTTPRepository(endpointURL, "");
            dbpediaEndpoint.initialize();

            RepositoryConnection conn =
                    dbpediaEndpoint.getConnection();
            try {
                TupleQuery query = conn.prepareTupleQuery(org.openrdf.query.QueryLanguage.SPARQL, spql1);
                TupleQueryResult result = query.evaluate();

                while(result.hasNext()) {
                    BindingSet bs = result.next();
                    for(Binding b : bs) {
                        System.out.println(b.getName() + "\t:\t" + b.getValue());
                    }
                }
            } finally {
                conn.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public void publishStatement(){
        try {
            String endpointURL = "http://elab.lab.uvalight.net:8080/openrdf-sesame/";
            HTTPRepository dbpediaEndpoint =
                    new HTTPRepository(endpointURL, "Test");
            dbpediaEndpoint.initialize();
            ValueFactory factory = dbpediaEndpoint.getValueFactory();
            URI bob = factory.createURI("http://example.org/bob");
            URI name = factory.createURI("http://example.org/name");
            Literal bobsName = factory.createLiteral("Bob");
            Statement nameStatement = factory.createStatement(bob, name, bobsName);
            dbpediaEndpoint.getConnection().add(nameStatement);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}