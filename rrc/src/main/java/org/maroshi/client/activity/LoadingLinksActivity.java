package org.maroshi.client.activity;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.lyo.client.oslc.resources.Requirement;
import org.eclipse.lyo.oslc4j.core.model.Link;
import org.maroshi.client.model.Property;
import org.maroshi.client.model.ResourceShape;
import org.maroshi.client.model.ValueType;
import org.maroshi.client.rrc.EnumeratedAttributesLoader;
import org.maroshi.client.util.LoggerHelper;

public class LoadingLinksActivity extends AbstractActivity {
	static Logger logger = Logger.getLogger(LoadingLinksActivity.class);
	static final String linkOptionFlag = "req.link";
	private boolean noLinks;
	private Hashtable<String, ArrayList<Link>> linksHash;
	private boolean failed = false;
	private ResourceShape resourcesShape;

	@Override
	public void init() {
		super.init();

		String[] linkLinesArr = getContext().getCommandLine().getOptionValues(
				linkOptionFlag);
		if (linkLinesArr == null) {
			noLinks = true;
			return;
		}
		String linkLine = null;
		// collect all the links into hash table.
		// key - link name
		// URI - stored in Array list
		// allowing duplicate links
		linksHash = new Hashtable<String, ArrayList<Link>>();

		for (int i = 0; i < linkLinesArr.length; i++) {
			linkLine = linkLinesArr[i];
			if (linkLine == null || linkLine.length() == 0) {
				logger.error("Missing option value for: --" + linkOptionFlag);
				failed = true;
				break;
			}
			String[] splitedAttribLine = linkLine.split("=", 2);
			if (splitedAttribLine == null || splitedAttribLine.length < 2) {
				logger.error("One \"=\" sign is requied for option --"
						+ linkOptionFlag);
				failed = true;
				break;
			}
			String linkName = splitedAttribLine[0].trim();
			if (linkName.length() > 0) { // link name not empty
				String linkURIstr = splitedAttribLine[1].trim();
				URI linkURI = linkStrToURI(linkURIstr);
				Link newLink = new Link(linkURI);
				if (linkURI != null) { // URI is valid
					ArrayList<Link> uriArr = linksHash.get(linkName);
					if (uriArr == null) {
						uriArr = new ArrayList<Link>();
						uriArr.add(newLink);
						linksHash.put(linkName, uriArr);
					} else {
						uriArr.add(newLink);
					}
				} else {
					// error is reported in linkStrToURI
					failed = true;
					break;
				}
			} else {
				logger.error("LinkName is reuired for option --"
						+ linkOptionFlag);
				failed = true;
				break;
			}
		}
		if (failed) {
			logger.error("Failed read linkName=linkURI pair from: "
					+ LoggerHelper.quote(linkLine));
		}
		else
			dumpLinksHashToLogger();
	}

	@Override
	public String execute() {
		String retVal = ActivityConstants.EXE_SUCCESS;

		if (failed)
			return ActivityConstants.EXE_FAIL;
		super.execute();
		if (noLinks) {
			logger.debug(LoggerHelper.LINE_TITLE + "  No Link.");
			return ActivityConstants.EXE_SUCCESS;
		}

		resourcesShape = getContext().getRequirementInstanceShape();

		Iterator<String> linksHashIterator = linksHash.keySet().iterator();
		while (linksHashIterator.hasNext()) {
			String linkName = (String) linksHashIterator.next();
			Property p = resourcesShape.getProperty(linkName);
			if (p == null) {// undefined attribute name
				reportUndefinedLinkName(linkName);
				retVal = ActivityConstants.EXE_FAIL;
				break;
			} else {// assign the attribute value
				if ( (retVal = assignLink(linkName)) == ActivityConstants.EXE_FAIL)
					break;
			}
		}
		return retVal;
	}

	@Override
	public void planNextActivity() {
		if (getContext().getExecutionResult() == ActivityConstants.EXE_FAIL)
			return;
		super.planNextActivity();
		nextActivityIs(new SubmitNewRequirementActivity());
	}

	private String assignLink(String linkName) {
		String retVal = ActivityConstants.EXE_SUCCESS;
		ArrayList<Link> linksList = linksHash.get(linkName);
		if (linksList != null) {
			URI attribDefinitionUri = resourcesShape
					.getPropertyUri(linkName);
//			Property resourceShapeProp = resourcesShape.getProperty(attribDefinitionUri);
//			resourceShapeProp.setValueType(ValueType.LocalResource);
			
			QName qName = URItoQName(attribDefinitionUri);
			Requirement req = getContext().getRequirement();
			Map<QName, Object> reqExtProperties = req
					.getExtendedProperties();
			
			// copy Link list to URI list
			ArrayList<URI> uriList = new ArrayList<URI>();
			for (Link link : linksList) {
				uriList.add(link.getValue());
			}
			// put the link in ExtendedProperties map
			// key = attribDefinitionUri
			// value = URI ArrayList
			reqExtProperties.put(qName, uriList);
		} else {
			logger.error("Null URI list for link name "
					+ LoggerHelper.quote(linkName) + ".");
			retVal = ActivityConstants.EXE_FAIL;
		}
		return retVal;
	}

	private void reportUndefinedLinkName(String linkName) {
		String requirementType = resourcesShape.getTitle();
		logger.error("Undefined link name " + LoggerHelper.quote(linkName)
				+ " for requirement type "
				+ LoggerHelper.quote(requirementType) + " .");
		StringBuffer allowedLinks = new StringBuffer();
		Iterator<String> iter = resourcesShape.getLinkNames().iterator();
		while (iter.hasNext()) {
			String currAttribName = (String) iter.next();
			allowedLinks.append(LoggerHelper.quote(currAttribName));
			if (iter.hasNext()) {
				allowedLinks.append(", ");
			}
		}
		allowedLinks.append(" .");
		logger.error("Defined link names: " + allowedLinks.toString());
	}

	private URI linkStrToURI(String linkURIstr) {
		URI retVal = null;
		try {
			retVal = new URI(linkURIstr);
		} catch (URISyntaxException e) {
			logger.error("Illegla URI syntax for linkURI: " + linkURIstr);
		}
		return retVal;
	}

	private void dumpLinksHashToLogger() {
		Level loggerLevel = logger.getEffectiveLevel();
		if (loggerLevel.isGreaterOrEqual(org.apache.log4j.Level.DEBUG)) {
			Enumeration<String> linkNames = linksHash.keys();
			while (linkNames.hasMoreElements()) {
				String linkName = (String) linkNames.nextElement();
				logger.debug(LoggerHelper.LINE_TITLE + "linkName="
						+ linkName);
				ArrayList<Link> linkArr = linksHash.get(linkName);
				for (Link uri : linkArr) {
					logger.debug(LoggerHelper.LINE_TITLE + "   linkURI="
							+ uri.getValue().toString());
				}
			}
			return;
		}
	}
}
