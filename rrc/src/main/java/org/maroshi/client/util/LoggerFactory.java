package org.maroshi.client.util;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.EnhancedPatternLayout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class LoggerFactory {
	static private String CONSOLE_LAYOUT_PATTERN = "%-5p %d{HH:MM:ss} %c{2}: %m%n";
	static final public  String LINE =        "--------------------------------------------------------------------------------";
	static final public  String DOUBLE_LINE = "================================================================================";
	static final public  String LINE_START =  "---START------------------------------------------------------------------------";
	static final public  String LINE_END =    "---END--------------------------------------------------------------------------";
	static private boolean isInitiated = false;
	
	public static void init() {
		if (isInitiated)
			return;
		
		ConsoleAppender consoleAppender = new ConsoleAppender();
		if (consoleAppender != null){
			consoleAppender.setName("default.console");
			EnhancedPatternLayout consoleLayout = new EnhancedPatternLayout();
			if (consoleLayout != null){
				consoleLayout.setConversionPattern(CONSOLE_LAYOUT_PATTERN);
				consoleAppender.setLayout(consoleLayout);
			}
			consoleAppender.activateOptions();
		}
		BasicConfigurator.configure(consoleAppender);
		isInitiated = true;
	}
	
	public static void config(){
		init();
		try {
			File f = ResourceLocator.locateConfigFile(Msg.getString("app.init.loggerFile"));
			if (f.exists()){
				PropertyConfigurator.configure(f.getCanonicalPath());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
}
