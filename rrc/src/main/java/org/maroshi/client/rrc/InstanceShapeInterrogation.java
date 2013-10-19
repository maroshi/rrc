package org.maroshi.client.rrc;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Formatter;

import net.oauth.OAuthException;

import org.apache.commons.cli.CommandLine;
import org.apache.http.HttpStatus;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.wink.client.ClientResponse;
import org.eclipse.lyo.client.exception.RootServicesException;
import org.eclipse.lyo.client.oslc.OSLCConstants;
import org.eclipse.lyo.client.oslc.jazz.JazzFormAuthClient;
import org.eclipse.lyo.client.oslc.jazz.JazzRootServicesHelper;
import org.maroshi.client.util.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class InstanceShapeInterrogation {
	private final static String smapleInstanceShapeUri = "https://jazz.net/sandbox02-rm/types/_1ufI4TZoEeOYbalKlkVQRA";

	static Logger logger = Logger.getLogger(InstanceShapeInterrogation.class);
	private Model rdfModel = null;
	private JazzFormAuthClient client = null;
	
	public InstanceShapeInterrogation(JazzFormAuthClient client) {
		this.client = client;
	}

	private Model loadModelForInstanceShape(String urlString){
		if (client == null)
			return null;
		
		ClientResponse response;
		try {
			response = client.getResource(urlString,OSLCConstants.CT_RDF);
			InputStream is = response.getEntity(InputStream.class);
			rdfModel = ModelFactory.createDefaultModel();
			rdfModel.read(is,urlString);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (OAuthException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return rdfModel;
	}
	public void dumpAllStatements(){
		StmtIterator itr = rdfModel.listStatements();
		int count = 1;
		while (itr.hasNext()) {
			Statement currStm = (Statement) itr.next();
			
			Formatter f = new Formatter();
			f.format("Statement[%1$03d]: %2$s", count, currStm.toString());
			logger.info(f.toString());
			f.close();
			count++;			
		}
	}

	public static void main(String[] args) {
		LoggerFactory.config();
		; // init logger

		CommandLine cmd = rrc1.readCliArgs(args);
		if (cmd == null)
			return;

		String rmContextUrl = cmd.getOptionValue("conn.site.rm");
		String jtsContextUrl = cmd.getOptionValue("conn.site.jts");
		String user = cmd.getOptionValue("conn.user");
		String passwd = cmd.getOptionValue("conn.password");
		String projectArea = cmd.getOptionValue("conn.projectArea");

		JazzRootServicesHelper helper;

		try {
			// STEP 1: Initialize a Jazz rootservices helper and indicate we're
			// looking for the RequirementManagement catalog
			helper = new JazzRootServicesHelper(rmContextUrl,
					OSLCConstants.OSLC_RM_V2);

			// STEP 2: Create a new Form Auth client with the supplied
			// user/password
			// RRC is a fronting server, so need to use the initForm() signature
			// which allows passing of an authentication URL.
			// For RRC, use the JTS for the authorization URL
			JazzFormAuthClient client = helper.initFormClient(user, passwd,
					jtsContextUrl);

			// STEP 3: Login in to Jazz Server
			if (client.formLogin() != HttpStatus.SC_OK) {
				logger.error("-- falied connection --");
				System.exit(1);
			}

			// STEP 4: Get the URL of the OSLC ChangeManagement catalog
			String catalogUrl = helper.getCatalogUrl();
			logger.info("-- catalogUrl=" + catalogUrl); 
			// STEP 5: Find the OSLC Service Provider for the project area we
			// want to work with
			String serviceProviderUrl = client.lookupServiceProviderUrl(
					catalogUrl, projectArea);
			logger.info("-- serviceProviderUrl=" + serviceProviderUrl);
			
			InstanceShapeInterrogation instanceShapeInterrogation = new InstanceShapeInterrogation(client);
//			String urlString = "https://jazz.net/sandbox02-rm/resources/_gatgoTcVEeOYbalKlkVQRA";
			String urlString = instanceShapeInterrogation.smapleInstanceShapeUri;
			if (instanceShapeInterrogation
					.loadModelForInstanceShape(urlString) == null){
				logger.fatal("-- failed load RDF model.");
				return;
			}
			instanceShapeInterrogation.dumpAllStatements();


		} catch (RootServicesException re) { 
			logger.log(Level.FATAL,
					"Unable to access the Jazz rootservices document at: "
							+ rmContextUrl + "/rootservices", re);
		} catch (Exception e) {
			logger.log(Level.FATAL, e.getMessage(), e);
		}
	}

}
