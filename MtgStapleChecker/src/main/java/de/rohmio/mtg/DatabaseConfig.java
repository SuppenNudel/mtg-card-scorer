package de.rohmio.mtg;

import java.io.File;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class DatabaseConfig {

	private String host;
	private int port;
	private String user;
	private String password;
	private String database;
	private String table;

	public static DatabaseConfig loadConfig() {
		DatabaseConfig config = new DatabaseConfig();
		Configurations configs = new Configurations();
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
