package de.rohmio.mtg.cardscore.database;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class DatabaseConfig {

	@Parameter(names = { "-host", "-h" }, description = "Database Server", required = true)
	private String host;

	@Parameter(names = "-port", required = true)
	private int port;

	@Parameter(names = "-user", required = true)
	private String user;

	@Parameter(names = "-password", description = "Database Password", password = true, required = true)
	private String password;

	@Parameter(names = "-database", required = true)
	private String database;

	@Parameter(names = "-table", required = true)
	private String table;

	public static DatabaseConfig loadConfig(String configFile) {
		DatabaseConfig db_params = new DatabaseConfig();
		JCommander.newBuilder().addObject(db_params).build().parse(configFile);
		return db_params;
	}
	
	@Override
	public String toString() {
		return String.format("host: %s; port: %s; user: %s; password: %s; database: %s; table: %s", host, port, user, password, database, table);
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}

	public String getDatabase() {
		return database;
	}

	public String getTable() {
		return table;
	}

}
