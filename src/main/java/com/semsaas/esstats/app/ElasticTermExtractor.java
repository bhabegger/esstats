package com.semsaas.esstats.app;

import java.io.InputStream;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

public class ElasticTermExtractor implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		Message in = exchange.getIn();
		InputStream is = in.getBody(InputStream.class);
		JsonFactory factory = new JsonFactory();
		JsonParser jp = factory.createJsonParser(is);
	    ObjectMapper mapper = new ObjectMapper();
	    
	    JsonNode termsNode = mapper.readValue(jp, JsonNode.class).path("terms");
	    StringBuffer buffer = new StringBuffer();
	    
	    for(int i=0; i<termsNode.size(); i++) {
	    	String item = termsNode.get(i).path("name").asText();
	    	buffer.append(item);
	    	buffer.append("\n");
	    }
	    in.setBody(buffer.toString());
	}

}
