package org.maroshi.client.activity;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.eclipse.lyo.client.oslc.resources.Requirement;
import org.eclipse.lyo.client.oslc.resources.RmConstants;
import org.maroshi.client.activity.DoActivity.DoActivityEnum;
import org.maroshi.client.util.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class LoadingTextActivity extends AbstractActivity {

	static Logger logger = Logger.getLogger(LoadingTextActivity.class);

	static final String titleOptionFlag = "req.title";
	static final String contentOptionFlag = "req.content";
	static final String descriptionOptionFlag = "req.desc";

	private String titleOptionVal;
	private String contentOptionVal;
	private String descriptionOptionVal;

	private boolean missingText = true;

	private boolean noTitle;
	private boolean noContent;
	private boolean noDescription;

	@Override
	public void init() {
		super.init();
		titleOptionVal = readOptionValue(titleOptionFlag);
		contentOptionVal = readOptionValue(contentOptionFlag);
		descriptionOptionVal = readOptionValue(descriptionOptionFlag);

		noTitle = (titleOptionVal == null)
				|| !isValidOptionValue(titleOptionFlag);
		noContent = (contentOptionVal == null)
				|| !isValidOptionValue(contentOptionFlag);
		if (noTitle && noContent) {
			logger.error("Required option(s) missing : --" + titleOptionFlag
					+ " and/or --" + contentOptionFlag + " !");
			return;
		}
		missingText = false;
	}

	@Override
	public String execute() {
		if (missingText)
			return ActivityConstants.EXE_FAIL;

		super.execute();

		Requirement req = getContext().getRequirement();

		if (!noTitle) { // there is title text
			req.setTitle(titleOptionVal);
			logger.debug("Successful assign title");
		}

		noDescription = (descriptionOptionVal == null)
				|| !isValidOptionValue(descriptionOptionFlag);
		if (!noDescription) {// there is description text
			req.setDescription(descriptionOptionVal);
			logger.debug("Successful assign description");
		}

		if (!noContent) {// there is content
			Element parsedContent = convertStringToXHTML(contentOptionVal);
			if (parsedContent == null) { // failed XHTML parsing
				return ActivityConstants.EXE_FAIL;
			}
			req.getExtendedProperties().put(RmConstants.PROPERTY_PRIMARY_TEXT,
					parsedContent);
			logger.debug("Successful assign XHTML content");
		}
		return ActivityConstants.EXE_SUCCESS;
	}

	@Override
	public void planNextActivity() {
		if (getContext().getExecutionResult() == ActivityConstants.EXE_FAIL)
			return;
		super.planNextActivity();
		if (getContext().getDoCommand() == DoActivityEnum.CREATE) {
			LoadingFolderActivity loadingFolderActivity = new LoadingFolderActivity();
			getSchedule().add(loadingFolderActivity);
			logger.debug(LoggerFactory.LINE_TITLE + "to -> "
					+ loadingFolderActivity.getClass().getName());
		}
	}

	private static Element convertStringToXHTML(String text) {

		// Create a DOM builder and parse the fragment.
		String fragment = "<div>" + text + "</div>";
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		Document parsedFragment = null;
		try {
			parsedFragment = factory.newDocumentBuilder().parse(
					new InputSource(new StringReader(fragment)));
		} catch (SAXException e) {
			logger.error("Failed XHTML parsing to --" + contentOptionFlag
					+ "=\"" + fragment + "\".");
			e.printStackTrace();
			return null;
		} catch (ParserConfigurationException e) {
			logger.error("Failed initialize XHTML parser.");
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		logger.debug("Successful XHTML parsing text.");

		// Create the document fragment node to hold the new nodes.
		Element divElement = null;
		divElement = parsedFragment.createElementNS(
				RmConstants.NAMESPACE_URI_XHTML, "div");
		try {
			divElement.appendChild(parsedFragment.getDocumentElement());
		} catch (Exception e) {
			logger.error("Failed append XHTML element.");
			e.printStackTrace();
			return null;
		}
		return divElement;
	}
}
