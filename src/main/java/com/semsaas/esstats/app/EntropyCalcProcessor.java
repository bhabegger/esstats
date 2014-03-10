package com.semsaas.esstats.app;

import java.io.InputStream;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntropyCalcProcessor implements Processor {
	
	Logger logger = LoggerFactory.getLogger(EntropyCalcProcessor.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		Message in = exchange.getIn();
		InputStream is = in.getBody(InputStream.class);
		JsonFactory factory = new JsonFactory();
		JsonParser jp = factory.createJsonParser(is);
	    ObjectMapper mapper = new ObjectMapper();
	    
	    JsonNode searchResult = mapper.readValue(jp, JsonNode.class);
	    
	    double totalDocs = searchResult.path("hits").path("total").asLong(0);
	    
	    JsonNode facetList = searchResult.path("facets").path("words").path("terms");
	    
	    Comparator<Map.Entry<String,Double>> compare = new Comparator<Map.Entry<String,Double>>() {
			@Override
			public int compare(Entry<String,Double> o1, Entry<String,Double> o2) {
				return -1 * Double.compare(o1.getValue(),o2.getValue());
			}
		};
		
	    PriorityQueue<Map.Entry<String,Double>> q = new PriorityQueue<Map.Entry<String,Double>>(facetList.size(),compare);
	    for(int i=0; i<facetList.size(); i++) {
	    	JsonNode termFacet = facetList.get(i);
	    	String term  = termFacet.path("term").asText();
	    	double count = termFacet.path("count").asLong(0);
	    	double pPresent = (count / totalDocs);
	    	double pAbsent = 1.0 - pPresent;
	    	double termEntropy =  - pPresent * Math.log(pPresent) - pAbsent * Math.log(pAbsent);
	    	
	    	q.add(new AbstractMap.SimpleEntry<String,Double>(term,termEntropy));
	    }
	    
	    StringBuffer output = new StringBuffer();
	    for(Map.Entry<String,Double> entry = q.poll(); entry != null; entry = q.poll()) {
	    	logger.info("H("+entry.getKey()+") = "+entry.getValue());
	    	
	    	output.append(entry.getKey());
	    	output.append("\t");
	    	output.append(entry.getValue());
	    	output.append("\n");
	    }
	    
	    in.setBody(output.toString());
	}

}
