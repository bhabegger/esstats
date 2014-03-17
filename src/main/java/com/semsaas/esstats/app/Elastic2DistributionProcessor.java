package com.semsaas.esstats.app;

import java.io.InputStream;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Elastic2DistributionProcessor implements Processor {
	Logger logger = LoggerFactory.getLogger(Elastic2DistributionProcessor.class);
	
	String[] facets = {"march", "april", "may"};
	
	@Override
	public void process(Exchange exchange) throws Exception {
		Message in = exchange.getIn();
		InputStream is = in.getBody(InputStream.class);
		JsonFactory factory = new JsonFactory();
		JsonParser jp = factory.createJsonParser(is);
	    ObjectMapper mapper = new ObjectMapper();
	    
	    JsonNode searchResult = mapper.readValue(jp, JsonNode.class);

	    List<Map.Entry> dists = new ArrayList<Map.Entry>();
	    for(String facet: facets) {
	    	String lineId = in.getHeader("LineId",String.class);
	    	JsonNode facetTermsNode = searchResult.path("facets").path(facet).path("terms");
	    	if(facetTermsNode.size() > 0) {
	    		StringBuffer buffer = new StringBuffer();
		    	buffer.append(lineId);
		    	for(int i=0; i<facetTermsNode.size(); i++) {
		    		JsonNode entryNode = facetTermsNode.get(i);
		    		buffer.append(" ");
		    		buffer.append(entryNode.path("term").asText());
		    		buffer.append(":");
		    		buffer.append(entryNode.path("count").asText());
		    	}
		    	dists.add(new AbstractMap.SimpleEntry<String, String>(facet,buffer.toString()));
	    	} else {
	    		logger.info("Facet "+facet+": Skipping empty distrib "+lineId);
	    	}
	    }
	    in.setBody(dists);
	}

}
