package org.maroshi.client.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.log4j.Logger;


public class ResourceLocator { 
	static Logger logger = Logger.getLogger(ResourceLocator.class);
	
	public static File locateConfigFile(String fileName) throws IOException {
		StringBuffer errMsg = new StringBuffer("Failed find config file:\n");
		File f = new File(fileName); //$NON-NLS-1$ //$NON-NLS-2$
		if (f.isAbsolute()){ // deal with absolute path
			if (!f.exists()) {
				errMsg.append(f.getCanonicalPath()).append("\n");
				logger.error(errMsg); //$NON-NLS-1$
				return f;
			}
			System.out.println("Located config file : " + f.getCanonicalPath());
			return f;
		}
		// deal with relative path, search in the following directories : config/* , ../config , . 
		f = new File(
				Msg.getString("app.init.configDir1"), fileName); //$NON-NLS-1$ //$NON-NLS-2$
		if (!f.exists()) { 
			errMsg.append(f.getCanonicalPath()).append("\n");
			f = new File(
					Msg.getString("app.init.configDir2"), fileName); //$NON-NLS-1$ //$NON-NLS-2$
			if (!f.exists()) {
				errMsg.append(f.getCanonicalPath()).append("\n");
				f = new File(fileName); //$NON-NLS-1$
				if (!f.exists()) {
					errMsg.append(f.getCanonicalPath()).append("\n");
					logger.error(errMsg); //$NON-NLS-1$
					return f;
				}
			}
		} else {
			logger.info("Located config file : " + f.getCanonicalPath());
		}
		return f;
	}

	public static ArrayList<String> readCliOptionsFromConfigFile(File f, ArrayList<String> configArgsList)
			throws FileNotFoundException, IOException {
		BufferedReader reader = new BufferedReader(new FileReader(f));
		if (configArgsList == null)
			configArgsList = new ArrayList<String>();
		String currLine;
		while(null != (currLine = reader.readLine())){
			String trimmedCurrLine = currLine.trim();
			if (trimmedCurrLine.length() == 0)
				continue;// ignore empty lines
			if (trimmedCurrLine.indexOf('#') == 0)
				continue;// ignore # comments
			
			// each line is long option with or without -- prefix
			if (trimmedCurrLine.startsWith("--")){
				configArgsList.add(currLine);
			}else {
				configArgsList.add("--"+currLine);				
			}
		}
		reader.close();
		return configArgsList;
	}

	public static void readAllCliOptions(
			ArrayList<String> configArgsList) throws IOException,
			FileNotFoundException {
		String fileOptionFlag = Msg.getString("app.init.optionFileFlag");
		String fileOptionFlagLong = Msg
				.getString("app.init.optionFileFlagLong");
		String fileOptionValue = null;
		String trimmedArgLine = null;
		ArrayList<String> fileNamesList = new ArrayList<String>();
	
		for (int cr = 0; cr < configArgsList.size(); cr++) {			
			String argLine = configArgsList.get(cr);
			trimmedArgLine = argLine.trim();
			fileNamesList.clear();
			if (trimmedArgLine.startsWith(fileOptionFlagLong)
					|| trimmedArgLine.startsWith(fileOptionFlag)) {
				int optionValuePos = trimmedArgLine.indexOf('=') + 1;
				if (optionValuePos == 0)
					continue; // there is no value in this line
				fileOptionValue = trimmedArgLine.substring(optionValuePos);
				if (fileOptionValue == null
						|| fileOptionValue.length() == 0)
					continue; // there is no value in this line
	
				// parse fileOptionValue for file names comma delimited
				fileNamesList.addAll(Arrays.asList(fileOptionValue
						.split(",")));
				if (fileNamesList.size() > 0) {
					for (String fileName : fileNamesList) {
						// read the args from the config file
						File f = locateConfigFile(fileName
								.trim());
						if (f.exists())
							readCliOptionsFromConfigFile(f,
									configArgsList);
					}// END for: read each file
				}
			}// END if: a line with -f or --optn.file
		}// END for: every line in the array, the array appends while searching
	}

}
