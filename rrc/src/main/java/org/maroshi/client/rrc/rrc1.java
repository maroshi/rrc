package org.maroshi.client.rrc;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.maroshi.client.activity.AbstractActivity;
import org.maroshi.client.activity.Context;
import org.maroshi.client.activity.LogoActivity;
import org.maroshi.client.activity.Schedule;
import org.maroshi.client.util.CliOptionsBuilder;
import org.maroshi.client.util.LoggerFactory;
import org.maroshi.client.util.Msg;

public class rrc1 {
	static Logger logger = Logger.getLogger(rrc1.class);

	public static void main(String[] args) {
		LoggerFactory.config();

		CommandLine cmd = readCliArgs(args);
		if (cmd == null)
			return;
		
		Schedule sched = Schedule.instance();
		sched.add(new LogoActivity());
		AbstractActivity currActivity = null;
		int currActivityIndex = 0;
		while (currActivityIndex < sched.size()) {
			currActivity = sched.get(currActivityIndex);
			// during process the one or more activities added the schedule
			// as long as there are more activities the processing will continue
			currActivity.process();
			currActivityIndex++;
		}
	}

	public static CommandLine readCliArgs(String[] args) {
		HelpFormatter helpFormatter = new HelpFormatter(); // init usage message
		Options options = null;
		CommandLine cmd = null;
		try {
			String[] configArgsArr = CliOptionsBuilder
					.collectCliArgumentsAndOptions(args);
			Context.instance().setCliArgs(configArgsArr);

			// read command line option definitions from config file
			options = CliOptionsBuilder.getOptions();
			if (options != null && options.getOptions().size() > 0) {
				
				GnuParser gnuParser = new GnuParser();
				cmd = gnuParser.parse(options, configArgsArr);
				if (cmd == null)
					return null;

				if (cmd.hasOption(Msg.getString("app.init.optionHelpFlagLong"))){
					helpFormatter.printHelp("rrc1", options);
				}
				logCliArgs(cmd);
				Context.instance().setCommandLine(cmd);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e1) {
			helpFormatter.printHelp("Demo 01", options, true);
			e1.printStackTrace();
		}
		return cmd;
	}

	private static void logCliArgs(CommandLine cmd) {
		if (logger.getEffectiveLevel().isGreaterOrEqual(Level.DEBUG)) {
			Option[] optionsArr = cmd.getOptions();
			logger.debug(LoggerFactory.LINE);
			logger.debug("Argument list from all sources in order of entry.");
			logger.debug(LoggerFactory.LINE_START);
			for (Option option : optionsArr) {
				logger.debug(option.getLongOpt() + "="
						+ option.getValue());
			}
			logger.debug(LoggerFactory.LINE_END);
		}
	}

}
