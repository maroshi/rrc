package org.maroshi.client.activity;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.DebugGraphics;
import javax.xml.namespace.QName;

import net.oauth.OAuthException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.wink.client.ClientResponse;
import org.eclipse.lyo.client.oslc.resources.Requirement;
import org.eclipse.lyo.oslc4j.core.model.OslcMediaType;
import org.eclipse.lyo.oslc4j.core.model.Property;
import org.eclipse.lyo.oslc4j.core.model.ResourceShape;
import org.maroshi.client.util.LoggerFactory;

public class LoadingAttributesActivity extends AbstractActivity {
	static Logger logger = Logger.getLogger(LoadingAttributesActivity.class);
	static final String attrOptionFlag = "req.attr";

	private String attrOptionVal;
	private boolean noAttrs = false;
	private boolean failed = false;
	private Hashtable<String, ArrayList<String>> attrHash = null;

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
		if (failed) {
			logger.error("Failed read name=value pair from: " + attribLine);
		}
	}

	@Override
	public String execute() {
		if (failed)
			return ActivityConstants.EXE_FAIL;
		super.execute();
		dumpAtriibutesHashToLogger();
		
		ResourceShape instanceShape = getContext().getRequirementInstanceShape();
		Property[] oslcPropertiesArr = instanceShape.getProperties();
		URI instanceShapeURI = instanceShape.getAbout();
		
		ClientResponse getResponse = null;
		try {
			getResponse = getContext().getJazzClient().getResource(instanceShapeURI.toString(),
					OslcMediaType.APPLICATION_RDF_XML);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (OAuthException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		ResourceShape instanceShape1 = getResponse.getEntity(instanceShape.getClass());
		getResponse.consumeContent();
		

		Enumeration<String> attribNamesEnum = attrHash.keys();
		while (attribNamesEnum.hasMoreElements()) {
			String currAttribName = (String) attribNamesEnum.nextElement();
			
			// find the attribute by the property title
			for (Property property : oslcPropertiesArr) {
				String propTitle = property.getTitle();
				if (propTitle!= null && propTitle.equals(currAttribName)){
					logger.debug("Found attribute metadata:"+currAttribName);
					
				}
			}
		}
		return ActivityConstants.EXE_SUCCESS;
	}

	private void dumpAtriibutesHashToLogger() {
		Level loggerLevel = logger.getEffectiveLevel();
		if (loggerLevel.isGreaterOrEqual(org.apache.log4j.Level.DEBUG)) {
			Enumeration<String> attribNames = attrHash.keys();
			while (attribNames.hasMoreElements()) {
				String attribName = (String) attribNames.nextElement();
				logger.debug(LoggerFactory.LINE_TITLE + "attribName="
						+ attribName);
				ArrayList<String> valuesArr = attrHash.get(attribName);
				for (String attribValue : valuesArr) {
					logger.debug(LoggerFactory.LINE_TITLE + "   attribValue="
							+ attribValue);
				}

			}
			return;
		}
	}
}
