package de.rohmio.mtg;

public enum CompLevel {
	
	Regular("R", 1),
	Competitive("C", 2),
	Major("M", 3),
	Professional("P", 5);
	
	private String top8Code;
	private int factor;
	
	private CompLevel(String top8Code, int factor) {
		this.top8Code = top8Code;
		this.factor = factor;
	}
	
	public String getTop8Code() {
		return top8Code;
	}

	public int getFactor() {
		return factor;
	}
	
}
