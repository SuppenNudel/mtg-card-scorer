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

public class Config {

	// mtgtop8 parameters
	private boolean mainboard;
	private boolean sideboard;
	private int startXdaysbefore;
	private int endXdaysbefore;
	private List<Legality> interrestingLegalities;
	private List<Format> interrestingFormats;
	private CompLevel[] compLevels;

	private int renewXdaysbefore;
	
	private String host;
	private int port;
	private String user;
	private String password;
	private String database;
	private String table;

	public static Config loadConfig() {
		Config config = new Config();
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
		try {
			Configuration databaseConfig = configs.properties(new File("config_database.properties"));
			// access configuration properties
			
			config.host = databaseConfig.getString("database.host");
			config.port = databaseConfig.getInt("database.port");
			config.user = databaseConfig.getString("database.user");
			config.password = databaseConfig.getString("database.password");
			config.database = databaseConfig.getString("database.database");
			config.table = databaseConfig.getString("database.table");
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
	
	public String getDatabase() {
		return database;
	}
	public String getHost() {
		return host;
	}
	
	public String getPassword() {
		return password;
	}
	public int getPort() {
		return port;
	}
	public String getUser() {
		return user;
	}
	public String getTable() {
		return table;
	}

}
