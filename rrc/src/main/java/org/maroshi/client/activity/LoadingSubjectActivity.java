package org.maroshi.client.activity;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import net.oauth.OAuthException;

import org.apache.log4j.Logger;
import org.apache.wink.client.ClientResponse;
import org.eclipse.lyo.client.oslc.OSLCConstants;
import org.eclipse.lyo.client.oslc.resources.Requirement;
import org.eclipse.lyo.oslc4j.core.model.Link;

public class LoadingSubjectActivity extends AbstractActivity {

	static Logger logger = Logger.getLogger(LoadingSubjectActivity.class);

	static final String reqUriOptionFlag = "app.reqURI";
	private String reqUriStr = null;
	private URI reqUri = null;
	private boolean noUriOption = false;

	@Override
	public void init() {
		super.init();
		if (isValidOptionValue(reqUriOptionFlag)) {
			reqUriStr = readOptionValue(reqUriOptionFlag);
			reqUri = linkStrToURI(reqUriStr);
			getContext().setModifySubjectURI(reqUri);
		} else
			noUriOption = true;
	}
	@SuppressWarnings("unchecked")
	@Override
	public String execute() {
		String retVal = ActivityConstants.EXE_SUCCESS;
		if (noUriOption)
			return ActivityConstants.EXE_FAIL;
		
		super.execute();
		ClientResponse response; 
		try {
			response = getContext().getJazzClient().getResource(reqUriStr, OSLCConstants.CT_RDF);
			Requirement subjectRequiremnt = response
					.getEntity(Requirement.class);
			getContext().setRequirement(subjectRequiremnt);
			String etag = response.getHeaders().getFirst(OSLCConstants.ETAG);
			getContext().seteTag(etag);
			
			Map<QName, Object> reqExtProp = subjectRequiremnt.getExtendedProperties();
			Iterator<Entry<QName, Object>> extPropIter = reqExtProp.entrySet().iterator();
			int i = 0;
			while (extPropIter.hasNext()) {
				Map.Entry<javax.xml.namespace.QName, java.lang.Object> extPropEntry = (Map.Entry<javax.xml.namespace.QName, java.lang.Object>) extPropIter
						.next();
				QName key = extPropEntry.getKey();
				logger.debug("Entry["+i+"] key= "+key.toString());
				Object val = extPropEntry.getValue();
				logger.debug("Entry["+i+"] val= "+val.getClass().getName());
				i++;
			}
			subjectRequiremnt.setTitle("Maroshi 09");
			
			// get the custom link array
			String linkURLstr = "https://jazz.net/sandbox02-rm/resources/_vNguIjwZEeODFeaB_T95kg";
			URI instanceShapePropURI = getContext().getRequirementInstanceShape().getPropertyUri("Custom Link");
			QName qName = URItoQName(instanceShapePropURI);
			logger.debug("qName="+qName.toString());
			reqExtProp.put(qName, new Link(new URI(linkURLstr)));
			logger.debug("--- done ----");
			
//			response.consumeContent();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (OAuthException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		return retVal;
	}
	@Override
	public void planNextActivity() {
		super.planNextActivity();
		nextActivityIs(new SubmitUpdateRequirementActivity());
	}

	private URI linkStrToURI(String linkURIstr) {
		URI retVal = null;
		try {
			retVal = new URI(linkURIstr);
		} catch (URISyntaxException e) {
			logger.error("Illegla URI syntax for option: " + reqUriOptionFlag);
		}
		return retVal;
	}
}
