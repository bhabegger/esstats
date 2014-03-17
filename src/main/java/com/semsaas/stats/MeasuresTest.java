package com.semsaas.stats;

import static org.junit.Assert.*;
import static com.semsaas.stats.Measures.*;

import org.junit.Test;

public class MeasuresTest {

	@Test
	public void nodiff() {
		double p[] = { 0.1, 0.3, 0.25, 0.2, 0.15 };
		double q[] = { 0.1, 0.3, 0.25, 0.2, 0.15 };
		
		double klb = KullbackLeiblerDivergence(p, q);
		System.out.println("nodiff: "+ klb);
		assert(klb == 0.0);
	}

	
	@Test
	public void normalizedNodiff() {
		double p[] = { 1, 3, 25, 2, 15 };
		double q[] = { 1, 3, 25, 2, 15 };
		
		double klb = KullbackLeiblerDivergence(normalize(p), normalize(q));
		System.out.println("normalizedNodiff: "+ klb);
		assert(klb == 0.0);
	}
	

	@Test
	public void swap() {
		double p[] = { 0.3, 0.25, 0.2, 0.15, 0.1};
		double q[] = { 0.1, 0.3, 0.25, 0.2, 0.15 };
		
		double kld = KullbackLeiblerDivergence(p, q);
		System.out.println("swap (kld): "+kld);
		
		double jsd = jsd(p,q);
		System.out.println("swap (jsd): "+jsd);
	}

	@Test
	public void orthoganal() {
		double p[] = { 0.0 ,1.0};
		double q[] = { 1.0, 0.0 };
		
		double jsd = jsd(p,q);
		System.out.println("orthogonal: "+ jsd);
	}
	
	@Test
	public void subdistrib() {
		double p[] = normalize(new double[]{ 10 , 5, 10, 20, 50, 20 });
		double q[] = normalize(new double[]{ 10 , 0, 10, 0, 50, 0 });
		
		double jsd = jsd(p,q);
		System.out.println("subdistrib: "+jsd);
	}

}
