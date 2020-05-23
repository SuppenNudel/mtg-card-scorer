package de.rohmio.mtg;

public enum MtgTop8Format {

	Standard("ST"),
	Pioneer("PI"),
	Modern("MO"),
	Legacy("LE"),
	Vintage("VI"),
	DuelCommander("EDH"),
	MtgoCommander("EDHM"),
	Block("BL"),
	Extended("EX"),
	Pauper("PAU"),
	Peasant("PEA"),
	Highlander("HIGH"),
	CanadianHighlander("CHL"),
	Limited("LI");
	
	private String top8Code;
	
	private MtgTop8Format(String top8Code) {
		this.top8Code = top8Code;
	}
	
	public String getTop8Code() {
		return top8Code;
	}

}
