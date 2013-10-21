package org.maroshi.client.activity;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import net.oauth.OAuthException;

import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.apache.wink.client.ClientResponse;
import org.eclipse.lyo.client.exception.JazzAuthErrorException;
import org.eclipse.lyo.client.exception.JazzAuthFailedException;
import org.eclipse.lyo.client.exception.ResourceNotFoundException;
import org.eclipse.lyo.client.exception.RootServicesException;
import org.eclipse.lyo.client.oslc.OSLCConstants;
import org.eclipse.lyo.client.oslc.jazz.JazzFormAuthClient;
import org.eclipse.lyo.client.oslc.jazz.JazzRootServicesHelper;
import org.eclipse.lyo.client.oslc.resources.Requirement;
import org.eclipse.lyo.client.oslc.resources.RmUtil;
import org.eclipse.lyo.oslc4j.core.model.CreationFactory;
import org.eclipse.lyo.oslc4j.core.model.Service;
import org.eclipse.lyo.oslc4j.core.model.ServiceProvider;
import org.maroshi.client.activity.DoActivity.DoActivityEnum;
import org.maroshi.client.model.ResourceShape;
import org.maroshi.client.util.LoggerHelper;

public class ConnectingToJazzActivty extends AbstractActivity {

	static Logger logger = Logger.getLogger(ConnectingToJazzActivty.class);

	static final String reqTypeOptionFlag = "app.reqType";
	static final String rmOptionFlag = "conn.site.rm";
	static final String jtsOptionFlag = "conn.site.jts";
	static final String userOptionFlag = "conn.user";
	static final String passwordOptionFlag = "conn.password";
	static final String projectOptionFlag = "conn.projectArea";

	private String reqTypeOptionVal;
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
		if (isValidOptionValue(reqTypeOptionFlag) && isValidOptionValue(rmOptionFlag)
				&& isValidOptionValue(jtsOptionFlag)
				&& isValidOptionValue(userOptionFlag)
				&& isValidOptionValue(passwordOptionFlag)
				&& isValidOptionValue(projectOptionFlag)) {
			missingOptionVal = false;
		}
		reqTypeOptionVal = readOptionValue(reqTypeOptionFlag);
		rmOptionVal = readOptionValue(rmOptionFlag);
		jtsOptionVal = readOptionValue(jtsOptionFlag);
		userOptionVal = readOptionValue(userOptionFlag);
		passwordOptionVal = readOptionValue(passwordOptionFlag);
		projectOptionVal = readOptionValue(projectOptionFlag);
	}

	@Override
	public String execute() {
		if (missingOptionVal)
			return ActivityConstants.EXE_FAIL;

		super.execute();

		// login
		if (loginJazzServer() == false) {
			return ActivityConstants.EXE_FAIL;
		}
		logger.info("Succesfull Jazz server login.");
		// locate service provider URL
		if (locateServiceProvider() == false) {
			return ActivityConstants.EXE_FAIL;
		}
		logger.info("Succesfull locating Jazz service provider.");
		// create requirement instance and save in context
		try {
			requirement = new Requirement();
			getContext().setRequirement(requirement);
		} catch (URISyntaxException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			return ActivityConstants.EXE_FAIL;
		}
		logger.debug("Succesfull creating Requirement instance.");

		String serviceProviderURI = getContext().getServiceProviderUrl();
		String reqOslcResourceType = requirement.getRdfTypes()[0].toString();
		boolean isCreatingRequirement = (getContext().getDoCommand() == DoActivityEnum.CREATE);
		if (isCreatingRequirement) {// if processing create request
			// locate requirements factory 
			if (locateRequirmentFactory(serviceProviderURI, reqOslcResourceType) == false) {
				return ActivityConstants.EXE_FAIL;
			}
			logger.info("Succesfull locating Requirements factory.");
		}
		// locate the metadata for the rquirment type
		if (locateInstanceShape(serviceProviderURI, reqOslcResourceType) == false) {
			return ActivityConstants.EXE_FAIL;
		}
		logger.info("Succesfull locating requirement type metadata for \"" + reqTypeOptionVal + "\".");
		return ActivityConstants.EXE_SUCCESS;
	}
	@Override
	public void planNextActivity() {
		super.planNextActivity();
		LoadingTextActivity loadingTextActivity = new LoadingTextActivity();
		getSchedule().add(loadingTextActivity);
		logger.debug(LoggerHelper.LINE_TITLE+"to -> "+loadingTextActivity.getClass().getName());
	}

	private boolean locateInstanceShape(String serviceProviderURI,
			String reqOslcResourceType) {
		ResourceShape requirmentInstanceShape;
		boolean retVal = true;
		try {
			requirmentInstanceShape = ResourceShape.lookupRequirementsInstanceShapes(
					serviceProviderURI, OSLCConstants.OSLC_RM_V2,
					reqOslcResourceType, getContext().getJazzClient(),
					reqTypeOptionVal);
			getContext()
					.setRequirementInstanceShape(requirmentInstanceShape);
			requirement.setInstanceShape(requirmentInstanceShape.getAbout());
		} catch (ResourceNotFoundException e) {
			errorReportUndefinedRequirementType(reqOslcResourceType);
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

	private void errorReportUndefinedRequirementType(String reqOslcResourceType) {

		String[] definedRequirementTypes = getContext()
				.getInstanceShapesTitleStrArr();
		if (definedRequirementTypes == null) {
			definedRequirementTypes = getInstanceShapesNames(reqOslcResourceType);
		}
		StringBuffer reqTypesSB = new StringBuffer("");
		for (int i = 0; i < definedRequirementTypes.length; i++) {
			reqTypesSB.append("\"").append(definedRequirementTypes[i])
					.append("\"");
			if (i < definedRequirementTypes.length - 1) {
				reqTypesSB.append(", ");
			}
		}
		logger.error("Undefined requirement type: --"+reqTypeOptionFlag+"=\"" + reqTypeOptionVal + "\"\n"
				+ "Defined requirement types are: " + reqTypesSB.toString());
	}

	private String[] getInstanceShapesNames(String reqOslcResourceType) {
		JazzFormAuthClient client = getContext().getJazzClient();
		ArrayList<String> instanceShapesTitleArr = new ArrayList<String>();

		ClientResponse response = null;
		try {
			response = client.getResource(getContext().getServiceProviderUrl(),
					OSLCConstants.CT_RDF);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (OAuthException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		ServiceProvider serviceProvider = response
				.getEntity(ServiceProvider.class);

		if (serviceProvider != null) {
			for (Service service : serviceProvider.getServices()) {
				URI domain = service.getDomain();
				if (domain != null
						&& domain.toString().equals(OSLCConstants.OSLC_RM_V2)) {
					CreationFactory[] creationFactories = service
							.getCreationFactories();
					if (creationFactories != null
							&& creationFactories.length > 0) {
						for (CreationFactory creationFactory : creationFactories) {
							for (URI resourceType : creationFactory
									.getResourceTypes()) {
								if (resourceType.toString() != null
										&& resourceType.toString().equals(
												reqOslcResourceType)) {
									URI[] instanceShapes = creationFactory
											.getResourceShapes();
									if (instanceShapes != null) {
										for (URI typeURI : instanceShapes) {
											try {
												response = client.getResource(
														typeURI.toString(),
														OSLCConstants.CT_RDF);
											} catch (IOException e) {
												e.printStackTrace();
											} catch (OAuthException e) {
												e.printStackTrace();
											} catch (URISyntaxException e) {
												e.printStackTrace();
											}
											ResourceShape resourceShape = response
													.getEntity(ResourceShape.class);
											instanceShapesTitleArr
													.add(resourceShape
															.getTitle());
										}
									}
								}
							}
						}
					}
				}
			}
		}

		String[] instanceShapesTitleStrArr = new String[instanceShapesTitleArr
				.size()];
		instanceShapesTitleArr.toArray(instanceShapesTitleStrArr);
		getContext().setInstanceShapesTitleStrArr(instanceShapesTitleStrArr);
		return instanceShapesTitleStrArr;
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
			errorReportUndefinedRequirementType(reqOslcResourceType);
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
			logger.error("Undefined project area: --"+projectOptionFlag+"=\"" + projectOptionVal + "\"\n");
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
			logger.error("Undefined RRC server: --"+rmOptionFlag+"=\"" + rmOptionVal + "\"\n");
			logger.error("Failed connect to: " + rmOptionVal);
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
		if (client == null){
			logger.error("Failed create JazzFormAuthClient.");
			reportFailedLogin();
			return false;
		}
		logger.debug("Succesfull creating JazzFormAuthClient.");

		// STEP 3: Login in to Jazz Server
		try {
			if (client.formLogin() != HttpStatus.SC_OK) {
				return false;
			}
		} catch (JazzAuthFailedException e) {
			reportFailedLogin();
			e.printStackTrace();
			return false;
		} catch (JazzAuthErrorException e) {
			reportFailedLogin();
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private void reportFailedLogin() {
		logger.error("User '" + userOptionVal + "' failed login to "
				+ jtsOptionVal+
				"\n"+
				"JTS server: --"+jtsOptionFlag+"=\"" + jtsOptionVal + "\"\n"+
				"User      : --"+userOptionFlag+"=\"" + userOptionVal + "\"\n"+
				"Password  : --"+passwordOptionFlag+"=\"????????\"");
	}
}
