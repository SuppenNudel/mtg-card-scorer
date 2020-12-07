package de.rohmio.mtg.cardscorer.config;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import de.rohmio.mtg.mtgtop8.api.model.CompLevel;
import de.rohmio.mtg.scryfall.api.model.Format;

public class MtgTop8Config {

	@Parameter(names = "-comp-levels")
	private List<String> compLevels;

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

	private Map<CompLevel, Integer> compLevelsMap;
	
	public static MtgTop8Config loadConfig(String configFile) {
		MtgTop8Config mtgtop8_config = new MtgTop8Config();
		JCommander.newBuilder().addObject(mtgtop8_config).build().parse(configFile);
		
		mtgtop8_config.compLevelsMap = mtgtop8_config.compLevels
			.stream()
			.map(t -> t.split("="))
			.collect(Collectors.toMap(
				arr -> CompLevel.valueOf(arr[0]),
				arr -> Integer.parseInt(arr[1]))
			);
		return mtgtop8_config;
	}
	
	@Override
	public String toString() {
		return String.format("comp-levels: %s; formats: %s; mainboard: %s; sideboard: %s; start: %s; end: %s; renewal-period: %s; card-names: %s",
				compLevels, formats, mainboard, sideboard, startXdaysBefore, endXdaysBefore, renewXdaysBefore, cardNames);
	}

	public Set<CompLevel> getCompLevels() {
		return EnumSet.copyOf(compLevelsMap.keySet());
	}

	public int getCompLevelFactor(CompLevel compLevel) {
		return compLevelsMap.get(compLevel);
	}

	public List<Format> getFormats() {
		return formats;
	}

	public boolean isMainboard() {
		return mainboard;
	}

	public boolean isSideboard() {
		return sideboard;
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
