package org.maroshi.client.activity;

import java.io.IOException;
import java.net.URISyntaxException;

import net.oauth.OAuthException;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.apache.wink.client.ClientResponse;
import org.eclipse.lyo.oslc4j.core.model.OslcMediaType;
import org.maroshi.client.rrc.JazzConnection;

public class SubmitNewRequirementActivity extends AbstractActivity {
	static Logger logger = Logger.getLogger(SubmitNewRequirementActivity.class);

	@Override
	public String execute() {
		super.execute();
		String retVal = ActivityConstants.EXE_SUCCESS;
		ClientResponse creationResponse = null;
		Context context = getContext();
		try {
			creationResponse = context.getJazzClient().createResource(
					context.getRequirementFactoryUrl(),
					context.getRequirement(),
					OslcMediaType.APPLICATION_RDF_XML,
					OslcMediaType.APPLICATION_RDF_XML);
		} catch (IOException e) {
			logger.error("Failed new requirement submission with IO Exception");
			e.printStackTrace();
			return ActivityConstants.EXE_FAIL;
		} catch (URISyntaxException e) {
			logger.error("Failed new requirement submission with URI Syntax Exception");
			e.printStackTrace();
			return ActivityConstants.EXE_FAIL;
		} catch (OAuthException e) {
			logger.error("Failed new requirement submission with Authorization Exception");
			e.printStackTrace();
			return ActivityConstants.EXE_FAIL;
		}

		if (creationResponse.getStatusCode() != HttpStatus.SC_CREATED) {
			logger.error("Failed new requirement submission, with server return code: "
					+ creationResponse.getStatusCode());
			return ActivityConstants.EXE_FAIL;
		}

		String req01URL = creationResponse.getHeaders().getFirst(
				HttpHeaders.LOCATION);
		creationResponse.consumeContent();
		logger.info("Succesful new requirement submission : "+req01URL);
		return retVal;
	}
}
