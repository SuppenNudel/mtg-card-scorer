package de.rohmio.mtg;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import de.rohmio.mtgtop8.api.model.enums.CompLevel;
import de.rohmio.scryfall.api.model.enums.Format;
import de.rohmio.scryfall.api.model.enums.Legality;

public class MtgTop8Config {

	// mtgtop8 parameters
	private boolean mainboard;
	private boolean sideboard;
	private int startXdaysbefore;
	private int endXdaysbefore;
	private List<Legality> interrestingLegalities;
	private List<Format> interrestingFormats;
	private CompLevel[] compLevels;
	private int renewXdaysbefore;

	public static MtgTop8Config loadConfig() {
		MtgTop8Config config = new MtgTop8Config();
		Configurations configs = new Configurations();
		try {
			Configuration top8Config = configs.properties(new File("config.properties"));
			// access configuration properties

			config.mainboard = top8Config.getBoolean("mtgtop8.mainboard");
			config.sideboard = top8Config.getBoolean("mtgtop8.sideboard");
			config.startXdaysbefore = top8Config.getInt("mtgtop8.startXdaysbefore");
			config.endXdaysbefore = top8Config.getInt("mtgtop8.endXdaysbefore");
			config.renewXdaysbefore = top8Config.getInt("mtgtop8.renewXdaysbefore");

			String[] formatsStringArray = top8Config.getString("mtgtop8.formats").split(",");
			config.interrestingFormats = new ArrayList<>();
			for (String formatString : formatsStringArray) {
				config.interrestingFormats.add(Format.valueOf(formatString));
			}

			String[] legalityStringArray = top8Config.getString("mtgtop8.legalities").split(",");
			config.interrestingLegalities = new ArrayList<>();
			for (String string : legalityStringArray) {
				config.interrestingLegalities.add(Legality.valueOf(string));
			}

			String[] complevelStringArray = top8Config.getString("mtgtop8.complevels").split(",");
			List<CompLevel> compLevelsList = new ArrayList<>();
			for (String string : complevelStringArray) {
				compLevelsList.add(CompLevel.valueOf(string));
			}
			config.compLevels = compLevelsList.toArray(new CompLevel[compLevelsList.size()]);
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}

		return config;
	}

	public CompLevel[] getCompLevels() {
		return compLevels;
	}

	public int getEndXdaysbefore() {
		return endXdaysbefore;
	}
	public List<Format> getInterrestingFormats() {
		return interrestingFormats;
	}
	public List<Legality> getInterrestingLegalities() {
		return interrestingLegalities;
	}
	public int getRenewXdaysbefore() {
		return renewXdaysbefore;
	}
	public int getStartXdaysbefore() {
		return startXdaysbefore;
	}
	public boolean isMainboard() {
		return mainboard;
	}
	public boolean isSideboard() {
		return sideboard;
	}

}
