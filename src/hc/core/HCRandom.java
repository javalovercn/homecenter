package hc.core;

import java.util.Random;

public class HCRandom {
	final Random random;
	final Random passRandom;
	
	public HCRandom(final long seed){
		random = new Random(seed);
		byte[] baseBytes = IConstant.passwordBS;
		if(baseBytes == null){
			baseBytes = String.valueOf(System.currentTimeMillis()).getBytes();
		}
		int nextRandomSeed = 0;
		for (int i = 0; i < baseBytes.length; i++) {
			nextRandomSeed += baseBytes[i] & 0xFF;
		}
		if(nextRandomSeed == seed){
			nextRandomSeed += random.nextInt();
		}
		passRandom = new Random(nextRandomSeed);
	}
	
	public final int nextInt(){
		return passRandom.nextInt() ^ random.nextInt();
	}
}
