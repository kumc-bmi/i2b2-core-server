/*******************************************************************************
 * Copyright (c) 2006-2018 Massachusetts General Hospital 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. I2b2 is also distributed under
 * the terms of the Healthcare Disclaimer.
 ******************************************************************************/
/*

 * 
 * Contributors:
 * 		Lori Phillips
 */
package edu.harvard.i2b2.ontology.delegate;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import edu.harvard.i2b2.common.exception.I2B2DAOException;
import edu.harvard.i2b2.common.exception.I2B2Exception;
import edu.harvard.i2b2.common.util.jaxb.JAXBUtilException;
import edu.harvard.i2b2.ontology.dao.ConceptDao;
import edu.harvard.i2b2.ontology.datavo.i2b2message.MessageHeaderType;
import edu.harvard.i2b2.ontology.datavo.i2b2message.ResponseMessageType;
import edu.harvard.i2b2.ontology.datavo.pm.ProjectType;
import edu.harvard.i2b2.ontology.datavo.vdo.ModifierType;
import edu.harvard.i2b2.ontology.datavo.vdo.ModifiersType;
import edu.harvard.i2b2.ontology.datavo.vdo.VocabRequestType;
import edu.harvard.i2b2.ontology.ws.GetCodeInfoDataMessage;
import edu.harvard.i2b2.ontology.ws.MessageFactory;

	public class GetModifierCodeInfoHandler extends RequestHandler {
	    private static Log log = LogFactory.getLog(GetModifierCodeInfoHandler.class);
		private GetCodeInfoDataMessage  codeInfoMsg = null;
		private VocabRequestType vocabType = null;
		private ProjectType project = null; 
		
		public GetModifierCodeInfoHandler(GetCodeInfoDataMessage requestMsg) throws I2B2Exception {
			try {
				codeInfoMsg = requestMsg;
				vocabType = requestMsg.getVocabRequestType();	
				setDbInfo(requestMsg.getMessageHeaderType());
				// test case for bad user
		//				codeInfoMsg.getMessageHeaderType().getSecurity().setUsername("aaaaaaa");
				project = getRoleInfo(codeInfoMsg.getMessageHeaderType());
			}  catch (JAXBUtilException e) {
				log.error("error setting up codeInfoHandler");
				throw new I2B2Exception("GetCodeInfoHandler not configured");
			}
		}
		@Override
		public String execute() throws I2B2Exception {
			// call ejb and pass input object
			ConceptDao conceptDao = new ConceptDao();
			ModifiersType modifiers = new ModifiersType();
			ResponseMessageType responseMessageType = null;
			
//			 if project == null, user was not validated or PM service problem

			if(project == null) {
				String response = null;
				responseMessageType = MessageFactory.doBuildErrorResponse(codeInfoMsg.getMessageHeaderType(), "User was not validated");
				response = MessageFactory.convertToXMLString(responseMessageType);
				log.debug("USER_INVALID or PM_SERVICE_PROBLEM");
				return response;	 
			} 
			

			List response = null;
			try {
				response = conceptDao.findModifierCodeInfo(vocabType, project, this.getDbInfo());
			} catch (I2B2DAOException e1) {
				responseMessageType = MessageFactory.doBuildErrorResponse(codeInfoMsg.getMessageHeaderType(), "Ontology database error");
			}  catch (I2B2Exception e1) {
				responseMessageType = MessageFactory.doBuildErrorResponse(codeInfoMsg.getMessageHeaderType(), "Ontology database configuration error");
			}
			
			// no errors found
			if(responseMessageType == null) {
//				 no db error but response is empty	
				if (response == null) {
					log.debug("query results are null");
					responseMessageType = MessageFactory.doBuildErrorResponse(codeInfoMsg.getMessageHeaderType(), "Query results are empty");
				}
//				 No errors, non-empty response received
				// max not specified so send results
				else {
					Iterator itr = response.iterator();
					while (itr.hasNext())
					{
						ModifierType node = (ModifierType)itr.next();
						modifiers.getModifier().add(node);
					}
					MessageHeaderType messageHeader = MessageFactory.createResponseMessageHeader(codeInfoMsg.getMessageHeaderType());          
					responseMessageType = MessageFactory.createBuildResponse(messageHeader,modifiers);
				}    
			}
	        String responseVdo = null;
	        responseVdo = MessageFactory.convertToXMLString(responseMessageType);
			return responseVdo;
		}
}
