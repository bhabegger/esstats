package com.semsaas.stats;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.function.DoubleDoubleFunction;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.jet.math.Functions;

public class Measures {
	public static Logger logger = LoggerFactory.getLogger(Measures.class);
	
	/**
	 * Kullbach-Leibler divergence (@see http://en.wikipedia.org/wiki/Kullback%E2%80%93Leibler_divergence)
	 * @param p - Categorical probability distribution represented as an array of double values between 0.0 and 1.0
	 * @param q - Categorical probability distribution represented as an array of double values between 0.0 and 1.0
	 * @return KLD(P || Q)
	 */
	public static double kld(double[] p, double[] q) {
		return KullbackLeiblerDivergence(p, q);
	}
	public static double KullbackLeiblerDivergence(double[] p, double[] q) {
		assert p.length == q.length;
		double sum = 0;
		double pchecksum = 0;
		double qchecksum = 0;
		
		for(int i=0; i<p.length; i++) {
			if(p[i]!=0) {
				if(q[i] != 0) { 
					sum += Math.log(p[i]/q[i]) * p[i];					
				} else {
					logger.warn("KulbachLieber: q["+i+"] was 0 when p["+i+"] was not");
				}
			}
			pchecksum += p[i];
			qchecksum += q[i];
		}
		if(pchecksum < 0.99 || pchecksum > 1.01 ) {
			logger.warn("KulbachLieber: p did not sum up to 1.0 but "+pchecksum);
		}
		if(qchecksum < 0.99 || qchecksum > 1.01 ) {
			logger.warn("KulbachLieber: q did not sum up to 1.0 but "+qchecksum);
		}
		return sum;
	}

	public static double KullbackLeiblerDivergence(String[] keys, Map<String,Double> p, Map<String,Double> q) {
		double sum = 0;
		double pchecksum = 0;
		double qchecksum = 0;
		
		for(int i=0; i<keys.length; i++) {
			String feature = keys[i];
			Double p_i = p.get(feature); if(p_i == null) p_i = 0.0;
			Double q_i = q.get(feature); if(q_i == null) q_i = 0.0;
			if(p_i != 0.0) {
				if(q_i != 0.0) { 
					sum += Math.log(p_i/q_i) * p_i;
				} else {
					logger.warn("KulbachLieber: q["+i+"] was 0 when p["+i+"] was not");
				}
			}
			pchecksum += p_i;
			qchecksum += q_i;
		}
		if(pchecksum < 0.99 || pchecksum > 1.01 ) {
			logger.warn("KulbachLieber: p did not sum up to 1.0 but "+pchecksum);
		}
		if(qchecksum < 0.99 || qchecksum > 1.01 ) {
			logger.warn("KulbachLieber: q did not sum up to 1.0 but "+qchecksum);
		}
		return sum;
	}
	
	static DoubleDoubleFunction jsdComponent = new DoubleDoubleFunction() {
		@Override
		public double apply(double p_i, double q_i) {
			Double m_i = 0.5 * (p_i + q_i);
			return	((p_i != 0.0 && m_i != 0.0) ? Math.log(p_i/m_i) * p_i : 0.0)
				+	((q_i != 0.0 && m_i != 0.0) ? Math.log(q_i/m_i) * q_i : 0.0);
		}
	};
	
	/**
	 * Jensen-Shannon divergence (@see http://en.wikipedia.org/wiki/Jensen%E2%80%93Shannon_divergence)
	 * @param p - Categorical probability distribution represented as an array of double values between 0.0 and 1.0
	 * @param q - Categorical probability distribution represented as an array of double values between 0.0 and 1.0
	 * @return JSD(P || Q)
	 */
	public static double jsd(double[] p, double[] q) {
		return JensenShannonDivergence(p, q);
	}
	
	public static double jsd(DoubleMatrix1D p, DoubleMatrix1D q) {
		return JensenShannonDivergence(p, q);
	}
	public static double JensenShannonDivergence(double[] p, double[] q) {
		assert p.length == q.length;
	
		double sum = 0.0;
		for(int i=0;i<p.length; i++) {
			Double p_i = p[i];
			Double q_i = q[i];
			
			sum +=	jsdComponent.apply(p_i, q_i);
		}
		
		return 0.5 * sum;
	}
	
	public static double JensenShannonDivergence(String[] keys, Map<String,Double> p, Map<String,Double> q) {
		Map<String,Double> m = new HashMap<String,Double>();
	
		double sum = 0.0;
		for(int i=0;i<keys.length; i++) {
			Double p_i = p.get(keys[i]); if(p_i == null) p_i = 0.0;
			Double q_i = q.get(keys[i]); if(q_i == null) q_i = 0.0;
			sum +=	jsdComponent.apply(p_i, q_i);
		}
		
		return 0.5 * sum;
	}
	
	public static double JensenShannonDivergence(DoubleMatrix1D p, DoubleMatrix1D q) {
		return 0.5 * p.aggregate(q, Functions.plus, jsdComponent);
	}
	
	/**
	 * Distribution normalization function
	 * @param d - Original distribution
	 * @return Normalized distribution obtained by summing the values of d and returning and array of values divided by this sum
	 */
	public static double[] normalize(double[] d) {
		double s = sum(d);
		double normalized[] = new double[d.length];
		for(int i=0; i<d.length; i++) {
			normalized[i] = d[i]/s;
		}
		return normalized;
	}
	
	public static DoubleMatrix1D normalize(DoubleMatrix1D d) {
		double sum = d.aggregate(Functions.plus, Functions.identity);
		return d.assign(new NormalizationFunction(sum));
	}

	public static Map<String, Double> normalize(Map<String, Double> dist) {
		double s = sum(dist.values());
		HashMap<String, Double> nDist = new HashMap<>();
		for(String k: dist.keySet()) {
			nDist.put(k,dist.get(k) / s);
		}
		return nDist;
	}
	private static double sum(Collection<Double> values) {
		double sum = 0;
		for(double v: values) {
			sum += v;
		}
		return sum;
	}
	
	public static double sum(double[] d) {
		double sum = 0;
		for(int i=0; i<d.length; i++) {
			sum += d[i];
		}
		return sum;
	}
}
