package org.maroshi.client.activity;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.maroshi.client.util.LoggerHelper;
import org.maroshi.client.util.Msg;

public class DoActivity extends AbstractActivity {
	static Logger logger = Logger.getLogger(DoActivity.class);
	static final String doOptionFlag = "app.do";

	public enum DoActivityEnum {
		CREATE, READ, UPDATE, DELETE, UNDEFINED
	};

	private DoActivityEnum doActivityEnum;
	String optionVal = null;

	@Override
	public void init() {
		super.init();
		optionVal = readOptionValue(doOptionFlag);
		if (optionVal.length() == 0) {
			logger.error("Missing option value for: --" + doOptionFlag);
			logger.error("Acceptable option value: create | read | update | delete");
		}
	}

	@Override
	public String execute() {
		String retVal = ActivityConstants.EXE_SUCCESS;
		if (optionVal.length() == 0)
			return ActivityConstants.EXE_FAIL;

		super.execute();
		if (optionVal.equalsIgnoreCase("create")) {
			doActivityEnum = DoActivityEnum.CREATE;
		} else if (optionVal.equalsIgnoreCase("read")) {
			doActivityEnum = DoActivityEnum.READ;
		} else if (optionVal.equalsIgnoreCase("update")) {
			doActivityEnum = DoActivityEnum.UPDATE;
		} else if (optionVal.equalsIgnoreCase("delete")) {
			doActivityEnum = DoActivityEnum.DELETE;
		} else {
			doActivityEnum = DoActivityEnum.UNDEFINED;
			logger.error("Illegal option value: --" + doOptionFlag + "="
					+ optionVal);
			logger.error("Acceptable option value: create | read | update | delete");
			retVal = ActivityConstants.EXE_FAIL;
		}
		getContext().setDoCommand(doActivityEnum);
		logger.debug(LoggerHelper.LINE_TITLE+"  "+optionVal);
		return retVal;
	}

	@Override
	public void planNextActivity() {
		if (optionVal.length() == 0)
			return;
		if (doActivityEnum == DoActivityEnum.UNDEFINED)
			return;

		super.planNextActivity();
		if (doActivityEnum != DoActivityEnum.READ ) {
			if (hasOption(ConnectingToJazzActivty.reqTypeOptionFlag)){
				nextActivityIs(new ConnectingToJazzActivty());
			}else {
				logger.error("Missing option value for: --" + ConnectingToJazzActivty.reqTypeOptionFlag);
				
			}
		}
	}

}