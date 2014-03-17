package com.semsaas.esstats.app;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

public class ExtractBestProcessor implements Processor {
	int maxSize = 10000;
	
	@Override
	public void process(Exchange exchange) throws Exception {
		Message in = exchange.getIn();
		List<Map.Entry<String, Object>> items = in.getBody(List.class);
		
		int size = items.size() > maxSize ? maxSize : items.size();
				
/*		String[] extractedItems = new String[size];
		for(int i=0;i<extractedItems.length; i++) {
			extractedItems[i] = items.get(i).getKey();
		} */
		
		StringBuffer extractedRegexp = new StringBuffer();
		for(int i=0;i<size; i++) {
			if(i>0) { extractedRegexp.append("|"); }
			extractedRegexp.append("\\\\Q");
			extractedRegexp.append(items.get(i).getKey());
			extractedRegexp.append("\\\\E");
		}
		
		in.setBody(extractedRegexp.toString());
	}

}
