package demo.platform;

import com.hp.systinet.repository.remote.client.RepositoryClient;
import com.hp.systinet.repository.remote.client.impl.RepositoryClientFactory;
import com.hp.systinet.repository.sdm.ArtifactBase;
import com.hp.systinet.repository.sdm.desc.ArtifactDescriptor;
import com.hp.systinet.repository.sdm.desc.PropertyDescriptor;
import com.hp.systinet.repository.sdm.properties.MultiplePropertyValue;
import com.hp.systinet.repository.sdm.properties.PropertyValue;
import com.hp.systinet.repository.sdm.properties.Relation;
import com.hp.systinet.repository.sdm.properties.SinglePropertyValue;
import com.hp.systinet.repository.sdm.properties.support.ComposedSinglePropertyValueTemplate;
import com.hp.systinet.repository.sdm.properties.support.PrimitiveSinglePropertyValueTemplate;
import com.hp.systinet.repository.sdm.propertytypes.Category;
import com.hp.systinet.repository.sdm.propertytypes.UuidProperty;
import org.openrdf.model.*;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.vocabulary.FOAF;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.http.HTTPRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.Rio;

import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by dvasunin on 06.06.14.
 */
public class Main {

    private RepositoryClient repositoryClient;
    private HTTPRepository rdfRepository;
    private Model model;
    private ValueFactory valueFactory;
    private String systinetNS = "http://systinet.local:8080/soa/web/service-catalog/";
    private String systinetSdmNameNS = systinetNS + "sdm_name/";
    private String systinetSdmPropNS = systinetNS + "sdm_prop/";
    private String systinetSdmPropTypeNS = systinetNS + "sdm_proptype/";
    private String systinetCardinalityNS = systinetNS + "cardinality/";
    private String systinetArtifactNS =  systinetNS + "artifact/";
    private String systinetRelationNS =  systinetNS + "relation/";
    private String systinetCategoryNS =  systinetNS + "category/";
    private URI cardinalityType;


    public Main(String systinetURL, String user, String password, String rdfUrl, String rdfRepositoryID) throws URISyntaxException {
        repositoryClient = RepositoryClientFactory.createRepositoryClient(
                systinetURL, user, password, false, null, 0);
        rdfRepository =  new HTTPRepository(rdfUrl, rdfRepositoryID);
        model = new LinkedHashModel();
        valueFactory = rdfRepository.getValueFactory();
        cardinalityType = valueFactory.createURI(systinetCardinalityNS, "type");
        model.add(
                valueFactory.createStatement(
                        cardinalityType,
                        RDF.TYPE,
                        RDF.ALT
                )
        );
        model.add(
                valueFactory.createStatement(
                        cardinalityType,
                        RDFS.MEMBER,
                        valueFactory.createURI(systinetCardinalityNS, "SINGLE_OPTIONAL")
                )
        );
        model.add(
                valueFactory.createStatement(
                        cardinalityType,
                        RDFS.MEMBER,
                        valueFactory.createURI(systinetCardinalityNS, "MULTIPLE_OPTIONAL")
                )
        );
        model.add(
                valueFactory.createStatement(
                        cardinalityType,
                        RDFS.MEMBER,
                        valueFactory.createURI(systinetCardinalityNS, "SINGLE_REQUIRED")
                )
        );
        model.add(
                valueFactory.createStatement(
                        cardinalityType,
                        RDFS.MEMBER,
                        valueFactory.createURI(systinetCardinalityNS, "MULTIPLE_REQUIRED")
                )
        );
    }

    private void addArtifacts() {
        //System.out.println(artifactType);
        List<ArtifactBase> artifacts = repositoryClient.search(null, null, null, 0,  10000);
        for (ArtifactBase au : artifacts) {
            ArtifactBase a = repositoryClient.getArtifact(au.get_uuid().toString());
            addArtifact(a);
        }
    }

    private void addPropertyValue(Resource res, URI pred, PropertyValue pv) {
        Literal value = null;
        if(pv instanceof Category){
            Category ct = (Category)pv;
            Resource r = valueFactory.createBNode();
            model.add(res, pred, r);
            model.add(r, valueFactory.createURI(systinetCategoryNS, "taxonomyURI"), valueFactory.createLiteral(ct.getTaxonomyURI()));
            model.add(r, valueFactory.createURI(systinetCategoryNS, "name"), valueFactory.createLiteral(ct.getName()));
            model.add(r, valueFactory.createURI(systinetCategoryNS, "value"), valueFactory.createLiteral(ct.getVal()));
        } else if(pv instanceof SinglePropertyValue) {
            model.add(res, pred, valueFactory.createLiteral(((SinglePropertyValue) pv).getValue().toString()));
         } else {
            model.add(res, pred, valueFactory.createLiteral(pv.toString()));
         }
    }
    
    public void addArtifact(ArtifactBase a) {
        URI artifactResource = valueFactory.createURI(systinetArtifactNS, a.get_uuid().toString());
        for(PropertyDescriptor propertyDesc : a.getArtifactDescriptor().enumerateProperties()) {
            if (!propertyDesc.isRelationship())
            {
                if(a.getProperty(propertyDesc.getSdmName()) != null) {
                    Literal value = null;
                    URI propertyType = valueFactory.createURI(systinetSdmPropNS, propertyDesc.getSdmName());
                    model.add(
                            valueFactory.createStatement(
                                    propertyType,
                                    cardinalityType,
                                    valueFactory.createURI(systinetCardinalityNS, propertyDesc.getPropertyCardinality().name())
                            )
                    );
                    model.add(
                            valueFactory.createStatement(
                                    propertyType,
                                    RDF.TYPE,
                                    valueFactory.createURI(systinetSdmPropTypeNS, propertyDesc.getPropertyTypeDescriptor().getPropertyTypeClass().getSimpleName())
                            )
                    );
                    if (propertyDesc.getPropertyCardinality().isMultiple()) {
                        for(SinglePropertyValue pv: a.getMultiProperty(propertyDesc.getSdmName()))
                            addPropertyValue(artifactResource, propertyType, pv);

                    } else {
                        addPropertyValue(artifactResource, propertyType, a.getProperty(propertyDesc.getSdmName()));
                    }
                }
            }
        }
        for (Relation r : a.getRelations()) {
            if (r.isOutgoing()) {
                URI subj = valueFactory.createURI(systinetArtifactNS, a.get_uuid().toString());
                URI pred = valueFactory.createURI(systinetRelationNS, r.getName());
                URI obj  = valueFactory.createURI(systinetArtifactNS, r.getTargetId().toString());
                model.add(valueFactory.createStatement(subj, pred, obj));
            }
        }
    }


//    public void enumerateArtifacts() {
//        for (ArtifactDescriptor artifactDescriptor : repositoryClient.getArtifactRegistry().enumerateArtifactDescriptors()) {
//            addArtifacts(artifactDescriptor.getSdmName());
//        }
//    }


    private void printModel() throws RDFHandlerException {
        System.out.println(model.size());
        Rio.write(model.unmodifiable(), System.out, RDFFormat.RDFXML);
    }

    private void updateRepository() throws RepositoryException {
        rdfRepository.getConnection().add(model);
    }

    public static void main(String[] args) {
        Main m = null;
        try {
            m = new Main("http://systinet.local:8080/soa", "admin", "admin", "http://elab.lab.uvalight.net:8080/openrdf-sesame/", "Test");
            //m.collectStatements();
            //ArtifactBase a = m.repositoryClient.getArtifact("be247a9a-d614-4ddc-9c26-a514b1d52042");
           // m.addArtifact(a);
            m.addArtifacts();
            m.printModel();
            m.updateRepository();
        } catch (Exception e) {
            e.printStackTrace();
        };
    }

}