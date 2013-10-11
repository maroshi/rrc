package org.maroshi.client.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.Logger;

public class VersionLocator {
	static Logger logger = Logger.getLogger(VersionLocator.class);
	private static boolean isInitiated = false;
	private static String versionInfo = null;
	
	public static String getVersion(){
		init();
		return versionInfo;
	}

	public static void init() {
		if (isInitiated)
			return;
		isInitiated = true;
		
		boolean foundFile = false;
		File dir = new File(Msg.getString("app.init.configDir1"));
		if (!dir.exists()) {
			dir = new File(Msg.getString("app.init.configDir2"));
		}
		if (dir.exists()) {
			FileFilter fileFilter = new WildcardFileFilter("*."
					+ Msg.getString("app.version.fileExtension"));
			File[] files = dir.listFiles(fileFilter);
			if (files.length > 0){
				foundFile = true;
				String fileBaseName = FilenameUtils.getBaseName(files[0].getName());
				StringBuffer sb = new StringBuffer(fileBaseName);
				Properties properties = new Properties();
				FileReader fr = null;
				try {
					fr = new FileReader(files[0]);
					properties.load(fr);
					fr.close();
					String buildNumberStr = null;
					buildNumberStr = properties.getProperty(Msg.getString("app.version.buildNumber.propertyName"));
					if (buildNumberStr == null){
						logger.error("Failed locate "+Msg.getString("app.version.buildNumber.propertyName")+" property in file '"+files[0].getName()+"'");
						buildNumberStr = "undefined";
					}
					sb.append(" build# ").append(buildNumberStr);
					versionInfo = sb.toString();
				} catch (FileNotFoundException e) {
					logger.error("Failed locate file  '"+files[0].getName()+"'");
					e.printStackTrace();
				} catch (IOException e) {
					logger.error("Failed read file  '"+files[0].getName()+"'");
					e.printStackTrace();
				}
			}
		} 
		if (!foundFile){
			logger.error("Failed locate application identifier file *."+Msg.getString("app.version.fileExtension"));
		}
	}
}
