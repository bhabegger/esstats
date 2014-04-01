package com.semsaas.esstats.app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
import cern.colt.matrix.ObjectFactory2D;
import cern.colt.matrix.ObjectMatrix2D;

import com.semsaas.stats.Measures;

public class DeanonimizeColt {
	
	HashMap<String, Integer> keyMap = new HashMap<>();
	HashMap<String, Scores> scores = new HashMap<>();

    static Comparator<Map.Entry<String,Double>> compare = new Comparator<Map.Entry<String,Double>>() {
		@Override
		public int compare(Entry<String,Double> o1, Entry<String,Double> o2) {
			return -1*Double.compare(o1.getValue(),o2.getValue());
		}
	};
	
	static Logger logger = LoggerFactory.getLogger(DeanonimizeColt.class);
	
	public static void main(String[] args) {
		DeanonimizeColt deanonimizer = new DeanonimizeColt();
		File currentScores = args.length > 2 ? new File(args[2]): null; 
		deanonimizer.run(new File(args[0]), new File(args[1]), currentScores);
	}
	
	public void run(File sourceDistFile, File targetDistFile, File currentScoresFile) {
		int sourceCount = 0;
		int targetCount = 0;
				
		// First walk through the files (number of lines, list of terms)
		{
			logger.info("Loading stats for source="+sourceDistFile.getName());
			try {
				BufferedReader sReader = new BufferedReader(new FileReader(sourceDistFile));
				String sLine = sReader.readLine();
				while(sLine != null) {
					sourceCount++;
					pushTerms(sLine);
					sLine = sReader.readLine();
				}
				sReader.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
			
			logger.info("Loading stats for target="+targetDistFile.getName());
			try {
				BufferedReader sReader = new BufferedReader(new FileReader(targetDistFile));
				String tLine = sReader.readLine();
				while(tLine != null) {
					targetCount++;
					pushTerms(tLine);
					tLine = sReader.readLine();
				}
				sReader.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}

		scores = new HashMap<>();
		try {
			if(currentScoresFile != null) {
				loadCurrentScores(currentScoresFile);
			}
		} catch(IOException e) {
			e.printStackTrace();
		}

		
		// Second walk through the files to populate the matrix
		DoubleMatrix2D targetDist = DoubleFactory2D.sparse.make(targetCount, keyMap.size(), 0.0);
		String[] targetIds = new String[targetCount];
		logger.info("Loading target matrix using "+targetDistFile.getName());
		loadMatrix(targetDistFile, targetDist, targetIds);

		int sourceStart = 0;
		int sourceEnd = -1;
		if(currentScoresFile != null) {
			sourceStart = scores.size();
			sourceEnd = sourceStart + 100;
			if(sourceCount < sourceEnd) {
				sourceEnd = sourceCount;
			}
			sourceCount = sourceEnd - sourceStart;
		}

		DoubleMatrix2D sourceDist = DoubleFactory2D.sparse.make(sourceCount, keyMap.size(), 0.0);
		String[] sourceIds = new String[sourceCount];
		logger.info("Loading source matrix using "+sourceDistFile.getName());
		loadMatrix(sourceDistFile, sourceDist, sourceIds, sourceStart, sourceEnd);		

		// Run deanonimization 
		logger.info("Deanonimizing target="+targetDistFile.getName()+" using source="+sourceDistFile.getName());

		// For each entry in the testDist file search for the closest distribution in the sourceDist file
		int[] matches = new int[3];
		for(int i=0;i<matches.length;i++) {
			matches[i] = 0;
		}
		int total = 0;
		int totalPresent = 0;
		
		
		for(int i=0; i<targetCount; i++) {
			logger.info("Deanonimizing "+targetIds[i]);
			PriorityQueue<Map.Entry<String, Double>> bestMatches = new PriorityQueue<Map.Entry<String, Double>>(matches.length,compare);
			double selfJSD = -1;
	
			Scores s = scores.get(targetIds[i]);
			if(s == null) {
				s = new Scores(matches.length);
			} else {
				for(int k=0; k<s.keys.length; k++) {
					bestMatches.add(new AbstractMap.SimpleEntry<String, Double>(s.keys[k],s.values[k]));
				}
			}
			
			for(int j=0; j<sourceCount; j++) {
				double jsd = Measures.JensenShannonDivergence(targetDist.viewRow(i),sourceDist.viewRow(j));
				// logger.info("JSD(target:"+tDist.getKey()+",source:"+sDist.getKey()+") = "+jsd);
				bestMatches.add(new AbstractMap.SimpleEntry<String, Double>(sourceIds[j],jsd));
				if(bestMatches.size() > matches.length) {
					// Remove the worst
					bestMatches.poll();
				}

				if(sourceIds[j].equals(targetIds[i])) {
					selfJSD = jsd;
				}
			}
			
			logger.info("*** "+targetIds[i]+"*** self JSD: "+selfJSD);
			while(bestMatches.size() > 0) {
				int pos = bestMatches.size();
				Entry<String,Double> entry = bestMatches.poll();
				String star = " ";
				if(targetIds[i].equals(entry.getKey())) {
					matches[pos-1]++;
					star = "*";
				}
				s.set(pos -1, entry.getKey(), entry.getValue());
				logger.info("["+pos+"]"+star+" target:"+targetIds[i]+" -> source:"+entry.getKey()+ " ("+entry.getValue()+")");
			}
			total++;
			if(selfJSD >= 0.0) {
				totalPresent++;
			}
		}
		
		int found = 0;
		for(int i=0; i<matches.length; i++) {
			found += matches[i];
			logger.info("Correct matches within first "+(i+1)+" entries: "+found+"/"+total+" = "+(((double)found)/total));
			logger.info("Correct matches within first "+(i+1)+" entries: "+found+"/"+totalPresent+" = "+(((double)found)/totalPresent));
		
		}
		
		if(currentScoresFile != null) {
			try {
				saveCurrentScores(currentScoresFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void loadCurrentScores(File currentScoresFile) throws IOException {
		if(currentScoresFile.exists()) {
			BufferedReader reader = new BufferedReader(new FileReader(currentScoresFile));
			String line = reader.readLine();
			while(line != null) {
				String[] parts = line.split("\t");
				String targetKey = parts[0];
				Scores targetScores = new Scores(parts.length -1);
				for(int i=1; i<parts.length; i++) {
					int idx = parts[i].indexOf(':');
					String sourceKey = parts[i].substring(0,idx);
					Double sourceScore = Double.parseDouble(parts[i].substring(idx+1));
					targetScores.set(i-1, sourceKey, sourceScore);
				}
				scores.put(targetKey, targetScores);
			}
			reader.close();
		}
	}

	private void saveCurrentScores(File currentScoresFile) throws IOException {
		PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(currentScoresFile)));
		for(Entry<String, Scores> entry: scores.entrySet()) {
			writer.print(entry.getKey());
			writer.print("\t");
			Scores targetScores = entry.getValue();
			for(int i=0; i<targetScores.keys.length; i++) {
				if(i>0) {
					writer.print("\t");
				}
				writer.print(targetScores.keys[i]);
				writer.print(":");
				writer.print(targetScores.values[i]);
			}
			writer.println();
		}
		writer.close();
	}

	private void loadMatrix(File distFile, DoubleMatrix2D distMatrix, String[] idArray) {
		loadMatrix(distFile, distMatrix, idArray, 0, -1);
	}
	
	private void loadMatrix(File distFile, DoubleMatrix2D distMatrix,	String[] idArray, int startLine, int endLine) {
		int i = 0;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(distFile));
			String sLine = reader.readLine();
			while(sLine != null && i < startLine) {
				sLine = reader.readLine();
			}
			while(sLine != null && (endLine < 0 || i < endLine)) {
				System.out.println(sLine);
				String[] parts = sLine.split(" ");
				idArray[i] = parts[0].substring(0,parts[0].indexOf('-'));
				Double sum = 0.0;
				for(int j=1; j<parts.length; j++) {
					sum += Double.parseDouble(parts[j].substring(parts[j].indexOf(":")+1));
				}
				
				for(int j=1; j<parts.length; j++) {
					String[] kv = parts[j].split(":");
					try {
						int k = keyMap.get(kv[0]);
						distMatrix.set(i, k, Double.parseDouble(kv[1]) / sum);
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
				i++;
				sLine = reader.readLine();
			}
			reader.close();
		} catch(IOException e) {
			e.printStackTrace();
		}		
	}

	private void pushTerms(String line) {
		String[] parts = line.split(" ");
		for(int i=1; i<parts.length; i++) {
			String[] kv = parts[i].split(":");
			if(!keyMap.containsKey(kv[0])) {
				keyMap.put(kv[0], keyMap.size());
			}
		}
	}
	
	class Scores {
		String[] keys;
		Double[] values;
		public Scores(int size) {
			keys = new String[size];
			values = new Double[size];
		}
		
		public void set(int i, String key, Double value) {
			keys[i] = key;
			values[i] = value;
		}
	}
}
