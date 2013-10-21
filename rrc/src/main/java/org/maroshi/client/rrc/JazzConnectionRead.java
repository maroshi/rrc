package org.maroshi.client.rrc;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import jena.cmdline.CmdLineUtils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.wink.client.ClientResponse;
import org.eclipse.lyo.client.exception.ResourceNotFoundException;
import org.eclipse.lyo.client.exception.RootServicesException;
import org.eclipse.lyo.client.oslc.OSLCConstants;
import org.eclipse.lyo.client.oslc.jazz.JazzFormAuthClient;
import org.eclipse.lyo.client.oslc.jazz.JazzRootServicesHelper;
import org.eclipse.lyo.client.oslc.resources.Requirement;
import org.eclipse.lyo.client.oslc.resources.RequirementCollection;
import org.eclipse.lyo.client.oslc.resources.RmConstants;
import org.eclipse.lyo.client.oslc.resources.RmUtil;
import org.eclipse.lyo.oslc4j.core.model.CreationFactory;
import org.eclipse.lyo.oslc4j.core.model.Link;
import org.eclipse.lyo.oslc4j.core.model.OslcMediaType;
import org.eclipse.lyo.oslc4j.core.model.ResourceShape;
import org.eclipse.lyo.oslc4j.core.model.Service;
import org.eclipse.lyo.oslc4j.core.model.ServiceProvider;
import org.maroshi.client.util.CliOptionsBuilder;
import org.maroshi.client.util.LoggerFactory;
import org.maroshi.client.util.LoggerHelper;
import org.maroshi.client.util.VersionLocator;

public class JazzConnectionRead {

	static void display(String msg) {
		System.out.println(msg);
	}

	static Logger logger = Logger.getLogger(JazzConnectionRead.class);

	public static void main(String[] args) {
		LoggerFactory.config();
		; // init logger

		CommandLine cmd = readCliArgs(args);
		if (cmd == null)
			return;

		String rmContextUrl = cmd.getOptionValue("conn.site.rm");
		String jtsContextUrl = cmd.getOptionValue("conn.site.jts");
		String user = cmd.getOptionValue("conn.user");
		String passwd = cmd.getOptionValue("conn.password");
		String projectArea = cmd.getOptionValue("conn.projectArea");

		JazzRootServicesHelper helper;

		try {
			// STEP 1: Initialize a Jazz rootservices helper and indicate we're
			// looking for the RequirementManagement catalog
			helper = new JazzRootServicesHelper(rmContextUrl,
					OSLCConstants.OSLC_RM_V2);

			// STEP 2: Create a new Form Auth client with the supplied
			// user/password
			// RRC is a fronting server, so need to use the initForm() signature
			// which allows passing of an authentication URL.
			// For RRC, use the JTS for the authorization URL
			JazzFormAuthClient client = helper.initFormClient(user, passwd,
					jtsContextUrl);

			// STEP 3: Login in to Jazz Server
			if (client.formLogin() != HttpStatus.SC_OK) {
				display("-- falied connection --");
				System.exit(1);
			}

			// STEP 4: Get the URL of the OSLC ChangeManagement catalog
			String catalogUrl = helper.getCatalogUrl();
			display("-- catalogUrl=" + catalogUrl); 
			// STEP 5: Find the OSLC Service Provider for the project area we
			// want to work with
			String serviceProviderUrl = client.lookupServiceProviderUrl(
					catalogUrl, projectArea);
			display("-- serviceProviderUrl=" + serviceProviderUrl);

			String systemReqURI = "https://jazz.net/sandbox01-rm/resources/_muLCIjKIEeOj1KtREAvjXQ";
			Requirement systemReq = new Requirement();
			ClientResponse getResponse = null;
			try {
				getResponse = client.getResource(systemReqURI,
						OslcMediaType.APPLICATION_RDF_XML);
				systemReq = getResponse.getEntity(Requirement.class);
				
				Map<QName, Object> extendedProperties = systemReq.getExtendedProperties();
				QName qName = new QName("https://jazz.net/sandbox01-rm/types/","_8E1FoTHjEeOj1KtREAvjXQ");
				display("qName="+qName.toString());
				
				Object property = extendedProperties.get(qName);
				if (property != null){
					String msg = property.getClass().getName();
					display("property.class="+msg);
					if (property instanceof java.net.URI){
						java.net.URI uriProperty = (java.net.URI)property;
						display("uriProperty="+uriProperty.toString());
					}
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			getResponse.consumeContent();

			display(systemReqURI.toString());
		} catch (RootServicesException re) { 
			logger.log(Level.FATAL,
					"Unable to access the Jazz rootservices document at: "
							+ rmContextUrl + "/rootservices", re);
		} catch (Exception e) {
			logger.log(Level.FATAL, e.getMessage(), e);
		}
	}

	public static CommandLine readCliArgs(String[] args) {
		HelpFormatter helpFormatter = new HelpFormatter(); // init usage message
		Options options = null;
		CommandLine cmd = null;
		try {
			String[] configArgsArr = CliOptionsBuilder
					.collectCliArgumentsAndOptions(args);

			// read command line option definitions from config file
			options = CliOptionsBuilder.getOptions();
			if (options != null && options.getOptions().size() > 0) {
				GnuParser gnuParser = new GnuParser();
				cmd = gnuParser.parse(options, configArgsArr);
				if (cmd == null)
					return null;

				if (logger.getEffectiveLevel().isGreaterOrEqual(Level.DEBUG)) {
					Option[] optionsArr = cmd.getOptions();
					logger.debug(LoggerHelper.LINE);
					logger.debug("Argument list from all sources in order of entry.");
					logger.debug(LoggerHelper.LINE_START);
					for (Option option : optionsArr) {
						logger.debug(option.getLongOpt() + "="
								+ option.getValue());
					}
					logger.debug(LoggerHelper.LINE_END);
				}
			}
			logCurrentVersion();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e1) {
			helpFormatter.printHelp("Demo 01", options, true);
			e1.printStackTrace();
		}
		return cmd;
	}

	public static void logCurrentVersion() {
		logger.info("");
		logger.info("");
		logger.info(LoggerHelper.DOUBLE_LINE);
		logger.info(LoggerHelper.DOUBLE_LINE);
		logger.info("rrc1 version information:");
		logger.info(VersionLocator.getVersion());
		logger.info(LoggerHelper.DOUBLE_LINE);
		logger.info(LoggerHelper.DOUBLE_LINE);
		logger.info("");
		logger.info("");
	}
}
