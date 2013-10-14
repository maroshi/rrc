package org.maroshi.client.activity;

import java.io.IOException;
import java.net.URISyntaxException;

import net.oauth.OAuthException;

import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.eclipse.lyo.client.exception.JazzAuthErrorException;
import org.eclipse.lyo.client.exception.JazzAuthFailedException;
import org.eclipse.lyo.client.exception.ResourceNotFoundException;
import org.eclipse.lyo.client.exception.RootServicesException;
import org.eclipse.lyo.client.oslc.OSLCConstants;
import org.eclipse.lyo.client.oslc.jazz.JazzFormAuthClient;
import org.eclipse.lyo.client.oslc.jazz.JazzRootServicesHelper;
import org.eclipse.lyo.client.oslc.resources.Requirement;
import org.eclipse.lyo.client.oslc.resources.RmUtil;
import org.eclipse.lyo.oslc4j.core.model.ResourceShape;
import org.maroshi.client.activity.DoActivity.DoActivityEnum;

public class ReqTypeActivty extends AbstractActivity {

	static Logger logger = Logger.getLogger(ReqTypeActivty.class);
	
	static final String reqTypeFlag = "app.reqType";
	static final String rmOptionFlag = "conn.site.rm";
	static final String jtsOptionFlag = "conn.site.jts";
	static final String userOptionFlag = "conn.user";
	static final String passwordOptionFlag = "conn.password";
	static final String projectOptionFlag = "conn.projectArea";

	private String reqTypeVal;
	private String rmOptionVal;
	private String jtsOptionVal;
	private String userOptionVal;
	private String passwordOptionVal;
	private String projectOptionVal;

	private boolean missingOptionVal = true;
	private JazzRootServicesHelper helper;
	private String requirementFactory;
	private Requirement requirement;

	@Override
	public void init() {
		super.init();
		if (isValidOptionValue(reqTypeFlag) && isValidOptionValue(rmOptionFlag)
				&& isValidOptionValue(jtsOptionFlag)
				&& isValidOptionValue(userOptionFlag)
				&& isValidOptionValue(passwordOptionFlag)
				&& isValidOptionValue(projectOptionFlag)) {
			missingOptionVal = false;
		}
		reqTypeVal = getContext().getCommandLine().getOptionValue(reqTypeFlag);
		rmOptionVal = getContext().getCommandLine().getOptionValue(rmOptionFlag);
		jtsOptionVal = getContext().getCommandLine().getOptionValue(jtsOptionFlag);
		userOptionVal = getContext().getCommandLine().getOptionValue(userOptionFlag);
		passwordOptionVal = getContext().getCommandLine().getOptionValue(passwordOptionFlag);
		projectOptionVal = getContext().getCommandLine().getOptionValue(projectOptionFlag);
	}

	@Override
	public String execute() {
		if (missingOptionVal)
			return ActivityConstants.EXE_FAIL;

		super.execute();

		if (loginJazzServer() == false) {
			return ActivityConstants.EXE_FAIL;
		}
		logger.info("Succesfull Jazz server login.");
		if (locateServiceProvider() == false) {
			return ActivityConstants.EXE_FAIL;
		}
		logger.info("Succesfull locating Jazz serverice provider.");

		try {
			requirement = new Requirement();
		} catch (URISyntaxException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			return ActivityConstants.EXE_FAIL;
		}
		logger.info("Succesfull locating Jazz service provider.");

		String serviceProviderURI = getContext().getServiceProviderUrl();
		String reqOslcResourceType = requirement.getRdfTypes()[0].toString();
		boolean isCreatingRequirement = (getContext().getDoCommand() == DoActivityEnum.CREATE);
		if (isCreatingRequirement
				&& locateRequirmentFactory(serviceProviderURI,
						reqOslcResourceType) == false) {
			return ActivityConstants.EXE_FAIL;
		}
		if (locateInstanceShape(serviceProviderURI, reqOslcResourceType) == false) {
			return ActivityConstants.EXE_FAIL;
		}
		logger.info("Succesfull locating requirement type "+reqTypeVal+".");
		return ActivityConstants.EXE_SUCCESS;
	}

	private boolean locateInstanceShape(String serviceProviderURI,
			String reqOslcResourceType) {
		ResourceShape requirmentInstanceShape;
		boolean retVal = true;
		try {
			requirmentInstanceShape = RmUtil.lookupRequirementsInstanceShapes(
					serviceProviderURI, OSLCConstants.OSLC_RM_V2,
					reqOslcResourceType, getContext().getJazzClient(),
					reqTypeVal);
			getContext()
					.setRequirementInstanceShapeUrl(requirmentInstanceShape);
		} catch (ResourceNotFoundException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			retVal = false;
		} catch (IOException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			retVal = false;
		} catch (OAuthException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			retVal = false;
		} catch (URISyntaxException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			retVal = false;
		}
		return retVal;
	}

	private boolean locateRequirmentFactory(String serviceProviderURI,
			String reqOslcResourceType) {
		boolean retVal = true;
		try {
			requirementFactory = getContext().getJazzClient()
					.lookupCreationFactory(serviceProviderURI,
							OSLCConstants.OSLC_RM_V2, reqOslcResourceType);
			getContext().setRequirementFactoryUrl(requirementFactory);
		} catch (ResourceNotFoundException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			retVal = false;
		} catch (IOException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			retVal = false;
		} catch (OAuthException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			retVal = false;
		} catch (URISyntaxException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			retVal = false;
		}
		return retVal;
	}

	private boolean locateServiceProvider() {
		// Get the URL of the OSLC ChangeManagement catalog
		String catalogUrl = helper.getCatalogUrl();
		// Find the OSLC Service Provider for the project area we
		// want to work with
		boolean retVal = true;
		try {
			String serviceProviderUrl = getContext().getJazzClient()
					.lookupServiceProviderUrl(catalogUrl, projectOptionVal);
			getContext().setServiceProviderUrl(serviceProviderUrl);
		} catch (ResourceNotFoundException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			retVal = false;
		} catch (IOException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			retVal = false;
		} catch (OAuthException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			retVal = false;
		} catch (URISyntaxException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			retVal = false;
		}
		return retVal;
	}

	private boolean loginJazzServer() {
		// STEP 1: Initialize a Jazz rootservices helper and indicate we're
		// looking for the RequirementManagement catalog

		try {
			helper = new JazzRootServicesHelper(rmOptionVal,
					OSLCConstants.OSLC_RM_V2);
		} catch (RootServicesException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			return false;
		}

		// STEP 2: Create a new Form Auth client with the supplied
		// user/password
		// RRC is a fronting server, so need to use the initForm() signature
		// which allows passing of an authentication URL.
		// For RRC, use the JTS for the authorization URL
		JazzFormAuthClient client = helper.initFormClient(userOptionVal,
				passwordOptionVal, jtsOptionVal);
		getContext().setJazzClient(client);

		// STEP 3: Login in to Jazz Server
		try {
			if (client.formLogin() != HttpStatus.SC_OK) {
				return false;
			}
		} catch (JazzAuthFailedException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			return false;
		} catch (JazzAuthErrorException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public void planNextActivity() {
		super.planNextActivity();
	}

}
