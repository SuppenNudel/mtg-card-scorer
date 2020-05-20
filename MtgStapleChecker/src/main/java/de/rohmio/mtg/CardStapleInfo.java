package de.rohmio.mtg;

public class CardStapleInfo {
	
	private String deckName;
	private int deckCount;
	private String format;
	
	@Override
	public String toString() {
		return String.format("%s of '%s' in '%s'", deckCount, deckName, format);
	}
	
	public void setDeckCount(int deckCount) {
		this.deckCount = deckCount;
	}
	public void setDeckName(String deckName) {
		this.deckName = deckName;
	}
	public void setFormat(String format) {
		this.format = format;
	}	
	public String getDeckName() {
		return deckName;
	}
	public int getDeckCount() {
		return deckCount;
	}
	public String getFormat() {
		return format;
	}

}
