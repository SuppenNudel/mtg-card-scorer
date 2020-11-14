package de.rohmio.mtg.staplechecker.model;

public class DeckboxCard {
	
	private String name;
	private String edition;
	private int collectorNumber;
	
	@Override
	public String toString() {
		return String.format("%s (%s #%s)", name, edition, collectorNumber);
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getEdition() {
		return edition;
	}
	public void setEdition(String edition) {
		this.edition = edition;
	}
	public int getCollectorNumber() {
		return collectorNumber;
	}
	public void setCollectorNumber(int collectorNumber) {
		this.collectorNumber = collectorNumber;
	}
	
}
