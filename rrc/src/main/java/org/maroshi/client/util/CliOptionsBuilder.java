package org.maroshi.client.util;

import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.maroshi.client.rrc.JazzConnection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CliOptionsBuilder {
	private static Options options = null;
	static Logger logger = Logger.getLogger(CliOptionsBuilder.class);

	public static Options getOptions() {
		if (options != null)
			return options;

		options = new Options();
		try {
			File f = ResourceLocator.locateConfigFile(Msg
					.getString("app.init.optionsXmlFile"));
			if (!f.exists())
				return null;

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(f);
			doc.getDocumentElement().normalize();
//			logger.debug("Root element "
//					+ doc.getDocumentElement().getNodeName());
			NodeList nodeLst = doc.getElementsByTagName("option");
			logger.debug(LoggerHelper.DOUBLE_LINE);

			logger.debug("Information for each option definition");

			for (int s = 0; s < nodeLst.getLength(); s++) {

				Node fstNode = nodeLst.item(s);

				if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
					logger.debug(LoggerHelper.LINE_START);

					String shortName = extractElementValue(fstNode, "short");
					if (shortName != null && shortName.length() > 0) {
						OptionBuilder.withArgName(shortName);
					}
					String longName = extractElementValue(fstNode, "long");
					if (longName != null && longName.length() > 0) {
						OptionBuilder.withLongOpt(longName);
					}
					String description = extractElementValue(fstNode,
							"description");
					if (description != null && description.length() > 0) {
						OptionBuilder.withDescription(description);
					}
					String argName = extractElementValue(fstNode, "argName");
					if (description != null && description.length() > 0) {
						OptionBuilder.withArgName(argName);
					}
					String hasArgs = extractElementValue(fstNode, "hasArgs");
					if (hasArgs != null && hasArgs.length() > 0
							&& hasArgs.equalsIgnoreCase("true")) {
						OptionBuilder.hasArgs();
					}
					String required = extractElementValue(fstNode, "required");
					if (required != null && required.length() > 0
							&& required.equalsIgnoreCase("true")) {
						OptionBuilder.isRequired();
					}
					Option option = OptionBuilder.create(shortName);
					if (option != null) {
						logger.debug("option " + s + ": " + option.toString());
						options.addOption(option);
					}
					logger.debug(LoggerHelper.LINE_END);
				}
			}
			logger.debug(LoggerHelper.DOUBLE_LINE);

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return null;
		} catch (SAXException e) {
			e.printStackTrace();
			return null;
		}
		return options;
	}

	private static String extractElementValue(Node fstNode, String elementName) {
		Element fstElmnt = (Element) fstNode;
		String fstElmntValue = null;
		NodeList fstNmElmntLst = fstElmnt.getElementsByTagName(elementName);
		if (fstNmElmntLst.getLength() > 0) {
			Element fstNmElmnt = (Element) fstNmElmntLst.item(0);
			NodeList fstNm = fstNmElmnt.getChildNodes();
			if (fstNm.getLength() > 0) {
				fstElmntValue = ((Node) fstNm.item(0)).getNodeValue();
				logger.debug(elementName + " : " + fstElmntValue);
			}
		}
		return fstElmntValue;
	}

	public static void main(String[] args) {
		getOptions();
	}

	public static void requestUserNameOption(ArrayList<String> configArgsList) {
		String shortUserFlag = Msg.getString("app.init.optionUserFlag") + "=";
		String longUserFlage = Msg.getString("app.init.optionUserFlagLong")
				+ "=";
		boolean foundFlag = false;
		for (String configArg : configArgsList) {
			if (configArg.startsWith(shortUserFlag)
					|| configArg.startsWith(longUserFlage)) {
				foundFlag = true;
				break; // break from the foreach loop
			}
		}
		if (!foundFlag) { // flag not found try to get it with the console
			logger.warn("Missing " + Msg.getString("app.init.optionUserFlag")
					+ " : No user name in arguments");

			Console console = System.console();
			if (console == null) {
				logger.fatal("No console interface for user input. Aborting!");
				System.exit(1);
			}
			String userFlag = null;
			userFlag = console.readLine("user : ");
			logger.debug(longUserFlage + userFlag);
			configArgsList.add(longUserFlage + userFlag);
		}
	}

	public static void requestPasswordOption(ArrayList<String> configArgsList) {
		String shortPasswordFlag = Msg.getString("app.init.optionPasswordFlag")
				+ "=";
		String longPasswordFlag = Msg
				.getString("app.init.optionPasswordFlagLong") + "=";
		boolean foundFlag = false;
		for (String configArg : configArgsList) {
			if (configArg.startsWith(shortPasswordFlag)
					|| configArg.startsWith(longPasswordFlag)) {
				foundFlag = true;
				break; // break from the foreach loop
			}
		}
		if (!foundFlag) { // flag not found try to get it with the console
			logger.warn("Missing "
					+ Msg.getString("app.init.optionPasswordFlag")
					+ " :No password in arguments");

			Console console = System.console();
			if (console == null) {
				logger.fatal("No console interface for password input. Aborting");
				System.exit(1);
			}
			char[] passwordFlag = new char[20];
			passwordFlag = console.readPassword("Password : ");
			logger.debug(longPasswordFlag + passwordFlag.toString());
			configArgsList.add(longPasswordFlag + passwordFlag.toString());
		}
	}

	public static String[] collectCliArgumentsAndOptions(String[] args)
			throws IOException, FileNotFoundException {
		// add the command line args
		ArrayList<String> configArgsList = new ArrayList<String>();
		for (int r = 0; r < args.length; r++)
			configArgsList.add(args[r]);

		// load args from option files contained in the command line
		// recursive function
		ResourceLocator.readAllCliOptions(configArgsList);

		// read the args from the config file
		File f = ResourceLocator.locateConfigFile(Msg
				.getString("app.init.configFile"));
		if (f.exists()) {
			ResourceLocator.readCliOptionsFromConfigFile(f, configArgsList);
		}

		// read user from console or UI if not specified yet.
		requestUserNameOption(configArgsList);
		// read password from console
		requestPasswordOption(configArgsList);

		// convert the list to array
		String[] configArgsArr = new String[configArgsList.size()];
		configArgsList.toArray(configArgsArr);
		return configArgsArr;
	}
}
