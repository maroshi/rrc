package org.maroshi.client.rrc;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Hashtable;

import net.oauth.OAuthException;

import org.apache.commons.cli.CommandLine;
import org.apache.http.HttpStatus;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.wink.client.ClientResponse;
import org.eclipse.lyo.client.exception.RootServicesException;
import org.eclipse.lyo.client.oslc.OSLCConstants;
import org.eclipse.lyo.client.oslc.jazz.JazzFormAuthClient;
import org.eclipse.lyo.client.oslc.jazz.JazzRootServicesHelper;
import org.maroshi.client.model.Property;
import org.maroshi.client.model.ResourceShape;
import org.maroshi.client.util.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class EnumeratedAttributesLoader {
	private final static String smapleInstanceShapeUri = "https://jazz.net/sandbox02-rm/types/_1ufI4TZoEeOYbalKlkVQRA";

	static Logger logger = Logger.getLogger(EnumeratedAttributesLoader.class);
	private Model rdfModel = null;
	private JazzFormAuthClient client = null;
	private ArrayList<Resource> shapePropertiesResourceArr = new ArrayList<Resource>();

	private org.maroshi.client.model.ResourceShape resourceShape = null;

	public EnumeratedAttributesLoader(JazzFormAuthClient client) {
		this.client = client;
	}

	public Model loadRdfModelForInstanceShape(
			ResourceShape resourceShape) {
		this.resourceShape = resourceShape;
		return loadRdfModelForInstanceShape(resourceShape.getAbout().toString());
	}

	private Model loadRdfModelForInstanceShape(String urlString) {
		if (client == null)
			return null;

		ClientResponse response;
		try {
			// read RDF document into model
			response = client.getResource(urlString, OSLCConstants.CT_RDF);
			InputStream is = response.getEntity(InputStream.class);
			rdfModel = ModelFactory.createDefaultModel();
			rdfModel.read(is, urlString);
			response.consumeContent();

			if (resourceShape == null) {
				// read RDF document into ResourceShape object with LYO
				response = client.getResource(urlString, OSLCConstants.CT_RDF);
				resourceShape = response
						.getEntity(org.maroshi.client.model.ResourceShape.class);
				response.consumeContent();
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (OAuthException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return rdfModel;
	}

	private Resource findFirstObjectFor(Resource r,
			com.hp.hpl.jena.rdf.model.Property p) {
		StmtIterator itr = rdfModel.listStatements(r, p, null, null);
		while (itr.hasNext()) {
			Statement statement = (Statement) itr.next();
			Resource rdfObject = (Resource) statement.getObject();
			return rdfObject;
		}
		return null;
	}

	public void loadEnumeratedAttributesToInstanceShape() {
		com.hp.hpl.jena.rdf.model.Property allowedValuedPrp = rdfModel
				.createProperty(OSLCConstants.OSLC_V2 + "allowedValues");
		com.hp.hpl.jena.rdf.model.Property propDefPrp = rdfModel
				.createProperty(OSLCConstants.OSLC_V2 + "propertyDefinition");
		com.hp.hpl.jena.rdf.model.Property rangePrp = rdfModel
				.createProperty(OSLCConstants.OSLC_V2 + "range");

		// process all RDF triples with allowedValuedPrp
		StmtIterator itr = rdfModel.listStatements(null, allowedValuedPrp,
				null, null);
		Hashtable<String, Hashtable<String, String>> identifiedEnumerationHash = new Hashtable<String, Hashtable<String, String>>();
		while (itr.hasNext()) { // each RDF triple with allowedValuedPrp
			Statement rdfStatement = (Statement) itr.next();
			Resource rdfSubject = rdfStatement.getSubject();
			Hashtable<String, String> currEnumeration = getEnumeratedValues_Hash(
					rangePrp, identifiedEnumerationHash, rdfStatement,
					rdfSubject);

			// identify the attribute pNode (as the RDF triplet subject)
			// query the propertyDefinition with RDF query
			Resource enumeratedAttributeURI_rdfRsource = rdfSubject
					.getPropertyResourceValue(propDefPrp);
			storeEnumeratedValuesHashInto_ResourceShape(propDefPrp, rdfSubject,
					currEnumeration, enumeratedAttributeURI_rdfRsource);
		}
	}

	private void storeEnumeratedValuesHashInto_ResourceShape(
			com.hp.hpl.jena.rdf.model.Property propDefPrp, Resource rdfSubject,
			Hashtable<String, String> enumeratedValues_Hash, Resource attribId) {
		if (attribId != null) {
			logger.debug("Assigned enumerated values to attrib: "
					+ attribId.getURI());
			URI attribURI = null;
			try {
				attribURI = new URI(attribId.getURI());
			} catch (URISyntaxException e) {
				e.printStackTrace();// cannot happen since attribId is
									// defined RDF resource
			}
			// locate attribURI in resourceShape
			Property attribProperty = resourceShape.getProperty(attribURI);
			if (attribProperty != null) {
				// assign the enumeratedValues to the property
				attribProperty.setAllowedValues(enumeratedValues_Hash);
			} else {
				logger.fatal("Unidentified enumerated attribute :"
						+ attribId.getURI());
			}
		} else {
			logger.fatal("Undefined enumerated attribute for : " + rdfSubject
					+ " , " + propDefPrp);
		}
	}

	private Hashtable<String, String> getEnumeratedValues_Hash(
			com.hp.hpl.jena.rdf.model.Property rangePrp,
			Hashtable<String, Hashtable<String, String>> identifiedEnumerationHash,
			Statement rdfStatement, Resource rdfSubject) {
		String enumerationId = findFirstObjectFor(rdfSubject, rangePrp)
				.getURI();
		// identify the enumeration (with the range rdfObject of the same
		// rdfSubject)
		Hashtable<String, String> currEnumeration = identifiedEnumerationHash
				.get(enumerationId);
		if (currEnumeration == null) {// load the enumeration values with
										// new query
			logger.debug("  Loading enumerated values hash table from Jazz server : "
					+ enumerationId);
			currEnumeration = loadEnumeratedValues((Resource) rdfStatement
					.getObject());
			identifiedEnumerationHash.put(enumerationId, currEnumeration);
		}
		return currEnumeration;
	}

	private Hashtable<String, String> loadEnumeratedValues(Resource rdfResource) {
		Model enumeratedValues_rdfModel = null;
		ArrayList<Resource> valueURI_rdfRsourceList;
		Hashtable<String, String> enumeratedValuesHash;
		com.hp.hpl.jena.rdf.model.Property allowedValue_RdfPrp;

		allowedValue_RdfPrp = rdfModel.createProperty(OSLCConstants.OSLC_V2
				+ "allowedValue");

		// store the value URI to list
		valueURI_rdfRsourceList = storeValueURIinArray(rdfResource,
				allowedValue_RdfPrp);

		// request RDF document holding the value URI labels into
		// enumeratedValues_rdfModel
		String firstVAlueURI = valueURI_rdfRsourceList.get(0).getURI();
		enumeratedValues_rdfModel = requestRdfDocumentForValueURI(
				enumeratedValues_rdfModel, firstVAlueURI);

		// read label per valueURIrdfRsource and store in enumeratedValuesHash
		enumeratedValuesHash = new Hashtable<String, String>();
		storeLabelsAndTheirURIinHash(enumeratedValues_rdfModel,
				valueURI_rdfRsourceList, enumeratedValuesHash);
		return enumeratedValuesHash;
	}

	private void storeLabelsAndTheirURIinHash(Model enumeratedValues_rdfModel,
			ArrayList<Resource> valueURI_rdfRsourceList,
			Hashtable<String, String> enumeratedValuesHash) {
		com.hp.hpl.jena.rdf.model.Property labelRdfProperty;
		for (Resource enumeratedValueURI : valueURI_rdfRsourceList) {
			labelRdfProperty = enumeratedValues_rdfModel
					.createProperty(OSLCConstants.RDFS + "label");
			NodeIterator lablesRdfIter = enumeratedValues_rdfModel
					.listObjectsOfProperty(enumeratedValueURI, labelRdfProperty);
			while (lablesRdfIter.hasNext()) {
				RDFNode labelRdfNode = (RDFNode) lablesRdfIter.next();
				logger.debug("   label='" + labelRdfNode.toString()
						+ "'\t\t valueURI=" + enumeratedValueURI.getURI());
				enumeratedValuesHash.put(labelRdfNode.toString(),
						enumeratedValueURI.getURI());
			}
		}
	}

	private Model requestRdfDocumentForValueURI(
			Model enumeratedValues_rdfModel, String enumeratedValues_URI) {
		ClientResponse response;
		InputStream is;
		// String enumeratedValues_URI = valueURIrdfRsourceList.get(0).getURI();
		try {
			response = client.getResource(enumeratedValues_URI,
					OSLCConstants.CT_RDF);
			is = response.getEntity(InputStream.class);
			enumeratedValues_rdfModel = ModelFactory.createDefaultModel();
			enumeratedValues_rdfModel.read(is, enumeratedValues_URI);
			response.consumeContent();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (OAuthException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return enumeratedValues_rdfModel;
	}

	private ArrayList<Resource> storeValueURIinArray(Resource rdfResource,
			com.hp.hpl.jena.rdf.model.Property allowedValuePrp) {
		ArrayList<Resource> valueURIrdfRsourceList;
		valueURIrdfRsourceList = new ArrayList<Resource>();
		NodeIterator rdfStatmentsIter = rdfModel.listObjectsOfProperty(
				rdfResource, allowedValuePrp);
		while (rdfStatmentsIter.hasNext()) {
			Resource enumeratedValueURI = (Resource) rdfStatmentsIter.next();
			valueURIrdfRsourceList.add(enumeratedValueURI);
		}
		return valueURIrdfRsourceList;
	}

	private void dumpResourceShape(String urlString) {

		ClientResponse response;

		try {
			response = client.getResource(urlString, OSLCConstants.CT_RDF);
			resourceShape = response
					.getEntity(org.maroshi.client.model.ResourceShape.class);
			ArrayList<org.maroshi.client.model.Property> attribsArr = new ArrayList<org.maroshi.client.model.Property>();
			for (int i = 0; i < resourceShape.getProperties().length; i++) {
				if (resourceShape.getProperties()[i].getRepresentation() == null) {
					attribsArr.add(resourceShape.getProperties()[i]);
				}
			}
			for (Property property : attribsArr) {
				logger.debug("Attribute: " + property.getTitle());
				logger.debug("   " + property.getValueType());
				logger.debug("   " + property.getPropertyDefinition());
				logger.debug("   " + property.getAllowedValuesRef());
			}
			response.consumeContent();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (OAuthException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		LoggerFactory.config();
		; // init logger

		CommandLine cmd = rrc1.readCliArgs(args);
		if (cmd == null)
			return;

		String rmContextUrl = cmd.getOptionValue("conn.site.rm");
		String jtsContextUrl = cmd.getOptionValue("conn.site.jts");
		String user = cmd.getOptionValue("conn.user");
		String passwd = cmd.getOptionValue("conn.password");
		String projectArea = cmd.getOptionValue("conn.projectArea");

		JazzRootServicesHelper helper;

		try {
			// STEP 1: Initialize a Jazz rootservices helper and indicate we're
			// looking for the RequirementManagement catalog
			helper = new JazzRootServicesHelper(rmContextUrl,
					OSLCConstants.OSLC_RM_V2);

			// STEP 2: Create a new Form Auth client with the supplied
			// user/password
			// RRC is a fronting server, so need to use the initForm() signature
			// which allows passing of an authentication URL.
			// For RRC, use the JTS for the authorization URL
			JazzFormAuthClient client = helper.initFormClient(user, passwd,
					jtsContextUrl);

			// STEP 3: Login in to Jazz Server
			if (client.formLogin() != HttpStatus.SC_OK) {
				logger.error("-- falied connection --");
				System.exit(1);
			}

			// STEP 4: Get the URL of the OSLC ChangeManagement catalog
			String catalogUrl = helper.getCatalogUrl();
			logger.info("-- catalogUrl=" + catalogUrl);
			// STEP 5: Find the OSLC Service Provider for the project area we
			// want to work with
			String serviceProviderUrl = client.lookupServiceProviderUrl(
					catalogUrl, projectArea);
			logger.info("-- serviceProviderUrl=" + serviceProviderUrl);

			EnumeratedAttributesLoader oslcShape = new EnumeratedAttributesLoader(
					client);
			// String urlString =
			// "https://jazz.net/sandbox02-rm/resources/_gatgoTcVEeOYbalKlkVQRA";
			String urlString = oslcShape.smapleInstanceShapeUri;

			if (oslcShape.loadRdfModelForInstanceShape(urlString) == null) {
				logger.fatal("-- failed load RDF model.");
				return;
			}
			oslcShape.loadEnumeratedAttributesToInstanceShape();

		} catch (RootServicesException re) {
			logger.log(Level.FATAL,
					"Unable to access the Jazz rootservices document at: "
							+ rmContextUrl + "/rootservices", re);
		} catch (Exception e) {
			logger.log(Level.FATAL, e.getMessage(), e);
		}
	}

}
