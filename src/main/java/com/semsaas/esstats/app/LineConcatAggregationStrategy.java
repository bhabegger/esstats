package com.semsaas.esstats.app;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.processor.aggregate.AggregationStrategy;

public class LineConcatAggregationStrategy implements AggregationStrategy {

	@Override
	public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
		if(oldExchange == null) {
			String body = newExchange.getIn().getBody(String.class);
			StringBuffer buffer = new StringBuffer(body);
			buffer.append("\n");
			newExchange.getIn().setBody(buffer);
			return newExchange;
		} else {
			Message oldIn = oldExchange.getIn();
			Message newIn = newExchange.getIn();
			StringBuffer buffer = oldIn.getBody(StringBuffer.class);
			String newLine = newIn.getBody(String.class);
			buffer.append(newLine);
			buffer.append("\n");
			return oldExchange;
		}
	}
}
