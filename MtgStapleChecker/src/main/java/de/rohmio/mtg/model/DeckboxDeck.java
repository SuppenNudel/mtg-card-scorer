package de.rohmio.mtg.model;

public class DeckboxDeck {
	
	private String name;
	private String id;
	
	public DeckboxDeck(String name, String id) {
		this.name = name;
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	public String getId() {
		return id;
	}

}
