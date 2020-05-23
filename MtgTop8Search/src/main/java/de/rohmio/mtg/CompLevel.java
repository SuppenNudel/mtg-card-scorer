package de.rohmio.mtg;

public enum CompLevel {
	
	Competitive("C"),
	Regular("R"),
	Professional("P"),
	Major("M");
	
	private String top8Code;
	
	private CompLevel(String top8Code) {
		this.top8Code = top8Code;
	}
	
	public String getTop8Code() {
		return top8Code;
	}
	
}
