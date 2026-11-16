// SPDX-FileCopyrightText: 2022 Agency for Digital Government
//
// SPDX-License-Identifier: EUPL-1.2

package se.ams.dcatprocessor.rdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.collections4.MultiValuedMap;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.GEO;
import org.eclipse.rdf4j.model.vocabulary.LOCN;
import org.eclipse.rdf4j.model.vocabulary.ORG;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.VCARD4;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BufferedGroupingRDFHandler;
import org.eclipse.rdf4j.rio.rdfxml.util.RDFXMLPrettyWriter;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import se.ams.dcatprocessor.models.Catalog;
import se.ams.dcatprocessor.models.DataClass;
import se.ams.dcatprocessor.models.DataService;
import se.ams.dcatprocessor.models.DataSet;
import se.ams.dcatprocessor.models.DatasetSeries;
import se.ams.dcatprocessor.models.Distribution;
import se.ams.dcatprocessor.models.FileStorage;
import se.ams.dcatprocessor.models.Organization;
import se.ams.dcatprocessor.rdf.namespace.ADMS;
import se.ams.dcatprocessor.rdf.namespace.ODRS;
import se.ams.dcatprocessor.rdf.namespace.SCHEMA;
import se.ams.dcatprocessor.rdf.namespace.SPDX;
import se.ams.dcatprocessor.rdf.validate.CardinalityValidator;
import se.ams.dcatprocessor.rdf.validate.MultipleURIValidator;
import se.ams.dcatprocessor.rdf.validate.SingleInputValidator;
import se.ams.dcatprocessor.rdf.validate.ValidationError;
import se.ams.dcatprocessor.specification.DcatSpecification;
import se.ams.dcatprocessor.util.Util;


/**
 * @see <a href="https://docs.dataportal.se/dcat/sv/#intro">https://docs.dataportal.se/dcat/sv/#intro</a>
 * @author nacbr
 *
 */

@Component
@Scope("prototype")
public class RDFWorker {
	private Model model;

	/**
	 * Holds the file that is presently being processed
	 */
	private String currentFileName;

	private final DcatSpecification specification;

	private final SingleInputValidator singleInputValidator;
	private final CardinalityValidator cardinalityValidator;
	private final MultipleURIValidator multipleURIValidator;

	public List<ValidationError> validationErrors = new ArrayList<>();
	
	public RDFWorker(DcatSpecification specification, CardinalityHandler cardinalityHandler) {
		this.specification = specification;
		this.multipleURIValidator = new MultipleURIValidator();
		this.singleInputValidator = new SingleInputValidator(specification);
		this.cardinalityValidator = new CardinalityValidator(cardinalityHandler);
	}

	/*
	 * Predefined errormessage
	 */
	private final String UNABLE_CREATE_MISSING_VALUES = this.getClass() + " : Unable to create DCAT. Reason: X is a mandatory but is missing";
	private final String INVALID_URI = this.getClass() + " : Unable to create DCAT. Reason: X is not a valid URI";

	/**
	 * Special delimiter for language-prefixed strings since it's not commonly used
	 */
	private final String SUN = "¤";

	/**
	 * 
	 * Creates a RDF-file that according to DCAT-AP-SE format based on the data in Catalog and the list of
	 * FileStorage objects. The elements created from the data in the FileStorage objects are appended to
	 * the Catalog
	 * 
	 * @see <a href="https://docs.dataportal.se/dcat/sv/#intro">https://docs.dataportal.se/dcat/sv/#intro</a>
	 * 
	 * @param catalog - Contains the data for Catalog
	 * @param fileStorages - 	The list of FileStorage objects where each FileStorage object contains 
	 * 							data from one API-specification file
	 * @return The resulting RDF-file as an XML-string
	 * @throws DcatException - If there is an error processing data or creating the file
	 * @throws IOException - If there is an error processing data or creating the file
	 */
	public String createDcatFile(Catalog catalog, List<FileStorage> fileStorages) throws DcatException, IOException {

		createModel();
		
		/**
		 * The Catalog data does not come from a file with a name we can access 
		 * so we use a generic name as an identifier for possible errors in this input data
		 */
		singleInputValidator.setCurrentFileName(catalog.fileName);
		cardinalityValidator.setCurrentFileName(catalog.fileName);
		multipleURIValidator.setCurrentFileName(catalog.fileName);

		/**
		 * Returnparmeter from createCatalog is a list with IRI where
		 * IRI[0] = Catalog
		 * IRI[1] = Agent
		 */
		IRI[] catalogAndAgent = createCatalog(catalog);
		
		model.add(catalogAndAgent[0], RDF.TYPE, DCAT.CATALOG);
		
		model.add(catalogAndAgent[0], DCTERMS.PUBLISHER, catalogAndAgent[1]);
		
		for (FileStorage fileStorage : fileStorages) {
			
			currentFileName = fileStorage.fileName;
			
			/**
			 * Save validation errors under the correct filename
			 */
			singleInputValidator.setCurrentFileName(currentFileName);
			cardinalityValidator.setCurrentFileName(currentFileName);
			multipleURIValidator.setCurrentFileName(currentFileName);
			
			/*
			 * Create DataSets and add them to the model and add the reference from Catalog
			 * to DataSet
			 */
			
			Util.checkNotNull(fileStorage.dcat_dataset, UNABLE_CREATE_MISSING_VALUES.replaceAll("X", "Catalog.dcat_dataset"));
			
			for (DataSet dataSet : fileStorage.dcat_dataset) {	
				IRI iriDataSet = createDataset(dataSet, catalogAndAgent[1]);
				model.add(catalogAndAgent[0], DCAT.HAS_DATASET, iriDataSet);
			}
			
			
			/*
			 * Create DataService and add them to the model and then 
			 * add the reference from Catalog to DataServices
			 */
			for (DataService dataService : fileStorage.dataService) {
				IRI dataServiceIRI = createDataService(dataService);
				model.add(catalogAndAgent[0], DCAT.HAS_SERVICE, dataServiceIRI);
			}
			
			/*
			 * Create DatasetSeries and add them to the model
			 */
			for (DatasetSeries datasetSeries : fileStorage.dcat_datasetSeries) {
    			IRI iriDatasetSeries = createDatasetSeries(datasetSeries, catalogAndAgent[1]);
    			model.add(catalogAndAgent[0], DCAT.HAS_DATASET, iriDatasetSeries);
			}
		}
		
		multipleURIValidator.validate();

		validationErrors.addAll(singleInputValidator.getValidationErrors());
		validationErrors.addAll(cardinalityValidator.getValidationErrors());
		validationErrors.addAll(multipleURIValidator.getValidationErrors());
	
		if(!validationErrors.isEmpty()) {
			DcatException dcatException = new DcatException("Error creating DCAT-AP-SE due to validationerrors. See enclosed list");
			dcatException.setValidationResults(validationErrors);
			throw dcatException;
		} else {
			return printModel(model);
		}
		
	}
	
	private void createModel() {
		model = new LinkedHashModel();
		//Add namespaces
		model.setNamespace(DCAT.NS);
		model.setNamespace(DCTERMS.NS);
		model.setNamespace(FOAF.NS);
		model.setNamespace(VCARD4.NS);
		model.setNamespace(RDF.NS);
		model.setNamespace(XSD.NS);
		model.setNamespace(LOCN.NS);
		model.setNamespace(SCHEMA.NS);
		model.setNamespace(PROV.NS);
		model.setNamespace(ADMS.NS);
		model.setNamespace(ODRS.NS);
		model.setNamespace(SPDX.NS);
		model.setNamespace(OWL.NS);
		model.setNamespace(ORG.NS);
		model.setNamespace(new SimpleNamespace("dcatap", "http://data.europa.eu/r5r/"));
	}

	/**
	 * Generates a RDF/XML string from the model
	 * 
	 * @return The generated string
	 */
	private String printModel(Model model) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		Rio.write(model, new BufferedGroupingRDFHandler((RDFHandler) new RDFXMLPrettyWriter(bos)));
		// Rio.write(model, new RDFXMLPrettyWriter(bos)); //Not so pretty output with this printer
		return bos.toString(Charset.forName("UTF-8"));
	}

	/**
	 * Creates a catalog from data contained in the catalog object
	 * @see <a href="http://www.w3.org/ns/dcat#Catalog">Catalog</a>
	 * @param catalog - Containing the data
	 * @return IRI[] - A list of IRI with two objects [0] Catalog(IRI) [1] Agent(IRI)
	 * @throws DcatException - If there is an error during processing
	 * @throws IOException - If there is an IOException
	 */
	private IRI[] createCatalog(Catalog catalog) throws DcatException, IOException{
		String path = DCAT.CATALOG.getLocalName();
		IRI[] catalogAndAgent = new IRI[2];
		
		Util.checkNotNull(catalog, UNABLE_CREATE_MISSING_VALUES.replaceAll("X", "Catalog"));
		
		/**
		 * Special check that subject exist and is valid URI because this is a stopping error
		 */
		checkSubject("Catalog.about", catalog.about); 
		
		Util.checkNotNull(catalog.publisher, UNABLE_CREATE_MISSING_VALUES.replaceAll("X", "Catalog.publisher"));
		
		/**
		 * Check that the keys for inputvalues:
		 * 1. Are defined in the specification
		 * 2. Occur within the range allowed in the specification
		 */
		List<String> checkedElsewhere = List.of("dcat:dataset", "dcterms:publisher"); //Items that will not be checked now
		cardinalityValidator.validate(DcatClass.CATALOG, catalog.dcData, checkedElsewhere, path);
		IRI catalogIRI = createIri(catalog.about);
		
		catalogAndAgent[0] = catalogIRI;
		
		IRI agentIRI = createAgent(catalog.publisher, path);
		catalogAndAgent[1] = agentIRI;
		
		addToModel(model, catalogIRI, catalog.dcData, path);
				
		/*
		 * Add the Rights...if it exist
		 */
		addRights(catalogIRI, catalog.rights, path);
		
		/*
		 * Add locations as anonymous nodes
		 */
		addNodes(catalogIRI, DCTERMS.SPATIAL, DCTERMS.LOCATION, catalog.spatial, path);
		
		return catalogAndAgent;
	}
	
	/**
	 * Creates a Agent with all its data @see <a href="http://xmlns.com/foaf/0.1/Agent">Agent</a>
	 * @param agent - The object containing the data for Agent
	 * @param parentPath - Path of the parent node
	 * @return - The Agent
	 */
	private IRI createAgent(DataClass agent, String parentPath) {
		String path = path(parentPath, FOAF.AGENT);
		/**
		 * Special check that subject exist and is valid URI because this is a stopping error
		 */
		checkSubject("Agent.about", agent.about); 
		
		/**
		 * Check that the keys for inputvalues:
		 * 1. Are defined in the specification
		 * 2. Occur within the range allowed in the specification
		 */
		cardinalityValidator.validate(DcatClass.AGENT, agent.dcData, List.of(), path);

		IRI agentIRI = SimpleValueFactory.getInstance().createIRI(agent.about);

		model.add(agentIRI, RDF.TYPE, FOAF.AGENT);

		addToModel(model, agentIRI, agent.dcData, path);

		return agentIRI;
	}

	/**
	 * Creates a Dataset with all its data and creates a link from
	 * the dataset to the Publisher
	 * @see <a href="http://www.w3.org/ns/dcat#Dataset">Dataset</a>
	 * @param dataSet The object containing the data for Dataset
	 * @param agentIRI - The Agent to link to
	 * @return - The Dataset
	 */
	private IRI createDataset(DataSet dataSet, IRI agentIRI) throws IOException {
		String path = DCAT.DATASET.getLocalName();
		/**
		 * Special check that subject exist and is valid URI because this is a stopping error
		 */
		checkSubject("DataSet.about", dataSet.about);
				
		/**
		 * Check that the keys for inputvalues:
		 * 1. Are defined in the specification
		 * 2. Occur within the range allowed in the specification
		 */
		List<String> checkedElsewhere = List.of("dcterms:publisher", "dcterms:creator"); //Items that will not be checked now
		cardinalityValidator.validate(DcatClass.DATASET, dataSet.dcData, checkedElsewhere, path);
		
		IRI dataSetIRI =  createIri(dataSet.about);
		model.add(dataSetIRI, RDF.TYPE, DCAT.DATASET);
		
		//Add all values to dataset
		addToModel(model, dataSetIRI, dataSet.dcData, path);

		//Add the reference to the external element publisher
		model.add(dataSetIRI, DCTERMS.PUBLISHER, agentIRI);

		/*
		 * Add creator
		 */
		if ((dataSet.creator != null) && !dataSet.creator.equals(null)) {
			IRI CreatorIRI = createAgent(dataSet.creator, path);
			model.add(dataSetIRI, DCTERMS.CREATOR, CreatorIRI);
		}

		
		/*
		 * Add locations as anonymous nodes
		 */
		addNodes(dataSetIRI, DCTERMS.SPATIAL, DCTERMS.LOCATION, dataSet.spatial, path);
		
		/*
		 * Add temporals as anonymous nodes
		 */
		addNodes(dataSetIRI, DCTERMS.TEMPORAL, DCTERMS.PERIOD_OF_TIME, dataSet.temporals, path);
		
		/*
		 * Add offers as anonymous nodes
		 */
		addNodes(dataSetIRI, SCHEMA.OFFERS, SCHEMA.OFFER, dataSet.offers, path);
	
		/*
		 * Add the Uppfyller...conformsTo as anonymous nodes
		 */
		addNodes(dataSetIRI, DCTERMS.CONFORMS_TO, DCTERMS.STANDARD, dataSet.conformsTo, path);
		
		/*
		 * Add the Documentation...foaf:page as anonymous nodes
		 */
		addNodes(dataSetIRI, FOAF.PAGE, FOAF.DOCUMENT, dataSet.documents, path);
		
		/*
		 * Add the Relations...dcat:qualifiedRelation as anonymous nodes
		 */
		addNodes(dataSetIRI, DCAT.QUALIFIED_RELATION, DCAT.RELATIONSHIP, dataSet.qualifiedRelations, path);
		
		/*
		 * Add Provenance...dcterms:provenance as anonymous nodes
		 */
		addNodes(dataSetIRI, DCTERMS.PROVENANCE_STATEMENT, DCTERMS.PROVENANCE, dataSet.provenances, path);
		
		/*
		 * Add the Övrig aktör ...prov:qualifiedAttribution as anonymous nodes
		 * Different from other anonymous nodes since it has an Agent(complex type)
		 */
		for (DataClass dataClass : dataSet.otherAgents) {
			List<Resource> anonymousNodesList = addNodes(dataSetIRI, PROV.QUALIFIED_ATTRIBUTION, PROV.ATTRIBUTION, List.of(dataClass), path);
			
			IRI tmpAgentIRI = createAgent(dataClass.agent, path(path, PROV.ATTRIBUTION));
			
			model.add(anonymousNodesList.get(0), PROV.AGENT_PROP, tmpAgentIRI);
		}
		
		
		/*
		 * Create ContactPoints and add them to the model and then 
		 * add the reference from DataSet to ContactPoints
		 */
		for (Organization org : dataSet.organizations) {
			IRI organizationIRI = createOrganization(org, path);
			model.add(dataSetIRI, DCAT.CONTACT_POINT, organizationIRI);
		}

		/*
		 * Create Distributions and add them to the model and then 
		 * add the reference from DataSet to Distributions
		 */
		for (Distribution distribution : dataSet.dcat_distribution) {
			IRI distributionIRI = createDistribution(distribution, path);
			model.add(dataSetIRI, DCAT.HAS_DISTRIBUTION, distributionIRI);
		}

		return dataSetIRI;
	}
	
	private void addRights(@NonNull IRI parentIRI, @NonNull DataClass rights, String parentPath) {
		
		if(Util.isNullOrEmpty(rights)) {
			return;
		}

		String rightsPath = path(parentPath, ODRS.RIGHTS_STATEMENT);
		
		/**
		 * Create the rights node as anonymous nodes with reference to parent node
		 */
		List<Resource> rightsNodesList = addNodes(parentIRI, DCTERMS.RIGHTS, ODRS.RIGHTS_STATEMENT, List.of(rights), parentPath);
		
		/**
		 * Create the license documents as anonymous nodes with reference to the Rights node
		 */
		addNodes(rightsNodesList.get(0), ODRS.COPYRIGHT_STATEMENT, DCTERMS.LICENSE_DOCUMENT, rights.licenseDocuments, rightsPath);
		
		/*
		 * Create Agents(publishers) and add them to the model and 
		 * then add the reference from DataService to Agents(publishers) 
		 */
		for (DataClass agent : rights.agents) {
			IRI agentIRI = createAgent(agent, rightsPath);
			model.add(rightsNodesList.get(0), ODRS.COPYRIGHT_HOLDER, agentIRI);
		}

	}
			
	/**
	 * Creates a Distribution with all its data
	 * @see <a href="http://www.w3.org/ns/dcat#Distribution">Distribution</a>
	 * @param distribution The object containing the data for Distribution
	 * @param parentPath - Path of the parent node
	 * @return The Distribution
	 */
	private IRI createDistribution(Distribution distribution, String parentPath) throws IOException {
		String path = path(parentPath, DCAT.DISTRIBUTION);
		/**
		 * Special check that subject exist and is valid URI because this is a stopping error
		 */
		checkSubject("Distribution.about", distribution.about);

		/**
		 * Check that the keys for inputvalues:
		 * 1. Are defined in the specification
		 * 2. Occur within the range allowed in the specification
		 */
		List<String> checkedElsewhere = List.of("dcat:accessService"); //Items that will not be checked now
		cardinalityValidator.validate(DcatClass.DISTRIBUTION, distribution.dcData, checkedElsewhere, path);
		
		IRI distributionIRI = createIri(distribution.about);
		model.add(distributionIRI, RDF.TYPE, DCAT.DISTRIBUTION);
		
		//Add all values to Organization
		addToModel(model, distributionIRI, distribution.dcData, path);
		
		/*
		 * Add the Uppfyller...conformsTo as anonymous nodes
		 */
		addNodes(distributionIRI, DCTERMS.CONFORMS_TO, DCTERMS.STANDARD, distribution.conformsTo, path);

		/*
		 * Add the licensedocuments as anonymous nodes
		 */
		addNodes(distributionIRI, DCTERMS.LICENSE, DCTERMS.LICENSE_DOCUMENT, distribution.licenseDocuments, path);
			
		/*
		 * Add the Documentation...foaf:page as anonymous nodes
		 */
		addNodes(distributionIRI, FOAF.PAGE, FOAF.DOCUMENT, distribution.documents, path);
		
		/*
		 * Add the Rights...if it exist
		 */
		addRights(distributionIRI, distribution.rights, path);
		
		/*
		 * Add CheckSum...if it exist
		 */
		if(Util.isNotNullOrEmpty(distribution.checksum)) {
			addNodes(distributionIRI, SPDX.CHECKSUMS, SPDX.CHECKSUM, List.of(distribution.checksum), path);
		}
		
		/*
		 * Create DataServices and add them to the model and
		 * then add the reference from Distribution to DataServices
		 */
		for (DataService dataService : distribution.dataServices) {
			IRI dataServiceIRI = createDataService(dataService);
			model.add(distributionIRI, DCAT.HAS_SERVICE, dataServiceIRI);
		}
				
		return distributionIRI;
	}
	
	/**
	 * Creates a Dataservice with all its data
	 * @see <a href="http://www.w3.org/ns/dcat#DataService">DataService</a>
	 * @param dataService - The object containing the data for dataService
	 * @return DataService - The Dataservice
	 */
	private IRI createDataService(DataService dataService) throws IOException {
		String path = DCAT.DATA_SERVICE.getLocalName();
		/**
		 * Special check that subject exist and is valid URI because this is a stopping error
		 */		
		checkSubject("Dataservice.about", dataService.about);
				
		/**
		 * Check that the keys for inputvalues:
		 * 1. Are defined in the specification
		 * 2. Occur within the range allowed in the specification
		 */
		List<String> checkedElsewhere = List.of("dcterms:publisher", "dcat:contactPoint"); //Items that will not be checked now
		cardinalityValidator.validate(DcatClass.DATASERVICE, dataService.dcData, checkedElsewhere, path);
		
		IRI dataServiceIRI = createIri(dataService.about);
		model.add(dataServiceIRI, RDF.TYPE, DCAT.DATA_SERVICE);
		
		//Add all simple values to Dataservice
		addToModel(model, dataServiceIRI, dataService.dcData, path);
		
		/*
		 * Add the Uppfyller...conformsTo as anonymous nodes
		 */
		addNodes(dataServiceIRI, DCTERMS.CONFORMS_TO, DCTERMS.STANDARD, dataService.conformsTo, path);
		
		/*
		 * Add the licensedocuments as anonymous nodes
		 */
		addNodes(dataServiceIRI, DCTERMS.LICENSE, DCTERMS.LICENSE_DOCUMENT, dataService.licenseDocuments, path);
		
		/*
		 * Add the Documentation...foaf:page as anonymous nodes
		 */
		addNodes(dataServiceIRI, FOAF.PAGE, FOAF.DOCUMENT, dataService.documents, path);
		
		/*
		 * Create Agents(publishers) and add them to the model and 
		 * then add the reference from DataService to Agents(publishers) 
		 */
		for (DataClass agent : dataService.agents) {
			IRI agentIRI = createAgent(agent, path);
			model.add(dataServiceIRI, DCTERMS.PUBLISHER, agentIRI);
		}

		/*
		 * Create Organizations and add them to the model and 
		 * then add the reference from DataService to Organizations 
		 */
		for (Organization organization : dataService.organizations) {
			IRI organizationIRI = createOrganization(organization, path);
			model.add(dataServiceIRI, DCAT.CONTACT_POINT, organizationIRI);
		}
		
		return dataServiceIRI;
	}

	/**
	 * Creates a DatasetSeries with all its data
	 * @see <a href="https://www.w3.org/ns/dcat#DatasetSeries">DataService</a>
	 * @param datasetSeries - The object containing the data for datasetSeries
	 * @return DatasetSeries - The DatasetSeries
	 */
	private IRI createDatasetSeries(DatasetSeries datasetSeries, IRI agentIRI) throws IOException {
		String path = DCAT.DATASET_SERIES.getLocalName();
		/**
		 * Special check that subject exist and is valid URI because this is a stopping error
		 */
		checkSubject("DatasetSeries.about", datasetSeries.about);

    	List<String> checkedElsewhere = List.of("dcterms:publisher"); //Items that will not be checked now
		cardinalityValidator.validate(DcatClass.DATASETSERIES, datasetSeries.dcData, checkedElsewhere, path);

		IRI datasetSeriesIRI = createIri(datasetSeries.about);
		model.add(datasetSeriesIRI, RDF.TYPE, DCAT.DATASET_SERIES);

		addToModel(model, datasetSeriesIRI, datasetSeries.dcData, path);

		//Add the reference to the external element publisher (publisher from Catalog)
		model.add(datasetSeriesIRI, DCTERMS.PUBLISHER, agentIRI);

		addNodes(datasetSeriesIRI, DCTERMS.SPATIAL, DCTERMS.LOCATION, datasetSeries.spatial, path);
		addNodes(datasetSeriesIRI, DCTERMS.TEMPORAL, DCTERMS.PERIOD_OF_TIME, datasetSeries.temporals, path);
		addNodes(datasetSeriesIRI, DCTERMS.CONFORMS_TO, DCTERMS.STANDARD, datasetSeries.conformsTo, path);
		addNodes(datasetSeriesIRI, FOAF.PAGE, FOAF.DOCUMENT, datasetSeries.documents, path);
		addNodes(datasetSeriesIRI, DCAT.QUALIFIED_RELATION, DCAT.RELATIONSHIP, datasetSeries.qualifiedRelations, path);

		for (Organization org : datasetSeries.organizations) {
			IRI organizationIRI = createOrganization(org, path);
			model.add(datasetSeriesIRI, DCAT.CONTACT_POINT, organizationIRI);
		}

		return datasetSeriesIRI;
	}
	
	/**
	 * Creates a ContactPoint with all its data
	 * @see <a href="http://www.w3.org/ns/dcat#Organization">Organization</a>
	 * @param org - The object containing the data for Organization
	 * @param parentPath - Path of the parent node
	 * @return - The Organization
	 */
	private IRI createOrganization(Organization org, String parentPath) throws IOException {
		String path = path(parentPath, VCARD4.ORGANIZATION);
		/**
		 * Special check that subject exist and is valid URI because this is a stopping error
		 */
		checkSubject("ContactPoint.about", org.about);
		
		/**
		 * Check that the keys for inputvalues:
		 * 1. Are defined in the specification
		 * 2. Occur within the range allowed in the specification
		 */
		List<String> checkedElsewhere = List.of("vcard:hasTelephone", "vcard:hasAddress"); //Items that will not be checked now
		cardinalityValidator.validate(DcatClass.ORGANISATION, org.dcData, checkedElsewhere, path);
				
		IRI organizationIRI = createIri(org.about);
		model.add(organizationIRI, RDF.TYPE, VCARD4.ORGANIZATION);

		//Add all simple values to Organization
		addToModel(model, organizationIRI, org.dcData, path);

		/*
		 * Add addresses as anonymous nodes
		 */
		addNodes(organizationIRI, VCARD4.HAS_ADDRESS, VCARD4.ADDRESS, org.adress, path);
		
		/*
		 * Add phones as anonymous nodes
		 */
		addNodes(organizationIRI, VCARD4.HAS_TELEPHONE, VCARD4.VOICE, org.phone, path);
		
		return organizationIRI;
	}
	
	
	/**
	 * Creates a node with an anonymous link or an URI depending on if an URI-link is submitted
	 * @param parentIRI - Reference to parent node
	 * @param parentIRINodeTypeRef - Parent node reference type
	 * @param nodeType - Type of node
	 * @param dataClasses - Placeholder for data to be added to this node
	 * @param parentPath - Path of the parent node
	 * @return The list of created BNodes
	 */
	private List<Resource> addNodes(@NonNull Resource parentIRI, @NonNull IRI parentIRINodeTypeRef, @NonNull IRI nodeType, @NonNull List<DataClass> dataClasses, String parentPath) {
		List<Resource> resources = new ArrayList<>();
		String nodePath = path(parentPath, nodeType);
		
		ValueFactory valueFactory = SimpleValueFactory.getInstance();
		
		for (DataClass dataClass : dataClasses) {
			Resource resource;
			if(Util.isNullOrEmpty(dataClass.about)) {
				resource = valueFactory.createBNode();			//Create an anonymous node	
			} else {
				/**
				 * Special check that subject exist and is valid URI because this is a stopping error
				 */
				checkSubject(parentIRINodeTypeRef.getLocalName(), dataClass.about);
				resource = createIri(dataClass.about);			//Otherwise create a node with an URI as reference
			}
			
			model.add(parentIRI, parentIRINodeTypeRef, resource);	//Add reference from parent IRI
			model.add(resource, RDF.TYPE, nodeType);
			addDataToNode(resource, dataClass.dcData, nodePath);	//Add data to node
			
			resources.add(resource);
		}
		
		return resources;
	}
	
	/**
	 * Adds the values in valueMap to the node
	 * @param resource - The node to add value to
	 * @param valueMap - Containing the data to be added
	 * @param path - Path to the node, e.g. Dataset.PeriodOfTime
	 */
	private void addDataToNode(Resource resource, MultiValuedMap<String, String> valueMap, String path) {
		
		ValueFactory valueFactory = SimpleValueFactory.getInstance();
		
		Set<String> keySet = valueMap.keySet();
		
		for (String key : keySet) {
		
			IRI iri = VocabularyStringToIRI.getIRI(key);
			
			if (Util.isNotNullOrEmpty(iri)) {
			
				Collection<String> values = valueMap.get(key);
				
				for (String value : values) {
					
					singleInputValidator.validateData(key, value, path);
					
					if (isWKTLiteral(key)) {
					    model.add(resource, iri, valueFactory.createLiteral(value, GEO.WKT_LITERAL));
					} else if (Util.isURI(value)) { // Check if its an URI and create a resource
						model.add(resource, iri, valueFactory.createIRI(value));
					} else if (isLanguageValue(value)) { // Check if it's a language specific string, Ex en:Some text in english
						String[] split = value.split(SUN);
						model.add(resource, iri, valueFactory.createLiteral(split[1], split[0]));
					} else {
						Literal dateLiteral = getDateValue(key, value);
						if (dateLiteral != null) { 					//Datevalue
							model.add(resource, iri, dateLiteral);
						} else if(isPhoneValue(key)) {	//Phone value...needs to handled separately from a URI
							model.add(resource, iri, valueFactory.createIRI(value));
						} else {// All other values....for now
							model.add(resource, iri, valueFactory.createLiteral(value));	
						}
					}
				}
			}
		}
	}
	

	/**
	 * Wrappermethod for creating an IRI and at the same time save the URI
	 * for later validation
	 * @param uriString - The URI
	 * @return IRI
	 */
	private IRI createIri(String uriString) {
		multipleURIValidator.addUri(uriString);
		return SimpleValueFactory.getInstance().createIRI(uriString);
	}

	private static String path(String parent, IRI type) {
		return parent.isEmpty() ? type.getLocalName() : parent + "." + type.getLocalName();
	}
	
	/**
	 * Checks that the given string has the format of a languange specific string
	 * That it has a format like this: "en:Description in English"
	 * @param testIfLanguage - The string to test
	 * @return T/F depending of the test
	 */
	private boolean isLanguageValue(String testIfLanguage) {
		if(Util.isNullOrEmpty(testIfLanguage)) {
			return false;
		}
		
		String[] split = testIfLanguage.split(SUN);
		if(split.length == 2) {
			if(Util.isNullOrEmpty(split[0]) || Util.isNullOrEmpty(split[1])) {
				return false;
			}
			return (split[0].length() == 2 || split[0].length() == 3) && split[1].length() >= 1;
		}
		
		return false;
	}
	
	private boolean isPhoneValue(String key) {
		String vcardHasValue = VCARD4.PREFIX + ":" + VCARD4.HAS_VALUE.getLocalName();
		return key.equals(vcardHasValue);
	}

	private boolean isWKTLiteral(String key) {
		List<String> allowedTypes = specification.datatypesFor(key);
    	return allowedTypes.contains(GEO.WKT_LITERAL.stringValue());
	}

	/**
	 * Returns the numeric datatype matching the InputType
	 * @param key - Used to get the allowable InputType(s)
	 * @return - The matching datatype
	 */

	private IRI getNumericDatatype(String key) {
    	List<String> allowedTypes = specification.datatypesFor(key);

    	if (allowedTypes.contains(XSD.NON_NEGATIVE_INTEGER.stringValue())) {
    	    return XSD.NON_NEGATIVE_INTEGER;
    	}
    	if (allowedTypes.contains("xsd:decimal")) {
    	    return XSD.DECIMAL;
    	}
    	return null;
	}

	/**
	 * Converts a string to a TemporalAmount object if possible. Otherwise returns null
	 * @param key - Used to get the allowable InputType(s)
	 * @param value - The value to be converted
	 * @return - The resulting TemporalAmount object or null 
	 */
	private TemporalAmount getTemporalAmount(String key, String value) {
	    if (Util.isNullOrEmpty(key) || Util.isNullOrEmpty(value)) {
	        return null; 
	    }

	    if(specification.datatypesFor(key).contains("xsd:duration")) {
	    	try {
			    return Period.parse(value);
			    /**
			     * No need to logg error here.
			     * Its has been already checked in SingleInputValidator
			     */
			} catch (DateTimeParseException | NumberFormatException e) {
				return null;
			}
	    }

	    return null;
	}

	/**
	 * Checks that a value exists and is a valid URI
	 * @param subjectKey - Name of the value
	 * @param subjectURI - The value itself
	 * @throws DcatException - Thrown if null or invalid
	 */
	private void checkSubject(String subjectKey, String subjectURI) throws DcatException {
		Util.checkNotNull(subjectURI, UNABLE_CREATE_MISSING_VALUES.replaceAll("X", subjectKey));
		if(!Util.isURI(subjectURI)) {
			throw new DcatException(INVALID_URI.replaceAll("X", subjectKey + "=" + subjectURI));
		}
	}
	
	/**
	 * Adds all the values in the valueMap to the model inside the sectionIRI(subject)
	 * @param model - The model to add the values to
	 * @param sectionIRI - The section where the values are added
	 * @param valueMap - The map containing the values
	 * @param path - Path to the node, e.g. Dataset.Distribution.
	 */

	private void addToModel(Model model, IRI sectionIRI, MultiValuedMap<String, String> valueMap, String path) {
		ValueFactory valueFactory = SimpleValueFactory.getInstance();
		Set<String> keySet = valueMap.keySet();
		
		for (String key : keySet) {

			IRI iri = VocabularyStringToIRI.getIRI(key);

			if (Util.isNotNullOrEmpty(iri)) {

				Collection<String> values = valueMap.get(key);

				for (String value : values) {

					singleInputValidator.validateData(key, value, path);
					
					/**
					 * First check if its a numeric value...otherwise it might be interpreted as date value further down
					 */
					IRI numericDatatype = getNumericDatatype(key);
					if(numericDatatype != null) {
						model.add(sectionIRI, iri, valueFactory.createLiteral(value, numericDatatype));
					} else {
						TemporalAmount temporalAmount = getTemporalAmount(key, value);
						if (Util.isNotNullOrEmpty(temporalAmount)) {
							model.add(sectionIRI, iri, valueFactory.createLiteral(temporalAmount));
						} else if (Util.isURI(value)) { // Check if its an URI and create a resource
							model.add(sectionIRI, iri, valueFactory.createIRI(value));
						} else if (isLanguageValue(value)) { // Check if it's a language specific string, Ex en¤Some text in english
							String[] split = value.split(SUN);
							model.add(sectionIRI, iri, valueFactory.createLiteral(split[1], split[0]));
						} else {
							Literal dateLiteral = getDateValue(key, value);
							if(dateLiteral != null) {	//Datevalue
								model.add(sectionIRI, iri, dateLiteral);
							} else {					//All other values....for now
								model.add(sectionIRI, iri, valueFactory.createLiteral(value));
							}
						}
					}
				}
			}
		}
	}
	
	// Regexp pattern used for date-format checking
	private final Pattern XSD_DATE_TIME_PATTERN = Pattern.compile("[1|2]{1}[0-9]{3}[-]{1}[0-9]{2}[-]{1}[0-9]{2}[T]{1}[0-9]{2}[:]{1}[0-9]{2}[:]{1}[0-9]{2}"); // YYYY-MM-DDThh:mm:ss
	private final Pattern XSD_DATE_PATTERN = Pattern.compile("[1|2]{1}[0-9]{3}[-]{1}[0-9]{2}[-]{1}[0-9]{2}"); // YYYY-MM-DD
	private final Pattern XSD_GYEAR_PATTERN = Pattern.compile("[0-9]{4}"); // YYYY

	/**
	 * Method validates a string against the supplied pattern
	 * 
	 * @param pattern - To validate against
	 * @param test    - The string to validate
	 * @return - T/F depending of the outcome
	 */
	private boolean validateAgainstPattern(Pattern pattern, String test) {
		return pattern.matcher(test).matches();
	}

	/**
	 * Converts a string value to a properly formatted DATE-Literal depending of the format of the
	 * string IF the string is a valid date. Otherwise null is returned
	 * @param value - The value to test
	 * @return - Date-formatted literal or null
	 */
	private Literal getDateValue(String key, String value) {
	    List<String> allowedTypes = specification.datatypesFor(key);
	    
		if (allowedTypes.isEmpty()) {
	        return null;
	    }
		
		ValueFactory valueFactory = SimpleValueFactory.getInstance();
		
		if (allowedTypes.contains(XSD.DATE.stringValue()) && validateAgainstPattern(XSD_DATE_PATTERN, value)) {
		    return valueFactory.createLiteral(value, XSD.DATE);
		}
		if (allowedTypes.contains(XSD.DATETIME.stringValue()) && validateAgainstPattern(XSD_DATE_TIME_PATTERN, value)) {
		    return valueFactory.createLiteral(value, XSD.DATETIME);
		}
		if (allowedTypes.contains(XSD.GYEAR.stringValue()) && validateAgainstPattern(XSD_GYEAR_PATTERN, value)) {
		    return valueFactory.createLiteral(value, XSD.GYEAR);
		}
		return null;
	}
}
