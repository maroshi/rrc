package org.maroshi.client.activity;

import org.apache.commons.cli.HelpFormatter;
import org.apache.log4j.Logger;
import org.maroshi.client.util.LoggerFactory;
import org.maroshi.client.util.Msg;
import org.maroshi.client.util.VersionLocator;

public class LogoActivity extends AbstractActivity{
	static Logger logger = Logger.getLogger(LogoActivity.class);
	static final String versionOptionFlag = Msg.getString("app.init.optionVersionFlagLong");
	static final String  noLogoOptionFlag = Msg.getString("app.init.optionNoLogoFlagLong");
	
	@Override
	public String execute() {
		super.execute();
		String logo = VersionLocator.getVersion();
		StringBuffer actualCommandLine = new StringBuffer("rrc1 ");
		// display version only
		if (hasOption(versionOptionFlag)){
			logger.info("version: "+logo);
			logger.info("");
			return ActivityConstants.EXE_SUCCESS;			
		}
		// skip is no log option is present
		if (hasOption(noLogoOptionFlag)){
			return ActivityConstants.EXE_SUCCESS;			
		}
		
		// display logo and actual command line arguments
		String[] argsArr = getContext().getCommandLineArgs();
		for (int i = 0; i < argsArr.length; i++) {
			String cmd = argsArr[i];
			int valueIndex = cmd.indexOf('=');
			if (valueIndex > 1){
				valueIndex++;
				actualCommandLine.append(cmd.substring(0, valueIndex)).append("\"");
				actualCommandLine.append(cmd.substring(valueIndex)).append("\" ");
			}else{				
				actualCommandLine.append(cmd).append(" ");			
			}
			
		}
		logger.info(logo);
		logger.info(actualCommandLine);
		logger.info("");
			
		return ActivityConstants.EXE_SUCCESS;
	}
	
	@Override
	public void planNextActivity() {
		super.planNextActivity();
		if (hasOption(DoActivity.doOptionFlag)){
			DoActivity doActivity = new DoActivity();
			getSchedule().add(doActivity);
			logger.debug(LoggerFactory.LINE_TITLE+"to -> "+doActivity.getClass().getName());
		}
		else{
			logger.error("Missing required option --"+DoActivity.doOptionFlag);
		}
	}

}
