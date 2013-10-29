package org.maroshi.client.activity;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

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
			response = getContext().getJazzClient().getResource(reqUriStr,
					OSLCConstants.CT_RDF);
			Requirement subjReq = response
					.getEntity(Requirement.class);
			String etag = response.getHeaders().getFirst(OSLCConstants.ETAG);
			response.consumeContent();

			getContext().setRequirement(subjReq);
			getContext().setETag(etag);

			Map<QName, Object> reqExtProp = subjReq
					.getExtendedProperties();
			// get the target link array
			String[] linkURLstr = {
					"https://jazz.net/sandbox02-rm/resources/_vNguIjwZEeODFeaB_T95kg",
					"https://jazz.net/sandbox02-rm/resources/_qQl9oTwYEeODFeaB_T95kg" };
			URI instanceShapePropURI = getContext()
					.getRequirementInstanceShape().getPropertyUri("Link");
			QName qName = URItoQName(instanceShapePropURI);
			logger.debug("    qName=" + qName.toString());
			HashSet<Link> toLinkSet = new HashSet<Link>();
			ArrayList<Link> linksArr = null;
			Object propVAlue = reqExtProp.get(qName);

//			linksArr = new ArrayList<Link>();
//			linksArr.add(new Link(new URI(linkURLstr[0]), "link 1"));
//			linksArr.add(new Link(new URI(linkURLstr[1]), "link 2"));
			
//			reqExtProp.put(qName, linksArr);
//			reqExtProp.remove(qName);
			
//			 subjectRequiremnt.addSpecifies(new Link(new URI(linkURLstr[0]),"spec 1"));
//			 subjectRequiremnt.addSpecifies(new Link(new URI(linkURLstr[1]),"spec 2"));
//			 subjectRequiremnt.setSpecifies(null);
//			Link anonLink = new Link(new URI(linkURLstr[0]), instanceShapePropURI.toString());
//			subjectRequiremnt.addAnonymousLink(anonLink);
			subjReq.setTitle("Test 09");

//			response.consumeContent();

			logger.debug("--- done ----");
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
