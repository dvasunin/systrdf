// (C) Copyright 2003-2011 Hewlett-Packard Development Company, L.P.

package demo.platform;

import java.util.*;

import com.hp.systinet.lang.Pair;
import com.hp.systinet.repository.remote.client.RepositoryClient;
import com.hp.systinet.repository.remote.client.impl.RepositoryClientFactory;
import com.hp.systinet.repository.sdm.ArtifactBase;
import com.hp.systinet.repository.sdm.desc.ArtifactDescriptor;
import com.hp.systinet.repository.sdm.desc.PropertyDescriptor;
import com.hp.systinet.repository.sdm.properties.Relation;
import com.hp.systinet.repository.sdm.properties.SinglePropertyValue;
import com.hp.systinet.repository.sdm.properties.Uuid;
import com.hp.systinet.repository.sdm.propertytypes.Category;
import com.hp.systinet.repository.remote.client.security.ArtifactSecurity; 
import com.hp.systinet.repository.remote.client.security.Principal;
import com.hp.systinet.repository.remote.client.security.Role;
import com.hp.systinet.repository.remote.client.security.Right;
import com.hp.systinet.repository.structures.ArtifactPartSelector;
import com.hp.systinet.repository.util.PropertyFilters;
import org.apache.commons.collections.map.MultiValueMap;

/**
 * This demo shows how to work with ATOM REST Client.
 */
public class PlatformClientDemo {
    /* the demo setups artifact's criticality and then search by it */
    private static final String CRITICALITY_TAX_URI = "uddi:systinet.com:soa:model:taxonomies:impactLevel";
    private static final String CRITICALITY_VAL = "uddi:systinet.com:soa:model:taxonomies:impactLevel:high";
    /** repository client is a Java interface of the ATOM REST API */
    private static RepositoryClient repositoryClient;

    /**
     * Initialize REST client.
     */	 
    private static void initialize() throws Exception {
        // read demo properties
        String endpointBase = DemoProperties.getProperty("shared.http.urlbase","http://localhost:8080/systinet");
        String user = DemoProperties.getProperty("platform.demos.user.name","demouser");
        String password = DemoProperties.getProperty("platform.demos.user.password", "changeit");

        // initialize repository client
        repositoryClient = RepositoryClientFactory.createRepositoryClient(
                endpointBase, user, password, false, null, 0);

    }

    /**
     * Entry point. 
     * @throws Exception
     *             thrown on error.
     */
    public static void main(String[] args) throws Exception {
        initialize();
        //enumerateArtifacts();
        //enumerateArtifactProperties("businessServiceArtifact");
        //createSearchDelete();
        //createGetUpdateDelete();
        //demoArtifactSecurity();
        //searchByPath();
        myArtifactTest();
        // full text search can be executed only if it is enabled on the server
        /*
        fullTextSearch();
        */

        System.out.println("\nDemo FINISHED!");
    }


    public static void enumerateArtifacts() {
        System.out.println("\n=== enumerateArtifacts ===");
        // enumerating all artifacts
        List<ArtifactDescriptor> artifactDescriptors =
                repositoryClient.getArtifactRegistry().enumerateArtifactDescriptors();
        List<String> artifactTypes = new ArrayList<String>(artifactDescriptors.size());
        for (ArtifactDescriptor artifactDescriptor : artifactDescriptors) {
            artifactTypes.add(artifactDescriptor.getSdmName());
        }
        // sort and print the output
        String[] sorted = artifactTypes.toArray(new String[artifactTypes.size()]);
        Arrays.sort(sorted);
        System.out.println("There are following artifact types: ");
        for (String s : sorted) {
            System.out.println("   "+s);
            enumerateArtifactProperties(s);
        }
    }

    /**
     * Uses REST client introspection to enumerate properties 
     * of an artifact type specified.
     * @param sdmName SDM name of an artifact type
     */
    public static void enumerateArtifactProperties(String sdmName){
        System.out.println("\n=== enumerateArtifactProperties("+sdmName+") ===");
        ArtifactDescriptor descriptor = 
            repositoryClient.getArtifactRegistry().getArtifactDescriptor(sdmName);

        // enumerate properties, relationship excluding
        System.out.println(sdmName+" has the following properties: ");
        for (PropertyDescriptor propertyDesc : descriptor.enumerateProperties()) {
            if (!propertyDesc.isRelationship())
            {
                System.out.println("   "
                        + propertyDesc.getSdmName()
                        + " ("
                        + propertyDesc.getPropertyCardinality().name()
                        + " - "
                        + propertyDesc.getPropertyTypeDescriptor().getPropertyTypeClass().getSimpleName()
                        + ")");
            }
        }

        // enumerate relations
        System.out.println(sdmName+" has the following relations: ");
        for (PropertyDescriptor propertyDesc : descriptor.enumerateRelations()) {
            System.out.println("   "
                    + propertyDesc.getSdmName()
                    + " ("
                    + propertyDesc.getPropertyCardinality().name()
                    + " - "
                    + (propertyDesc.isIncomingRelationship()?"incoming":"outgoing")
                    + ")");
        }
    }


    /**
     * Shows how to create, search delete and purge artifact. 
     */
    public static void createSearchDelete() {
        System.out.println("\n=== createSearchDelete() ===");
        // create a new service artifact		
        ArtifactBase businessService = repositoryClient.getArtifactFactory()
        .newArtifact("businessServiceArtifact");
        businessService.setName("Demo Service Name");
        Category propertyCriticality = new Category(
                CRITICALITY_TAX_URI, "High",
                CRITICALITY_VAL);
        businessService.setProperty("criticality", propertyCriticality);
        // store it on the server and re-assign to have  
        // a persistent view of the artifact
        businessService = repositoryClient.createArtifact(businessService);
        System.out.println("Service artifact created, its UUID is: "
                + businessService.get_uuid().toString());

        List<Pair<String, String>> searchCriteria=new ArrayList<Pair<String,String>>();
        searchCriteria.add(new Pair<String, String>("criticality.val",CRITICALITY_VAL));
        List<ArtifactBase> artifacts=repositoryClient.search(
                "businessServiceArtifact", searchCriteria, "name", 0, 0);

        System.out.println("Search by criticality returned the following artifacts:");
        for (ArtifactBase a : artifacts) {
            System.out.println("  "+a.getName()+"("+a.get_uuid()+")");	
        }

        // delete artifact
        repositoryClient.deleteArtifact(businessService.get_uuid().toString());	
        System.out.println("Artifact deleted.");
        // purge artifact
        repositoryClient.purgeArtifact(businessService.get_uuid().toString());	
        System.out.println("Artifact purged.");


    }
    
    /**
     * Shows how to create, search, update, delete, purge artifact.
     * It also shows how to establish relation between artifacts.
     */
    private static void createGetUpdateDelete() {
        System.out.println("\n=== createGetUpdateDelete() ===");
        // create web service artifact
        ArtifactBase webService = repositoryClient.getArtifactFactory()
        .newArtifact("webServiceArtifact");
        webService.setName("Demo Webservice Name");
        // store it on the server
        webService = repositoryClient.createArtifact(webService);
        System.out.println("Web service artifact created, its UUID is: "+webService.get_uuid().toString());

        // create service artifact		
        ArtifactBase businessService = repositoryClient.getArtifactFactory()
        .newArtifact("businessServiceArtifact");
        businessService.setName("Demo Service Name");
        Category propertyCriticality = new Category(
                CRITICALITY_TAX_URI, "High",
                CRITICALITY_VAL);
        businessService.setProperty("criticality", propertyCriticality);
        // add relation from service to webService
        List<Uuid> services = new ArrayList<Uuid>();
        services.add(webService.get_uuid());
        businessService.setMultiRelationProperty("service", services);
        
        // store service on server
        businessService = repositoryClient.createArtifact(businessService);
        System.out.println("Service artifact created, its UUID is: "+businessService.get_uuid().toString());

        // get business service from server
        businessService = repositoryClient.getArtifact(businessService
                .get_uuid().toString());	
        System.out.println("The service re-fetched from server by its uuid.");
        // list related services
        List<Relation> multiRelationProperty = businessService.getMultiRelationProperty("service");		
        for (Relation relation : multiRelationProperty) {
            System.out.println("The service has relation to web service: "+relation.getTargetId().toString());
        }

        // update consumable property
        Boolean consumable = businessService.getBooleanProperty("readyForConsumption"); 
        System.out.println("The service is "+
                (consumable!=null&&consumable?"consumable":"not consumable")+".");
        businessService.setBooleanProperty("readyForConsumption", Boolean.TRUE);
        repositoryClient.updateArtifact(businessService);
        System.out.println("The service artifact updated to be ready for consumption.");
        // get business service from server
        businessService = repositoryClient.getArtifact(businessService
                .get_uuid().toString());	
        consumable = businessService.isPropertyInitialized("readyForConsumption") && businessService.getBooleanProperty("readyForConsumption"); 
        System.out.println("The service is "+
                (consumable!=null&&consumable?"consumable":"not consumable")+".");

        // delete artifacts
        repositoryClient.deleteArtifact(businessService.get_uuid().toString());		
        repositoryClient.deleteArtifact(webService.get_uuid().toString());
        System.out.println("Artifacts deleted.");

        // purge artifacts
        repositoryClient.purgeArtifact(businessService.get_uuid().toString());		
        repositoryClient.purgeArtifact(webService.get_uuid().toString());
        System.out.println("Artifacts purged.");
    }

    /**
     * Shows how to work with artifact security (ownership and ACL).
     */
    private static void demoArtifactSecurity(){
        System.out.println("\n=== demoArtifactSecurity ===");

        // create artifact 
        ArtifactBase art = repositoryClient.getArtifactFactory().newArtifact("hpsoaProjectArtifact");
        art.setName("Demo Artifact Security");
        art = repositoryClient.createArtifact(art);
        String originalOwner = art.get_owner();
        System.out.println("Artifact created.");
        String artUuid = art.get_uuid().toString(); 

        System.out.println("--- get artifact security ---");
        ArtifactSecurity artSec = repositoryClient.getArtifactSecurity(artUuid);
        System.out.println(artSec);
        
        System.out.println("--- changing owner to Provider role ---");
        Principal origOwner = artSec.getOwner();
        artSec = new ArtifactSecurity(artSec.getAccessControlList());
        artSec.setOwner(new Role("Provider"));
        artSec = repositoryClient.setArtifactSecurity(artUuid,artSec);
        System.out.println(artSec);

        System.out.println("--- changing back to original owner ---");
        artSec = new ArtifactSecurity(artSec.getAccessControlList());
        artSec.setOwner(origOwner);
        artSec = repositoryClient.setArtifactSecurity(artUuid,artSec);
        System.out.println(artSec);
        
        System.out.println("--- sharing artifact ---");
        artSec = new ArtifactSecurity(artSec.getAccessControlList());
        artSec.addRight(new Role("Shared"),Right.READ);
        artSec = repositoryClient.setArtifactSecurity(artUuid,artSec);
        System.out.println(artSec);

        System.out.println("--- unsharing artifact ---");
        artSec = new ArtifactSecurity(artSec.getAccessControlList());
        artSec.removeRight(new Role("Shared"),Right.READ);
        artSec = repositoryClient.setArtifactSecurity(artUuid,artSec);
        System.out.println(artSec);

        // purge the artifact
        repositoryClient.deleteArtifact(artUuid);
        System.out.println("Artifact deleted.");
        repositoryClient.purgeArtifact(artUuid);
        System.out.println("Artifact purged.");
    }


    private static void printArtifact(ArtifactBase a){
          System.out.println("############# " + a.getName() + "(" + a.get_uuid() + ") ######################");
          System.out.println("\t" + a.getArtifactDescriptor().getSdmName());
          System.out.println("\t" + a.getDescription());
          System.out.println("###############################################################################");
    }


    private static void myArtifactTest() {

//        Map<String, List<Pair<String, String>>> target = new HashMap<>();
//        Map<String, List<Pair<String, String>>> source = new HashMap<>();


        //List<ArtifactBase> artifacts=repositoryClient.search(null, null, null ,0,0);
        ArtifactBase a = repositoryClient.getArtifact("be247a9a-d614-4ddc-9c26-a514b1d52042");
        //System.out.println("Search returned the following artifacts:");
        //List<Pair<String, String>> searchCriteria=new ArrayList<Pair<String,String>>();
        //searchCriteria.add(new Pair<String, String>("_path","*"));
        //List<ArtifactBase> artifacts=repositoryClient.search(null, null, null ,0,10000);
        //for (ArtifactBase au : artifacts) {
        //  ArtifactBase a = repositoryClient.getArtifact(au.get_uuid().toString());
//            System.out.println("############# " + a.getName() + "(" + a.get_uuid() + ") ######################");
//            System.out.println("\t" + a.getArtifactDescriptor().getSdmName());
//            System.out.println("\t" + a.getDescription());
//            System.out.println("\tProperty descriptor:");
//            for(PropertyDescriptor pd : a.getArtifactDescriptor().enumerateCategories()) {
//                System.out.println("\t\t" + pd.getTaxonomyUri() + "\t" + pd.getSdmName());
//            }
//            for(Relation r : a.getRelations()){
//                if(r.isOutgoing()) {
//                    List<Pair<String, String>> po = null;
//                    if((po = target.get(a.get_uuid().toString())) == null) {
//                        po = new ArrayList<>();
//                        target.put(a.get_uuid().toString(), po);
//                    }
//                    po.add(new Pair<String, String>(r.getName(), r.getTargetId().toString()));
//                }
//                if(r.isIncoming()) {
//                    source.put(a.get_uuid().toString(),new Pair<String, String>(r.getName(), r.getSourceId().toString()));
//                }
//            }
//
            enumerateArtifactProperties(a.get_artifactSdmName());
        System.out.println("==========================================================");
        for(PropertyDescriptor pd : a.getArtifactDescriptor().enumerateProperties()){
            if (!pd.isRelationship()) {
                System.out.println(pd.getSdmName() + "\t" + pd.getPropertyTypeDescriptor().getPropertyTypeClass().getSimpleName());
                if(pd.getPropertyCardinality().isMultiple()){
                    for(SinglePropertyValue sp : a.getMultiProperty(pd.getSdmName())){
                        System.out.println("\t" + sp.getValue());
                    }
                } else {
                   System.out.println("\t" +  a.getProperty(pd.getSdmName()));
                }
            }
        }
        System.out.println("========================================");
            a.foreachProperty(PropertyFilters.createFilter(a.getArtifactDescriptor(), ArtifactPartSelector.NO_RELATIONS),
                    new com.hp.systinet.lang.ExecutableParametrized<java.lang.Void,com.hp.systinet.repository.sdm.properties.SinglePropertyValue> () {
                @Override
                public Void execute(SinglePropertyValue sv) {
                    System.out.println(sv.getValue());
                    return null;
                }
            });
//            System.out.println("==========================================================");
//        }
//        for(String uid : target.keySet()) {
//            System.out.println(uid);
//            for(Pair<String, String> p : target.get(uid)){
//                System.out.println("\t" + p.getFirst() + "\t" + p.getSecond());
//            };
//        }

    }


    /**
     * Shows how to search by artifact's path, which was an artifact's identifier 
     * in Systinet 3.x. repository API. This (4.x) repository API identifies artifacts 
     * using UUID.
     */
    public static void searchByPath() {
        System.out.println("\n=== searchByPath() ===");
        List<Pair<String, String>> searchCriteria=new ArrayList<Pair<String,String>>();
        searchCriteria.add(new Pair<String, String>("_path","/taxonomies/uddi-com-systinet-repository-sdm-taxonomies-lifecycleStages"));
        // searchCriteria.add(new Pair<String, String>("_path","*/uddi-com-systinet-repository-sdm-taxonomies-lifecycleStages"));
        List<ArtifactBase> artifacts=repositoryClient.search(
                "taxonomyArtifact", searchCriteria, "name", 0, 0);

        System.out.println("Search returned the following artifacts:");
        for (ArtifactBase a : artifacts) {
            System.out.println("  "+a.getName()+"("+a.get_uuid()+")");	
        }
    }
    /**
     * Shows how to perform full text search. Full text search works properly 
     * only if it is enabled at the server side (see Installation Guide).
     */
    public static void fullTextSearch() {
        System.out.println("\n=== fullTextSearch() ===");
        // search "lifecycle" in all artifact types (null) 
        // order by relevance DESC, name ASC ... return first page  
        List<ArtifactBase> artifacts=repositoryClient.fullTextSearch(
                "lifecycle",null, "relevance-,name", 0, 0);

        System.out.println("Full text search returned the following artifacts:");
        for (ArtifactBase a : artifacts) {
            System.out.println("  "+a.getName()+"("+a.get_artifactSdmName()+")");	
        }
        // search "lifecycle" in taxonomy artifacts 
        // with default ordering ... return first page  
        artifacts=repositoryClient.fullTextSearch(
                "lifecycle","taxonomyArtifact", null, 0, 0);
        System.out.println("Full text search (in taxonomyArtifacts) returned the following artifacts:");
        for (ArtifactBase a : artifacts) {
            System.out.println("  "+a.getName()+"("+a.get_artifactSdmName()+")");	
        }
    }

}
