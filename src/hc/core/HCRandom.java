package hc.core;

import java.util.Random;

public class HCRandom {
	final Random random;
	final Random passRandom;
	int nextIdx = 0;
	
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
		final int out = passRandom.nextInt() ^ random.nextInt();
		if((nextIdx++) % 5 == 0){
			final int sleepMS = Math.abs(out % 10);
			if(sleepMS > 0){
				try{
					Thread.sleep(sleepMS);
				}catch (Exception e) {
				}
			}
			return out ^ (((int)System.currentTimeMillis()) & 0xFF);
		}else{
			return out;
		}
	}
}
