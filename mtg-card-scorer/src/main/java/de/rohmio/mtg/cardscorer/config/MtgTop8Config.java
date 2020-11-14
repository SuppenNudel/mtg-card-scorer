package de.rohmio.mtg.cardscorer.config;

import java.util.List;

import com.beust.jcommander.Parameter;

import de.rohmio.mtg.mtgtop8.api.model.CompLevel;
import de.rohmio.mtg.scryfall.api.model.Format;
import de.rohmio.mtg.scryfall.api.model.Legality;

public class MtgTop8Config {
	
	@Parameter(names = "-legalities")
	private List<Legality> legalities;
	
	@Parameter(names = "-comp-levels")
	private List<CompLevel> compLevels;
	
	@Parameter(names = "-formats")
	private List<Format> formats;
	
	@Parameter(names = "-mainboard")
	private boolean mainboard;
	
	@Parameter(names = "-sideboard")
	private boolean sideboard;
	
	@Parameter(names = "-start")
	private int startXdaysBefore;
	
	@Parameter(names = "-end")
	private int endXdaysBefore;
	
	@Parameter(names = "-renewal-period")
	private int renewXdaysBefore;
	
	@Parameter(names = "-card-names", description = "Card Names to check")
	private List<String> cardNames;

	public List<Legality> getLegalities() {
		return legalities;
	}

	public List<CompLevel> getCompLevels() {
		return compLevels;
	}

	public List<Format> getFormats() {
		return formats;
	}

	public boolean isMainboard() {
		return false; // mainboard;
	}

	public boolean isSideboard() {
		return false; // sideboard;
	}

	public int getStartXdaysBefore() {
		return startXdaysBefore;
	}
	
	public int getEndXdaysBefore() {
		return endXdaysBefore;
	}

	public int getRenewXdaysBefore() {
		return renewXdaysBefore;
	}
	
}
