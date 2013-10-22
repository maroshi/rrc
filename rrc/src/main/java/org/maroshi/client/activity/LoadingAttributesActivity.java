package org.maroshi.client.activity;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.lyo.client.oslc.OSLCConstants;
import org.eclipse.lyo.client.oslc.resources.Requirement;
import org.eclipse.lyo.oslc4j.core.model.OslcConstants;
import org.maroshi.client.model.Property;
import org.maroshi.client.model.ResourceShape;
import org.maroshi.client.model.ValueType;
import org.maroshi.client.rrc.EnumeratedAttributesLoader;
import org.maroshi.client.util.LoggerHelper;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class LoadingAttributesActivity extends AbstractActivity {
	static Logger logger = Logger.getLogger(LoadingAttributesActivity.class);
	static final String attrOptionFlag = "req.attr";

	private String attrOptionVal;
	private boolean noAttrs = false;
	private boolean failed = false;
	private Hashtable<String, ArrayList<String>> attrHash = null;
	private ResourceShape resourcesShape;

	private static final String XML_ELEMENT_NAME = "test_1";

	@Override
	public void init() {
		super.init();

		String[] attrLinesArr = getContext().getCommandLine().getOptionValues(
				attrOptionFlag);
		if (attrLinesArr == null) {
			noAttrs = true;
		}
		String attribLine = null;
		// collect all the attribute into hash table.
		// key - attribute name
		// value - stored in Array list
		attrHash = new Hashtable<String, ArrayList<String>>();
		for (int i = 0; i < attrLinesArr.length; i++) {
			attribLine = attrLinesArr[i];
			if (attribLine == null || attribLine.length() == 0) {
				failed = true;
				break;
			}
			String[] splitedAttribLine = attribLine.split("=", 2);
			if (splitedAttribLine == null || splitedAttribLine.length != 2) {
				failed = true;
				break;
			}
			String nameKey = splitedAttribLine[0].trim();
			String valueStr = splitedAttribLine[1].trim();
			ArrayList<String> valuesArr = attrHash.get(nameKey);
			if (valuesArr == null) {
				valuesArr = new ArrayList<String>();
				valuesArr.add(valueStr);
				attrHash.put(nameKey, valuesArr);
			} else {
				valuesArr.add(valueStr);
			}
		}
		dumpAtriibutesHashToLogger();
		if (failed) {
			logger.error("Failed read name=value pair from: " + attribLine);
		}
	}

	@Override
	public String execute() {
		String retVal = ActivityConstants.EXE_SUCCESS;

		if (failed)
			return ActivityConstants.EXE_FAIL;
		super.execute();

		resourcesShape = getContext().getRequirementInstanceShape();
		EnumeratedAttributesLoader enumeratedAttributesLoader = new EnumeratedAttributesLoader(
				getContext().getJazzClient());
		enumeratedAttributesLoader.loadRdfModelForInstanceShape(resourcesShape);
		enumeratedAttributesLoader.loadEnumeratedAttributesToInstanceShape();

		Iterator<String> attrHashIterator = attrHash.keySet().iterator();
		while (attrHashIterator.hasNext()) {
			String attribName = (String) attrHashIterator.next();
			Property p = resourcesShape.getProperty(attribName);
			if (p == null) {// undefined attribute name
				reportUndefinedAttribute(attribName);
				retVal = ActivityConstants.EXE_FAIL;
			} else {// assign the attribute value
				retVal = assignAttribute(retVal, attribName);
			}
		}
		return retVal;
	}

	@Override
	public void planNextActivity() {
		if (getContext().getExecutionResult() == ActivityConstants.EXE_FAIL)
			return;
		super.planNextActivity();
		SubmitNewRequirementActivity submitNewRequirementActivity = new SubmitNewRequirementActivity();
		getSchedule().add(submitNewRequirementActivity);
		logger.debug(LoggerHelper.LINE_TITLE + "to -> "
				+ submitNewRequirementActivity.getClass().getName());
	}

	private String assignAttribute(String retVal, String attribName) {
		ArrayList<String> attrValuesList = attrHash.get(attribName);
		if (attrValuesList != null) {
			Iterator<String> attrValueIter = attrValuesList.iterator();
			// for each value for the specific attrib named attribName
			// if there are many values for a singled value attrib
			// the last accepted value will persist
			while (attrValueIter.hasNext()) {
				String attrValue = (String) attrValueIter.next();
				if (attrValue != null) {
					URI attribDefinitionUri = resourcesShape
							.getPropertyUri(attribName);
					if (isValidValue(attribDefinitionUri, attrValue)) {
						attrValue = getAllowedValueFor(attrValue, attribDefinitionUri);
						QName qName = new QName(attribDefinitionUri.toString(),
								"");
						Requirement req = getContext().getRequirement();
						Map<QName, Object> reqExtProperties = req
								.getExtendedProperties();
						
//						resourcesShape.getProperty(attribDefinitionUri).setValueType(ValueType.Resource);
//						attrValue="https://jazz.net/sandbox02-rm/types/_bbYogTZoEeOYbalKlkVQRA#3658147b-3b94-407e-8d04-93dfe20a255d";
						reqExtProperties.put(qName, attrValue);
					} else {
						retVal = ActivityConstants.EXE_FAIL;
					}
				} else {
					logger.error("Null attribute value for attribute name "
							+ LoggerHelper.quote(attribName) + ".");
					retVal = ActivityConstants.EXE_FAIL;
				}
			}
		} else {
			logger.error("Null attribute values list for attribute name "
					+ LoggerHelper.quote(attribName) + ".");
			retVal = ActivityConstants.EXE_FAIL;
		}
		return retVal;
	}

	private String getAllowedValueFor(String attrValue, URI attribDefinitionUri) {
		String retVal = attrValue;
		Property resourceShaperPrp = resourcesShape.getProperty(attribDefinitionUri);
		if (resourceShaperPrp != null){
			if (resourceShaperPrp.hasAllowedValues()){
				retVal = resourceShaperPrp.getAllowedValueURL(attrValue);
				if (retVal == null){
					logger.fatal("Passed value validation, yet fail locate enumerated value for "+LoggerHelper.quote(attrValue));
				}
			}
		}
		return retVal;
	}

	private boolean isValidValue(URI attribDefinitionUri, String attrValue) {
		boolean retVal = true;

		Property resourceShapePrp = resourcesShape
				.getProperty(attribDefinitionUri);
		if (resourceShapePrp.hasAllowedValues()) {// attribute with allowed
													// values list
			if (resourceShapePrp.getAllowedValueURL(attrValue) == null) {
				reportUndefinedAttributeValue(attrValue, resourceShapePrp);
				retVal = false;
			}
		} else { // validate attrValue conform to its type (validate XML
					// literal)
			String xmlTypeStr = resourceShapePrp.getValueType().toString();
			// validate only attributes in XML_NAMESPACE
			if (xmlTypeStr.startsWith(OslcConstants.XML_NAMESPACE)){
				// extract the type without the namespace
				xmlTypeStr = xmlTypeStr.substring(OslcConstants.XML_NAMESPACE.length());
				// Create a DOM builder and parse the fragment.
				SAXSource schemaDoc = buildSchema_SaxSource(xmlTypeStr);
				SAXSource testDoc = buildTest_SaxSource(attrValue);
				SchemaFactory factory = SchemaFactory
						.newInstance("http://www.w3.org/2001/XMLSchema");
				try {
					Schema schema = factory.newSchema(schemaDoc);
					Validator vlidator = schema.newValidator();
					vlidator.validate(testDoc);
				} catch (SAXException e) {
					retVal = false;
					e.printStackTrace();
				} catch (IOException e) {
					retVal = false;
					e.printStackTrace();
				}
			}
			else {
				logger.debug("Cannot validate value from type: "+xmlTypeStr);
			}
		}
		return retVal;
	}

	public static void main(String[] args) {
		String xmlType = "int";
		String attrValue = "123";
		SAXSource shcemaXml = buildSchema_SaxSource(xmlType);
		SAXSource testXml = buildTest_SaxSource(attrValue);
		SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
		try {
			Schema schema = schemaFactory.newSchema(shcemaXml);
			Validator validator = schema.newValidator();
			validator.validate(testXml);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static SAXSource loadSchemaFile(File xsdFile) {
		return null;
	}
	final static String schemaStr1 = ""+
	"<?xml version=\"1.0\"?> <xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"  "+
	" > <xs:element name=\""+XML_ELEMENT_NAME+"\"  type=\"xs:float\"   ></xs:element></xs:schema>";

	private static SAXSource buildSchema_SaxSource(String xmlType) {
		String fragment = "<?xml version=\"1.0\"?> <xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"  "
				+ "><xs:element name=\""+XML_ELEMENT_NAME+"\"  type=\"xs:"+xmlType+"\" ></xs:element></xs:schema>";;
		SAXSource doc = new SAXSource(new InputSource(new StringReader(fragment)));
		return doc;
	}

	private static SAXSource buildTest_SaxSource(String xmlText) {
		String fragment = "<" + XML_ELEMENT_NAME + ">" + xmlText + "</"
				+ XML_ELEMENT_NAME + ">";
		SAXSource doc = new SAXSource(new InputSource(new StringReader(fragment)));
		return doc;
	}

	private void reportUndefinedAttributeValue(String attrValue,
			Property resourceShapePrp) {
		String attribName = resourceShapePrp.getTitle();
		String reqTypeName = resourcesShape.getTitle();
		Set<String> allowedValues = resourceShapePrp.getAllowedValues();
		if (allowedValues == null) {
			logger.error("Failed load enumerated values for attribute"
					+ LoggerHelper.quote(attribName) + " in requirement type "
					+ LoggerHelper.quote(reqTypeName));
			return;
		}
		logger.error("Undefined attribute value "
				+ LoggerHelper.quote(attrValue) + " for attribute "
				+ LoggerHelper.quote(attribName) + " for requirement type "
				+ LoggerHelper.quote(reqTypeName) + ".");
		Iterator<String> allowedValuesIter = allowedValues.iterator();
		StringBuffer sb = new StringBuffer();
		while (allowedValuesIter.hasNext()) {
			String allowedValue = (String) allowedValuesIter.next();
			sb.append(LoggerHelper.quote(allowedValue));
			if (allowedValuesIter.hasNext()) {
				sb.append(", ");
			}
		}
		sb.append(" .");
		logger.error("Allowed values: " + sb.toString());
	}

	private void reportUndefinedAttribute(String attribName) {
		String requirementType = resourcesShape.getTitle();
		logger.error("Undefined attribute name "
				+ LoggerHelper.quote(attribName) + " for requirement type "
				+ LoggerHelper.quote(requirementType) + ".");
		StringBuffer allowedAttribs = new StringBuffer();
		Iterator<String> iter = resourcesShape.getAttributeNames().iterator();
		while (iter.hasNext()) {
			String currAttribName = (String) iter.next();
			allowedAttribs.append(LoggerHelper.quote(currAttribName));
			if (iter.hasNext()) {
				allowedAttribs.append(", ");
			}
		}
		allowedAttribs.append(" .");
		logger.error("Defined attribute names: " + allowedAttribs.toString());
	}

	private void dumpAtriibutesHashToLogger() {
		Level loggerLevel = logger.getEffectiveLevel();
		if (loggerLevel.isGreaterOrEqual(org.apache.log4j.Level.DEBUG)) {
			Enumeration<String> attribNames = attrHash.keys();
			while (attribNames.hasMoreElements()) {
				String attribName = (String) attribNames.nextElement();
				logger.debug(LoggerHelper.LINE_TITLE + "attribName="
						+ attribName);
				ArrayList<String> valuesArr = attrHash.get(attribName);
				for (String attribValue : valuesArr) {
					logger.debug(LoggerHelper.LINE_TITLE + "   attribValue="
							+ attribValue);
				}
			}
			return;
		}
	}
}
