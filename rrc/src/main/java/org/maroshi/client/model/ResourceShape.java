package org.maroshi.client.model;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import net.oauth.OAuthException;

import org.apache.log4j.Logger;
import org.apache.wink.client.ClientResponse;
import org.eclipse.lyo.client.exception.ResourceNotFoundException;
import org.eclipse.lyo.client.oslc.OSLCConstants;
import org.eclipse.lyo.client.oslc.OslcClient;
import org.eclipse.lyo.oslc4j.core.annotation.OslcDescription;
import org.eclipse.lyo.oslc4j.core.annotation.OslcName;
import org.eclipse.lyo.oslc4j.core.annotation.OslcNamespace;
import org.eclipse.lyo.oslc4j.core.annotation.OslcPropertyDefinition;
import org.eclipse.lyo.oslc4j.core.annotation.OslcRange;
import org.eclipse.lyo.oslc4j.core.annotation.OslcReadOnly;
import org.eclipse.lyo.oslc4j.core.annotation.OslcRepresentation;
import org.eclipse.lyo.oslc4j.core.annotation.OslcResourceShape;
import org.eclipse.lyo.oslc4j.core.annotation.OslcTitle;
import org.eclipse.lyo.oslc4j.core.annotation.OslcValueShape;
import org.eclipse.lyo.oslc4j.core.annotation.OslcValueType;
import org.eclipse.lyo.oslc4j.core.model.AbstractResource;
import org.eclipse.lyo.oslc4j.core.model.CreationFactory;
import org.eclipse.lyo.oslc4j.core.model.OslcConstants;
import org.eclipse.lyo.oslc4j.core.model.Representation;
import org.eclipse.lyo.oslc4j.core.model.Service;
import org.eclipse.lyo.oslc4j.core.model.ServiceProvider;
import org.eclipse.lyo.oslc4j.core.model.ValueType;
import org.maroshi.client.activity.LoadingAttributesActivity;
import org.maroshi.client.util.LoggerHelper;

@OslcNamespace(OslcConstants.OSLC_CORE_NAMESPACE)
@OslcResourceShape(title = "OSLC Resource Shape Resource Shape", describes = OslcConstants.TYPE_RESOURCE_SHAPE)
public final class ResourceShape extends AbstractResource {
	static Logger logger = Logger.getLogger(ResourceShape.class);

	private final SortedSet<URI> describes = new TreeSet<URI>();
	private final TreeMap<URI, Property> properties = new TreeMap<URI, Property>();

	private String title;
	private Hashtable<String, URI> propertyNamesHash;
	private ArrayList<String> attributeNamesList;
	private ArrayList<String> linkNamesList;

	public ResourceShape() {
		super();
	}

	public ResourceShape(final URI about) {
		super(about);
	}

	public void addDescribeItem(final URI describeItem) {
		this.describes.add(describeItem);
	}

	public void addProperty(final Property property) {
		this.properties.put(property.getPropertyDefinition(), property);
	}

	// Bugzilla 392780
	public Property getProperty(URI definition) {
		return properties.get(definition);
	}

	@OslcDescription("Type or types of resource described by this shape")
	@OslcPropertyDefinition(OslcConstants.OSLC_CORE_NAMESPACE + "describes")
	@OslcReadOnly
	@OslcTitle("Describes")
	public URI[] getDescribes() {
		return describes.toArray(new URI[describes.size()]);
	}

	@OslcDescription("The properties that are allowed or required by this shape")
	@OslcName("property")
	@OslcPropertyDefinition(OslcConstants.OSLC_CORE_NAMESPACE + "property")
	@OslcRange(OslcConstants.TYPE_PROPERTY)
	@OslcReadOnly
	@OslcRepresentation(Representation.Inline)
	@OslcTitle("Properties")
	@OslcValueShape(OslcConstants.PATH_RESOURCE_SHAPES + "/"
			+ OslcConstants.PATH_PROPERTY)
	@OslcValueType(ValueType.LocalResource)
	public Property[] getProperties() {
		return properties.values().toArray(new Property[properties.size()]);
	}

	@OslcDescription("Title of the resource shape. SHOULD include only content that is valid and suitable inside an XHTML <div> element")
	@OslcPropertyDefinition(OslcConstants.DCTERMS_NAMESPACE + "title")
	@OslcReadOnly
	@OslcTitle("Title")
	@OslcValueType(ValueType.XMLLiteral)
	public String getTitle() {
		return title;
	}

	public void setDescribes(final URI[] describes) {
		this.describes.clear();
		if (describes != null) {
			this.describes.addAll(Arrays.asList(describes));
		}
	}

	public void setProperties(final Property[] properties) {
		this.properties.clear();
		if (properties != null) {
			for (Property prop : properties) {
				this.properties.put(prop.getPropertyDefinition(), prop);
			}
		}
	}

	public void setTitle(final String title) {
		this.title = title;
	}

	public static ResourceShape lookupRequirementsInstanceShapes(
			final String serviceProviderUrl, final String oslcDomain,
			final String oslcResourceType, OslcClient client,
			String requiredInstanceShape) throws IOException, OAuthException,
			URISyntaxException, ResourceNotFoundException {

		ClientResponse response = client.getResource(serviceProviderUrl,
				OSLCConstants.CT_RDF);
		ServiceProvider serviceProvider = response
				.getEntity(ServiceProvider.class);

		if (serviceProvider != null) {
			for (Service service : serviceProvider.getServices()) {
				URI domain = service.getDomain();
				if (domain != null && domain.toString().equals(oslcDomain)) {
					CreationFactory[] creationFactories = service
							.getCreationFactories();
					if (creationFactories != null
							&& creationFactories.length > 0) {
						for (CreationFactory creationFactory : creationFactories) {
							for (URI resourceType : creationFactory
									.getResourceTypes()) {
								if (resourceType.toString() != null
										&& resourceType.toString().equals(
												oslcResourceType)) {
									URI[] instanceShapes = creationFactory
											.getResourceShapes();
									if (instanceShapes != null) {
										for (URI typeURI : instanceShapes) {
											response = client.getResource(
													typeURI.toString(),
													OSLCConstants.CT_RDF);
											ResourceShape resourceShape = response
													.getEntity(ResourceShape.class);
											String typeTitle = resourceShape
													.getTitle();
											if ((typeTitle != null)
													&& (typeTitle
															.equalsIgnoreCase(requiredInstanceShape))) {
												return resourceShape;
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}

		throw new ResourceNotFoundException(serviceProviderUrl,
				"InstanceShapes");
	}

	public URI getPropertyUri(String propertyTitle) {
		URI retUri = null;
		if (propertyNamesHash == null) {
			loadPropertyNamesHash();
		}
		retUri = propertyNamesHash.get(propertyTitle);
		return retUri;
	}
	public Property getProperty(String propertyTitle){
		Property retPrp = null;
		URI prpUri = getPropertyUri(propertyTitle);
		if (prpUri != null){
			retPrp = properties.get(prpUri);
		}
		return retPrp;
	}
	
	

	private void loadPropertyNamesHash() {
		propertyNamesHash = new Hashtable<String, URI>();
		attributeNamesList = new ArrayList<String>();
		linkNamesList = new ArrayList<String>();

		Iterator<URI> propertyUriIter = properties.keySet().iterator();
		while (propertyUriIter.hasNext()) {
			URI uri = (URI) propertyUriIter.next();
			if (uri != null) {
				Property currProperty = properties.get(uri);
				if (currProperty != null) {
					String currPropertyTitle = currProperty.getTitle();
					if (currPropertyTitle != null) {
						propertyNamesHash.put(currPropertyTitle, uri);
						if (currProperty.getRepresentation() != null) {
							linkNamesList.add(currPropertyTitle);
						} else {
							attributeNamesList.add(currPropertyTitle);
						}
					} else {
						logger.error("Null  titled property "
								+ LoggerHelper.quote(uri.toString())
								+ " in properties Map in resource shape ."
								+ LoggerHelper.quote(getTitle()) + ".");
					}
				} else {
					logger.error("Undefined property "
							+ LoggerHelper.quote(uri.toString())
							+ " in properties Map in resource shape ."
							+ LoggerHelper.quote(getTitle()) + ".");
				}
			} else {
				logger.error("Undefined null URI in properties Map for in resource shape "
						+ LoggerHelper.quote(getTitle()) + ".");
				return;
			}
		}

	}

	public ArrayList<String> getAttributeNames() {
		return attributeNamesList;

	}

	public ArrayList<String> getLinkNames() {
		return linkNamesList;
	}
}
