package org.maroshi.client.rrc;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.oauth.OAuthException;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.wink.client.ClientResponse;
import org.eclipse.lyo.client.exception.ResourceNotFoundException;
import org.eclipse.lyo.client.oslc.OSLCConstants;
import org.eclipse.lyo.client.oslc.jazz.JazzFormAuthClient;
import org.eclipse.lyo.client.oslc.resources.Requirement;
import org.eclipse.lyo.client.oslc.resources.RmConstants;
import org.eclipse.lyo.client.oslc.resources.RmUtil;
import org.eclipse.lyo.oslc4j.core.model.OslcMediaType;
import org.eclipse.lyo.oslc4j.core.model.ResourceShape;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class RequirmentFactory {

	private JazzFormAuthClient rrcClient = null;
	private String serviceProviderURI = null;
	private String requirementFactory = null;

	private RequirmentFactory(JazzFormAuthClient rrcClient,
			String serviceProviderURI) {
		super();
		this.rrcClient = rrcClient;
		this.serviceProviderURI = serviceProviderURI;
	}

	static RequirmentFactory current = null;

	static RequirmentFactory instance(JazzFormAuthClient client,
			String serviceProviderURI) {
		if (current != null) {
			return current;
		} else {
			current = new RequirmentFactory(client, serviceProviderURI);
		}
		return current;
	}

	public Requirement create(String reqType, String folderURI, String title,
			String primaryText) throws URISyntaxException,
			ResourceNotFoundException, OAuthException {

		if (reqType == null || reqType.length() == 0)
			return null;
		if (rrcClient == null)
			return null;

		// STEP 1: Create base requirements
		// Get the Creation Factory URL for change requests so that we can
		// create one
		Requirement requirement = new Requirement();
		String reqOslcResourceType = requirement.getRdfTypes()[0].toString();
		try {
			requirementFactory = rrcClient.lookupCreationFactory(
					serviceProviderURI, OSLCConstants.OSLC_RM_V2,
					reqOslcResourceType);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// STEP 2: Get Feature Requirement Type URL
		ResourceShape featureInstanceShape = null;
		try {
			featureInstanceShape = RmUtil.lookupRequirementsInstanceShapes(
					serviceProviderURI, OSLCConstants.OSLC_RM_V2,
					reqOslcResourceType, rrcClient, reqType);
		} catch (IOException e) {
			e.printStackTrace();
		}
		JazzConnection.display("-- featureInstanceShape="
				+ featureInstanceShape.getAbout());

		// STEP 3: Set the type and title
		requirement.setInstanceShape(featureInstanceShape.getAbout());
		requirement.setTitle(title);

		// STEP 4: Set the PrimaryText into Extended Properties as XHTML
		// document
		org.w3c.dom.Element htmlElement;
		htmlElement = convertStringToHTML(primaryText);
		requirement.getExtendedProperties().put(
				RmConstants.PROPERTY_PRIMARY_TEXT, htmlElement);

		// STEP 5: Set the target folder
		URI targerFolderURI = new URI(folderURI);
		requirement.getExtendedProperties().put(
				RmConstants.PROPERTY_PARENT_FOLDER, targerFolderURI);

		// STEP 6: Submit the create (POST) request
		ClientResponse creationResponse = null;
		try {
			creationResponse = rrcClient.createResource(requirementFactory,
					requirement, OslcMediaType.APPLICATION_RDF_XML,
					OslcMediaType.APPLICATION_RDF_XML);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (OAuthException e) {
			e.printStackTrace();
		}
		
		if (creationResponse.getStatusCode() != HttpStatus.SC_CREATED){
			JazzConnection.display("Failed creation: "+creationResponse.getStatusCode());
		}

		String req01URL = creationResponse.getHeaders().getFirst(
				HttpHeaders.LOCATION);
		creationResponse.consumeContent();
		JazzConnection.display("-- requirement.getURI=" + req01URL);

		// STEP 5: Reload the POSTED requirement from the server
		// GET resources from req03 in order edit its values
		ClientResponse getResponse = null;
		try {
			getResponse = rrcClient.getResource(req01URL,
					OslcMediaType.APPLICATION_RDF_XML);
		} catch (IOException e) {
			e.printStackTrace();
		}
		requirement = getResponse.getEntity(Requirement.class);
		getResponse.consumeContent();

		return requirement;
	}

	private static Element convertStringToHTML(String text) {

		Document document = null;
		try {
			document = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		}
		// Create a DOM builder and parse the fragment.
		String fragment = "<div>" + text + "</div>";
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		Document parsedFragment = null;
		DocumentFragment docfrag = null;
		try {
			parsedFragment = factory.newDocumentBuilder().parse(
					new InputSource(new StringReader(fragment)));
			docfrag = parsedFragment.createDocumentFragment();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Create the document fragment node to hold the new nodes.
		Element divElement = null;
		divElement = parsedFragment.createElementNS(
				RmConstants.NAMESPACE_URI_XHTML, "div");
		try {
			divElement.appendChild(parsedFragment.getDocumentElement());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return divElement;
	}
}
