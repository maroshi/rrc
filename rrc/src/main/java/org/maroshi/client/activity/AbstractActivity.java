package org.maroshi.client.activity;

import java.net.URI;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.eclipse.lyo.oslc4j.core.model.OslcConstants;
import org.maroshi.client.util.LoggerHelper;

import com.hp.hpl.jena.rdf.arp.impl.Names;

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
	protected void loggNextActivityDestination(AbstractActivity activityInstance){
		logger.debug(LoggerHelper.LINE_TITLE + "  to -> "
				+ activityInstance.getClass().getName());

	}
	protected void nextActivityIs(AbstractActivity activity){
		getSchedule().add(activity);
		loggNextActivityDestination(activity);	
	}
	
	public static QName URItoQName(URI inpURI){
		QName retVal = null;
		String uriStr = inpURI.toString();
		String nameSpace = null;
		if (uriStr.startsWith(OslcConstants.OSLC_CORE_NAMESPACE)){
			nameSpace = OslcConstants.OSLC_CORE_NAMESPACE;
		}else if (uriStr.startsWith(OslcConstants.XML_NAMESPACE)){			
			nameSpace = OslcConstants.XML_NAMESPACE;
		}else if (uriStr.startsWith(OslcConstants.DCTERMS_NAMESPACE)){			
			nameSpace = OslcConstants.DCTERMS_NAMESPACE;
		}else if (uriStr.startsWith(OslcConstants.OSLC_DATA_NAMESPACE)){			
			nameSpace = OslcConstants.OSLC_DATA_NAMESPACE;
		}else if (uriStr.startsWith(OslcConstants.RDF_NAMESPACE)){			
			nameSpace = OslcConstants.RDF_NAMESPACE;
		}else if (uriStr.startsWith(OslcConstants.RDFS_NAMESPACE)){			
			nameSpace = OslcConstants.RDFS_NAMESPACE;
		}
		if (nameSpace == null){
			int lastSlashIdx = uriStr.lastIndexOf('/');
			nameSpace = uriStr.substring(0, lastSlashIdx + 1);
		}
		String prefix = uriStr.substring(nameSpace.length());
		retVal = new QName(nameSpace, prefix);
		return retVal;
	}
}
