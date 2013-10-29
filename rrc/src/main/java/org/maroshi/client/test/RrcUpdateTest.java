package org.maroshi.client.test;

import java.net.URI;
import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;

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
import org.eclipse.lyo.client.oslc.resources.Requirement;
import org.eclipse.lyo.client.oslc.resources.RmUtil;
import org.eclipse.lyo.oslc4j.core.model.Link;
import org.eclipse.lyo.oslc4j.core.model.OslcMediaType;
import org.eclipse.lyo.oslc4j.core.model.ResourceShape;
import org.maroshi.client.activity.ActivityConstants;
import org.maroshi.client.util.LoggerFactory;
import org.maroshi.client.util.LoggerHelper;

public class RrcUpdateTest {

	private static final Logger logger = Logger.getLogger(RrcUpdateTest.class
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

		String webContextUrl = cmd.getOptionValue("url");// https://jazz.net/sandbox01-rm
		String user = cmd.getOptionValue("user");// maroshi
		String passwd = cmd.getOptionValue("password");// dmtg0613
		String projectArea = cmd.getOptionValue("project");// dmtg01 (Requirements Management)

		try {
			JazzRootServicesHelper helper = new JazzRootServicesHelper(webContextUrl, OSLCConstants.OSLC_RM_V2);
			String authUrl = webContextUrl.replaceFirst("-rm", "-jts");
			JazzFormAuthClient client = helper.initFormClient(user, passwd,	authUrl);
			if (client.formLogin() == HttpStatus.SC_OK) {

				final String reqUriStr = "https://jazz.net/sandbox01-rm/resources/_lno_gD4bEeOtMNUT1fy5zw";
				// GET request for the artifact
				logger.info("--------------------------------");
				logger.info("--- GET request for the artifact");
				ClientResponse response = client.getResource(reqUriStr,	OSLCConstants.CT_RDF);
				Requirement subjReq = response.getEntity(Requirement.class);
				String etag = response.getHeaders().getFirst(OSLCConstants.ETAG);
				response.consumeContent();

				// PUT request for the artifact
				if (response.getStatusCode() == HttpStatus.SC_OK) {
					/*
					 * modify subject requirement
					 */
					final String titleStr = "Test 31";
					final String targetURLstr = "https://jazz.net/sandbox01-rm/resources/_w2C80SVdEeORZ48lLR6jXA";
					// indication for modified subject
					subjReq.setTitle(titleStr);

					Map<QName, Object> extPropMap = subjReq.getExtendedProperties();
					URI targetURI = new URI(targetURLstr);
					QName linkQname = new QName("http://www.ibm.com/xmlns/rdm/types/", "Link");
					// get all Link references
					Object linksObj = extPropMap.get(linkQname);
					if (linksObj != null) {
						// remove all Link references
						extPropMap.remove(linkQname);
					}
					// add one Link reference
					 extPropMap.put(linkQname, targetURI);
					// add one Decomposes reference (watch PUT request)
					 subjReq.addDecomposes(new Link(targetURI));

					 /*
					  * submit subject requirement
					  */
					logger.info("--------------------------------");
					logger.info("--- PUT request for the artifact");
					response = client.updateResource(reqUriStr, 
							subjReq,OslcMediaType.APPLICATION_RDF_XML, 
							OslcMediaType.APPLICATION_RDF_XML, 
							etag);
					response.consumeContent();
				}

				// GET request for the updated artifact
				if (response.getStatusCode() == HttpStatus.SC_OK) {
					logger.info("----------------------------------------");
					logger.info("--- GET request for the updated artifact");
					response = client.getResource(reqUriStr, OSLCConstants.CT_RDF);
					subjReq = response.getEntity(Requirement.class);
					response.consumeContent();
					logger.info("--- Successful completion");
					logger.info("----------------------------------------");
				}
			}
		} catch (RootServicesException re) {
			re.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
