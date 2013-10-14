package org.maroshi.client.activity;

import java.util.ArrayList;

import org.apache.log4j.Logger;

public class Schedule extends ArrayList<AbstractActivity> {
	static Logger logger = Logger.getLogger(Schedule.class);
	static boolean isInitialized = false;
	private static Schedule instance = null;

	public static Schedule instance() {
		if (!isInitialized){
			instance = new Schedule();
			isInitialized = true;
		}
		return instance;
	}
}
