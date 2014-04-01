package com.semsaas.esstats.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;

import com.semsaas.stats.Measures;

public class DeanonimizeInMemory {
    static Comparator<Map.Entry<String,Double>> compare = new Comparator<Map.Entry<String,Double>>() {
		@Override
		public int compare(Entry<String,Double> o1, Entry<String,Double> o2) {
			return -1*Double.compare(o1.getValue(),o2.getValue());
		}
	};
	
	static Logger logger = LoggerFactory.getLogger(DeanonimizeInMemory.class); 
	public static void main(String[] args) {
		File sourceDist = new File(args[0]);
		File testDist   = new File(args[1]);


		logger.info("Loading source="+sourceDist.getAbsolutePath());
		ArrayList<Map.Entry<String,Map<String,Double>>> sDistList = new ArrayList<>();
		try {
			BufferedReader sReader = new BufferedReader(new FileReader(sourceDist));
			String sLine = sReader.readLine();
			while(sLine != null) {
				Map.Entry<String,Map<String,Double>> sDist = loadDist(sLine);
				sDist.setValue(Measures.normalize(sDist.getValue()));
				sDistList.add(sDist);
				sLine = sReader.readLine();
			}
			sReader.close();
		} catch(IOException e) {
			e.printStackTrace();
		}

		logger.info("Loading target="+testDist.getAbsolutePath());
		ArrayList<Map.Entry<String,Map<String,Double>>> tDistList = new ArrayList<>();
		try {
			BufferedReader tReader = new BufferedReader(new FileReader(testDist));
			String tLine = tReader.readLine();
			while(tLine != null) {
				Map.Entry<String,Map<String,Double>> tDist = loadDist(tLine);
				tDist.setValue(Measures.normalize(tDist.getValue()));
				tDistList.add(tDist);
				tLine = tReader.readLine();
			}
			tReader.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
		logger.info("Deanonimizing target="+testDist.getAbsolutePath()+" using source="+sourceDist.getAbsolutePath());

		// For each entry in the testDist file search for the closest distribution in the sourceDist file
			int[] matches = new int[3];
			for(int i=0;i<matches.length;i++) {
				matches[i] = 0;
			}
			int total = 0;
			int totalPresent = 0;
			
			for(int i=0; i<tDistList.size(); i++) {
				Map.Entry<String,Map<String,Double>> tDist = tDistList.get(i);
				logger.info("Deanonimizing "+tDist.getKey());
				if(tDist.getValue().size() > 0) {
					PriorityQueue<Map.Entry<String, Double>> bestMatches = new PriorityQueue<Map.Entry<String, Double>>(matches.length,compare);
					double selfJSD = -1;
	
					for(Map.Entry<String,Map<String,Double>> sDist: sDistList) {
						if(sDist.getValue().size() > 0) {
							HashSet<String> keySet = new HashSet<>();
							keySet.addAll(tDist.getValue().keySet());
							keySet.addAll(sDist.getValue().keySet());
							
							String[] keys = new String[keySet.size()];
							keys = keySet.toArray(keys);
							double jsd = Measures.JensenShannonDivergence(keys,sDist.getValue(), tDist.getValue());

							// logger.info("JSD(target:"+tDist.getKey()+",source:"+sDist.getKey()+") = "+jsd);
							bestMatches.add(new AbstractMap.SimpleEntry<String, Double>(sDist.getKey(),jsd));
							if(bestMatches.size() > matches.length) {
								// Remove the worst
								bestMatches.poll();
							}

							if(sDist.getKey().equals(tDist.getKey())) {
								selfJSD = jsd;
							}
						}
					}
					
					logger.info("*** "+tDist.getKey()+"*** self JSD: "+selfJSD);
					while(bestMatches.size() > 0) {
						int pos = bestMatches.size();
						Entry entry = bestMatches.poll();
						String star = " ";
						if(tDist.getKey().equals(entry.getKey())) {
							matches[pos-1]++;
							star = "*";
						}						
						logger.info("["+pos+"]"+star+" target:"+tDist.getKey()+" -> source:"+entry.getKey()+ " ("+entry.getValue()+")");
					}
					total++;
					if(selfJSD >= 0.0) {
						totalPresent++;
					}
				}
			}
			
			int found = 0;
			for(int i=0; i<matches.length; i++) {
				found += matches[i];
				logger.info("Correct matches within first "+(i+1)+" entries: "+found+"/"+total+" = "+(((double)found)/total));
				logger.info("Correct matches within first "+(i+1)+" entries: "+found+"/"+totalPresent+" = "+(((double)found)/totalPresent));
			
			}
	}
	
	private static Entry<String, Map<String, Double>> loadDist(String line) {
		String[] parts = line.split(" ");
		String key = parts[0];
		if(key.contains("-")) {
			key = key.substring(0, key.indexOf("-"));
		}
		HashMap<String,Double> dist = new HashMap<>();
		for(int i=1; i<parts.length; i++) {
			String[] kv = parts[i].split(":");
			dist.put(kv[0],Double.valueOf(kv[1]));
		}
		return new AbstractMap.SimpleEntry<String, Map<String, Double>>(key,dist);
	}
}
