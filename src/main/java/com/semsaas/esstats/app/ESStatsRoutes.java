package com.semsaas.esstats.app;

import java.util.HashMap;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.builder.ExpressionBuilder;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.AggregationStrategy;

public class ESStatsRoutes extends RouteBuilder {
	private Processor entropyCalculator = new EntropyCalcProcessor();
	private Processor dumpList = new LineListDumpingProcessor();
	private Processor extractBest = new ExtractBestProcessor();
	private Processor extractDistrib= new Elastic2DistributionProcessor();

	private Processor extractTerms = new ElasticTermExtractor();
	HashMap<String,HashMap<String,Double>> dists = new HashMap<>();
	private Processor loadDist = new Processor() {
		
		@Override
		public void process(Exchange exchange) throws Exception {
			String line= exchange.getIn().getBody(String.class);
			String[] parts = line.split(" ");
			String key = parts[0] + "," + parts[1];
			HashMap<String,Double> dist = new HashMap<>();
			for(int i=2; i<parts.length; i++) {
				String[] kv = parts[i].split(":");
				dist.put(kv[0],Double.valueOf(kv[1]));
			}
			dists.put(key, dist);
		}
	};
	
	AggregationStrategy lineConcatAggregation = new LineConcatAggregationStrategy();
	
	String esBaseEndpoint = "http4://134.214.110.24:9200/aol/";

	@Override
	public void configure() throws Exception {
		from("file://input")
			.setHeader(Exchange.HTTP_METHOD).constant("POST")
			.to(esBaseEndpoint+"/_search")
			.process(entropyCalculator)
			.setHeader(Exchange.FILE_NAME).javaScript("request.headers.get('"+Exchange.FILE_NAME+"').replaceAll('json$',''+request.body.size()+'.txt')")
			.multicast()
				.to("direct:dump")
				.to("direct:select");
		;
		
		from("direct:dump")
			.process(dumpList)
			.to("file://output")
		;
		
		from("direct:select")
			.process(extractBest)
			.to("string-template://templates/word.json")
			.to("file://words")
		;
		
		from("file://users")
			.split().tokenize("\n").streaming() 
				.setHeader("LineId").body(String.class)
				.inOnly("seda:users.distrib");
		;
		
		from("seda:users.distrib?concurrentConsumers=6")
			.to("direct:extract")
			.setHeader("AggregationDone").header(Exchange.SPLIT_COMPLETE)
			.split().body()
				.setHeader("group").javaScript("request.body.getKey()")
				.setBody().javaScript("request.body.getValue()")
				.inOnly("seda:group")
		;
		from("seda:group")
			.removeHeaders("CamelFile*")
			.to("log:grouping?showHeaders=true")
			.setHeader(Exchange.FILE_NAME,header("group").append(".txt"))
			.inOnly("seda:file.collector")
		;
		
		from("direct:extract")
			.to("log:user")
			.to("string-template://templates/facets.json")
			.to(esBaseEndpoint+"/_search")
			.process(extractDistrib)
		;
		
		from("file://trigger")
			.to("log:terms")
			.to(esBaseEndpoint+"/_termlist?field=anonid")
			.process(extractTerms)
			.to("file://terms")
		;
		
		from("seda:file.collector")
			.to("log:collecting?showHeaders=true")
			.aggregate(header(Exchange.FILE_NAME))
			.aggregationStrategy(lineConcatAggregation)
			.completionPredicate(PredicateBuilder.or(header("AggregationDone"),header(Exchange.AGGREGATED_SIZE).isGreaterThanOrEqualTo(20)))
			.eagerCheckCompletion()
				.to("log:dumping?showHeaders=true")
				.to("file://collect?fileExist=Append")
		;
	}
}
