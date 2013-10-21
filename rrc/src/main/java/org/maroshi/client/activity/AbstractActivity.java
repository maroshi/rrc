package org.maroshi.client.activity;

import org.apache.log4j.Logger;
import org.maroshi.client.util.LoggerHelper;

public abstract class AbstractActivity {
	static Logger logger = Logger.getLogger(AbstractActivity.class);

	private Context context = Context.instance();
	private Schedule schedule = Schedule.instance();
	
	public void process(){
		init();
		context.setExecutionResult(execute());
		planNextActivity();
		finish();
	}

	public void init() {
		logger.debug(LoggerHelper.LINE);
		logger.debug(LoggerHelper.LINE_TITLE + "Init "
				+ this.getClass().getName());
		logger.debug(LoggerHelper.LINE_TITLE);
	}

	public String execute() {
		logger.debug(LoggerHelper.LINE_TITLE);
		logger.debug(LoggerHelper.LINE_TITLE + "Execute "
				+ this.getClass().getName());
		logger.debug(LoggerHelper.LINE_TITLE);
		return ActivityConstants.EXE_SUCCESS;
	}

	public void planNextActivity() {
		if (context.getExecutionResult() == ActivityConstants.EXE_FAIL)
			return;
		
		logger.debug(LoggerHelper.LINE_TITLE);
		logger.debug(LoggerHelper.LINE_TITLE + "Plan Next Activity "
				+ this.getClass().getName());
		logger.debug(LoggerHelper.LINE_TITLE);
	}
	public void finish() {
		if (context.getExecutionResult() == ActivityConstants.EXE_FAIL)
			return;
		
		logger.debug(LoggerHelper.LINE_TITLE);
		logger.debug(LoggerHelper.LINE_TITLE + "Finish "
				+ this.getClass().getName());
		logger.debug(LoggerHelper.LINE);
		logger.debug(LoggerHelper.NEW_LINE);

	}


	public Context getContext() {
		return context;
	}

	public Schedule getSchedule() {
		return schedule;
	}

	protected boolean isValidOptionValue(String optFlg) {
		boolean retVal = true;
		String optionVal = readOptionValue(optFlg);
		if (optionVal == null)
			return false;
		if (optionVal.length() == 0) {
			logger.error("Missing option value for: --" + optFlg);
			retVal = false;
		}
		return retVal;
	}
	protected String readOptionValue(String optFlg) {
		String valueStr = getContext().getCommandLine().getOptionValue(optFlg);
		if (valueStr == null)
			return null;
		return valueStr.trim();
	}
	protected boolean hasOption(String optFlag){
		return context.getCommandLine().hasOption(optFlag);
	}
}
