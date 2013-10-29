package org.maroshi.client.activity;

import java.io.IOException;
import java.net.URISyntaxException;

import net.oauth.OAuthException;

import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.apache.wink.client.ClientResponse;
import org.eclipse.lyo.client.oslc.jazz.JazzFormAuthClient;
import org.eclipse.lyo.client.oslc.resources.Requirement;
import org.eclipse.lyo.oslc4j.core.model.OslcMediaType;

public class SubmitUpdateRequirementActivity extends AbstractActivity {
	static Logger logger = Logger
			.getLogger(SubmitUpdateRequirementActivity.class);

	@Override
	public String execute() {
		super.execute();
		String retVal = ActivityConstants.EXE_SUCCESS;
		ClientResponse updateResponse = null;
		Context context = getContext();
		JazzFormAuthClient client = context.getJazzClient();
		String targetURI = context.getModifySubjectURI().toString();
		Requirement targetReq = context.getRequirement();
		
		try {
		// Get the eTAG, we need it to update
		String etag = context.getETag();
		updateResponse = context.getJazzClient().updateResource(
				targetURI, 
				targetReq, 
				OslcMediaType.APPLICATION_RDF_XML,
				OslcMediaType.APPLICATION_RDF_XML,
				etag);
		updateResponse.consumeContent();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (OAuthException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		if (updateResponse.getStatusCode() != HttpStatus.SC_OK) {
			logger.error("Failed update requirement submission, with server return code: "
					+ updateResponse.getStatusCode() + " "+ updateResponse.getMessage());
			return ActivityConstants.EXE_FAIL;
		}

		logger.info("Succesful requirement update submission : "+targetURI);
		return retVal;
	}
}
