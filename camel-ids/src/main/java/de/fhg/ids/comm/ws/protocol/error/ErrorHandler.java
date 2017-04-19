package de.fhg.ids.comm.ws.protocol.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.ids.comm.ws.protocol.ProtocolState;
import de.fhg.ids.comm.ws.protocol.fsm.Event;

public class ErrorHandler {	
	private static final Logger LOG = LoggerFactory.getLogger(ErrorHandler.class);

	public boolean handleError(Event e, ProtocolState state, boolean isConsumer) {
		String entity = "Provider";
		if(isConsumer) {
			entity = "Consumer";
		}
		LOG.debug("*******************************************************************************************************");
		LOG.debug("*  error handler during rat protocol execution ");
		LOG.debug("*  -> state: " + state.description());
		LOG.debug("*  -> side: "+entity+"");		
		LOG.debug("*  -> error: " + e.getMessage().getError().getErrorMessage());
		LOG.debug("*******************************************************************************************************");
		
		return true;
	}

}
