package com.semsaas.stats;

import cern.colt.function.DoubleFunction;

public class NormalizationFunction implements DoubleFunction {
	double divisor;
	public NormalizationFunction(double divisor) {
		this.divisor = divisor;
	}

	@Override
	public double apply(double arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

}
