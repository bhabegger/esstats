package com.semsaas.esstats.app;

import java.util.Collection;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

public class LineListDumpingProcessor implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		Message in = exchange.getIn();
		Collection<Map.Entry<Object,Object>> items = in.getBody(Collection.class);
		
		StringBuffer output = new StringBuffer();
		for(Map.Entry<Object,Object> entry: items) {
	    	output.append(entry.getKey());
	    	output.append("\t");
	    	output.append(entry.getValue());
	    	output.append("\n");
		}
		
		in.setBody(output);
	}
}
