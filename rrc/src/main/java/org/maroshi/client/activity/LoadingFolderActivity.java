package org.maroshi.client.activity;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;
import org.eclipse.lyo.client.oslc.resources.RmConstants;
import org.maroshi.client.activity.DoActivity.DoActivityEnum;
import org.maroshi.client.util.LoggerFactory;

public class LoadingFolderActivity extends AbstractActivity {

	static Logger logger = Logger.getLogger(LoadingFolderActivity.class);
	static final String folderUriOptionFlag = "req.folder.URI";

	private String folderUriOptionVal;

	private boolean missingFolder = true;

	@Override
	public void init() {
		super.init();
		folderUriOptionVal = readOptionValue(folderUriOptionFlag);
		if (folderUriOptionVal == null) {
			logger.warn("Required option missing : --" + folderUriOptionFlag
					+ " ! Using root folder as default.");
			missingFolder = false;
			return;
		}
		if (!isValidOptionValue(folderUriOptionFlag)) {
			return;
		}
		missingFolder = false;
	}

	@Override
	public String execute() {
		String retVal = ActivityConstants.EXE_SUCCESS;
		if (missingFolder)
			return ActivityConstants.EXE_FAIL;
		super.execute();
		
		URI targerFolderURI;
		if (folderUriOptionVal == null) // accept null folder URI to be default root folder
			return ActivityConstants.EXE_SUCCESS;
		
		try {
			targerFolderURI = new URI(folderUriOptionVal);
			getContext().getRequirement().getExtendedProperties()
					.put(RmConstants.PROPERTY_PARENT_FOLDER, targerFolderURI);
		} catch (URISyntaxException e) {
			logger.error("Failed load requirement folder URI: "
					+ folderUriOptionVal);
			e.printStackTrace();
			return ActivityConstants.EXE_FAIL;
		}
		logger.debug("Successful loading requirment folder URI: "
				+ targerFolderURI.toString());
		return retVal;
	}

	@Override
	public void planNextActivity() {
		if (getContext().getExecutionResult() == ActivityConstants.EXE_FAIL)
			return;
		super.planNextActivity();
		LoadingAttributesActivity loadingAttributesActivity = new LoadingAttributesActivity();
		getSchedule().add(loadingAttributesActivity);
		logger.debug(LoggerFactory.LINE_TITLE + "to -> "
				+ loadingAttributesActivity.getClass().getName());
	}
}
