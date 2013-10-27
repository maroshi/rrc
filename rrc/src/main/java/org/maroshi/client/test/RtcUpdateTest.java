package org.maroshi.client.test;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.apache.wink.client.ClientResponse;
import org.eclipse.lyo.client.exception.RootServicesException;
import org.eclipse.lyo.client.oslc.OSLCConstants;
import org.eclipse.lyo.client.oslc.jazz.JazzFormAuthClient;
import org.eclipse.lyo.client.oslc.jazz.JazzRootServicesHelper;
import org.eclipse.lyo.oslc4j.core.model.AnyResource;
import org.maroshi.client.util.LoggerFactory;

public class RtcUpdateTest {

	private static final Logger logger = Logger.getLogger(RtcUpdateTest.class
			.getName());

	public static void main(String[] args) throws ParseException {
		LoggerFactory.config();

		Options options = new Options();
		options.addOption("url", true, "url");
		options.addOption("user", true, "user ID");
		options.addOption("password", true, "password");
		options.addOption("project", true, "project area");
		CommandLineParser cliParser = new GnuParser();
		CommandLine cmd = cliParser.parse(options, args);

		String webContextUrl = cmd.getOptionValue("url");// https://jazz.net/sandbox02-ccm
		String user = cmd.getOptionValue("user");// maroshi
		String passwd = cmd.getOptionValue("password");// dmtg0613
		String projectArea = cmd.getOptionValue("project");// dmtg02 (Change and Configuration Management)

		try {
			JazzRootServicesHelper helper = new JazzRootServicesHelper(webContextUrl, OSLCConstants.OSLC_CM_V2);
			String authUrl = webContextUrl.replaceFirst("-ccm", "-jts");
			JazzFormAuthClient client = helper.initFormClient(user, passwd,	authUrl);
			if (client.formLogin() == HttpStatus.SC_OK) {

				final String wiUriStr = "https://jazz.net/sandbox02-ccm/resource/itemName/com.ibm.team.workitem.WorkItem/2069";
				// GET request for the artifact
				logger.info("--------------------------------");
				logger.info("--- GET request for the artifact");
				ClientResponse response = client.getResource(wiUriStr,	OSLCConstants.CT_RDF);
				AnyResource subjWi = response.getEntity(AnyResource.class);
				response.consumeContent();
			}
		} catch (RootServicesException re) {
			re.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
