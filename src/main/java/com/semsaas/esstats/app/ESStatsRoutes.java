package com.semsaas.esstats.app;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

public class ESStatsRoutes extends RouteBuilder {
	private Processor entropyCalculator = new EntropyCalcProcessor();

	@Override
	public void configure() throws Exception {
		from("file://input")
			.setHeader(Exchange.HTTP_METHOD).constant("POST")
			.to("http4://134.214.110.24:9200/aol/_search")
			.process(entropyCalculator)
			.to("file://output")
		;
	}
}
