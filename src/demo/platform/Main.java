package demo.platform;

import com.hp.systinet.repository.remote.client.RepositoryClient;
import com.hp.systinet.repository.remote.client.impl.RepositoryClientFactory;
import com.hp.systinet.repository.sdm.ArtifactBase;
import com.hp.systinet.repository.sdm.properties.Relation;
import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.http.HTTPRepository;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by dvasunin on 06.06.14.
 */
public class Main {

    private RepositoryClient repositoryClient;
    private HTTPRepository rdfRepository;
    private Model allRelationsModel;
    private ValueFactory valueFactory;
    private String systinetNS = "http://systinet.local:8080/soa/web/service-catalog/";


    public Main(String systinetURL, String user, String password, String rdfUrl, String rdfRepositoryID) throws URISyntaxException {
        //Setup repository client
        repositoryClient = RepositoryClientFactory.createRepositoryClient(
                systinetURL, user, password, false, null, 0);
        rdfRepository =  new HTTPRepository(rdfUrl, rdfRepositoryID);
        allRelationsModel = new LinkedHashModel();
        valueFactory = rdfRepository.getValueFactory();
    }

    private void collectStatements() {
        List<ArtifactBase> artifacts = repositoryClient.search(null, null, null, 0,  10000);
        for (ArtifactBase au : artifacts) {
            ArtifactBase a = repositoryClient.getArtifact(au.get_uuid().toString());
            for (Relation r : a.getRelations()) {
                if (r.isOutgoing()) {
                    URI subj = valueFactory.createURI(systinetNS + "artifact/" + a.get_uuid());
                    URI pred = valueFactory.createURI(systinetNS + "relation/" + r.getName());
                    URI obj  = valueFactory.createURI(systinetNS + "artifact/" + r.getTargetId());
                    allRelationsModel.add(valueFactory.createStatement(subj, pred, obj));
                }
            }
        }
    }

    private void printModel(){
        System.out.println(allRelationsModel.size());
        for(Statement s : allRelationsModel) {
            System.out.println(s.getSubject() + "\t" + s.getPredicate() + "\t" + s.getObject());
        }
    }

    private void updateRepository() throws RepositoryException {
        rdfRepository.getConnection().add(allRelationsModel);
    }

    public static void main(String[] args) {
        Main m = null;
        try {
            m = new Main("http://systinet.local:8080/soa", "admin", "admin", "http://elab.lab.uvalight.net:8080/openrdf-sesame/", "Test");
            m.collectStatements();
            m.printModel();
            m.updateRepository();
        } catch (Exception e) {
            e.printStackTrace();
        };
    }

}