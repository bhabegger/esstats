package com.semsaas.esstats.app;


public class StatsCalc {
	public static void main(String[] args) {
		final org.apache.camel.main.Main main = new org.apache.camel.main.Main();
		
		main.addRouteBuilder(new ESStatsRoutes());
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				try {
					main.stop();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}));
		try {
			main.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
