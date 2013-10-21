package org.maroshi.client.activity;

import org.apache.commons.cli.CommandLine;
import org.apache.log4j.Logger;
import org.eclipse.lyo.client.oslc.jazz.JazzFormAuthClient;
import org.eclipse.lyo.client.oslc.resources.Requirement;
import org.eclipse.lyo.oslc4j.core.model.ResourceShape;

public class Context {
	static Logger logger = Logger.getLogger(Context.class);
	static boolean isInitialized = false;
	private static Context instance = null;
	private CommandLine commandLine = null;
	private String executionResult = null;
	private String[] commandLineArgs = null;
	private DoActivity.DoActivityEnum doCommand = null;
	private JazzFormAuthClient jazzClient = null;
	private String serviceProviderUrl = null;
	private String requirementFactoryUrl = null;
	private org.maroshi.client.model.ResourceShape requirementInstanceShape = null;
	private String[] instanceShapesTitleStrArr = null;
	private Requirement requirement = null;


	public static Context instance() {
		if (!isInitialized){
			instance = new Context();
			isInitialized = true;
		}
		return instance;
	}

	public void setCommandLine(final CommandLine cmd) {
		this.commandLine = cmd;		
	}

	public CommandLine getCommandLine() {
		return commandLine;
	}

	public String getExecutionResult() {
		return executionResult;
	}

	public void setExecutionResult(String executionResult) {
		this.executionResult = executionResult;
	}

	public void setCliArgs(String[] args) {
		commandLineArgs = args;		
	}

	public String[] getCommandLineArgs() {
		return commandLineArgs;
	}

	public DoActivity.DoActivityEnum getDoCommand() {
		return doCommand;
	}

	public void setDoCommand(DoActivity.DoActivityEnum doCommand) {
		this.doCommand = doCommand;
	}

	public JazzFormAuthClient getJazzClient() {
		return jazzClient;
	}

	public void setJazzClient(JazzFormAuthClient jazzClient) {
		this.jazzClient = jazzClient;
	}

	public String getServiceProviderUrl() {
		return serviceProviderUrl;
	}

	public void setServiceProviderUrl(String serviceProviderUrl) {
		this.serviceProviderUrl = serviceProviderUrl;
	}

	public String getRequirementFactoryUrl() {
		return requirementFactoryUrl;
	}

	public void setRequirementFactoryUrl(String requirementFactoryUrl) {
		this.requirementFactoryUrl = requirementFactoryUrl;
	}

	public org.maroshi.client.model.ResourceShape getRequirementInstanceShape() {
		return requirementInstanceShape;
	}

	public void setRequirementInstanceShape(org.maroshi.client.model.ResourceShape requirementInstanceShape) {
		this.requirementInstanceShape = requirementInstanceShape;
	}

	public String[] getInstanceShapesTitleStrArr() {
		return instanceShapesTitleStrArr;
	}

	public void setInstanceShapesTitleStrArr(String[] instanceShapesTitleStrArr) {
		this.instanceShapesTitleStrArr = instanceShapesTitleStrArr;
	}

	public Requirement getRequirement() {
		return requirement;
	}

	public void setRequirement(Requirement requirement) {
		this.requirement = requirement;
	}

}
