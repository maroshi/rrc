package org.maroshi.client.activity;

import org.apache.log4j.Logger;
import org.maroshi.client.util.LoggerFactory;

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
		logger.debug(LoggerFactory.LINE);
		logger.debug(LoggerFactory.LINE_TITLE + "Init "
				+ this.getClass().getName());
		logger.debug(LoggerFactory.LINE_TITLE);
	}

	public String execute() {
		logger.debug(LoggerFactory.LINE_TITLE);
		logger.debug(LoggerFactory.LINE_TITLE + "Execute "
				+ this.getClass().getName());
		logger.debug(LoggerFactory.LINE_TITLE);
		return ActivityConstants.EXE_SUCCESS;
	}

	public void planNextActivity() {
		if (context.getExecutionResult() == ActivityConstants.EXE_FAIL)
			return;
		
		logger.debug(LoggerFactory.LINE_TITLE);
		logger.debug(LoggerFactory.LINE_TITLE + "Plan Next Activity "
				+ this.getClass().getName());
		logger.debug(LoggerFactory.LINE_TITLE);
	}
	public void finish() {
		if (context.getExecutionResult() == ActivityConstants.EXE_FAIL)
			return;
		
		logger.debug(LoggerFactory.LINE_TITLE);
		logger.debug(LoggerFactory.LINE_TITLE + "Finish "
				+ this.getClass().getName());
		logger.debug(LoggerFactory.LINE);
		logger.debug(LoggerFactory.NEW_LINE);

	}


	public Context getContext() {
		return context;
	}

	public Schedule getSchedule() {
		return schedule;
	}

	protected boolean isValidOptionValue(String optFlg) {
		boolean retVal = true;
		String optionVal = getContext().getCommandLine().getOptionValue(optFlg);
		if (optionVal.length() == 0) {
			logger.error("Missing option value for: --" + optFlg);
			retVal = false;
		}
		return retVal;
	}
}
