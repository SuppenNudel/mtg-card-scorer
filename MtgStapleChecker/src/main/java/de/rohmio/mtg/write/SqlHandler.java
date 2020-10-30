package de.rohmio.mtg.write;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.InsertOnDuplicateSetMoreStep;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import de.rohmio.mtg.MtgStapleChecker;
import de.rohmio.mtg.model.CardStapleInfo;
import de.rohmio.scryfall.api.model.enums.Format;

public class SqlHandler implements IOHandler {
	
	private static Logger log = Logger.getLogger(SqlHandler.class.getName());

	private String host;
	private int port;
	private String user;
	private String password;
	private String database;
	private String table;

	private String url;
	
	public SqlHandler(String host, int port, String user, String password, String database, String table) {
		this.host = host;
		this.port = port;
		this.user = user;
		this.password = password;
		this.database = database;
		this.table = table;
	}

	@Override
	public void init() throws IOException {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} 
		url = String.format("jdbc:mysql://%s:%s/%s?serverTimezone=GMT", host, port, database);
	}
	
	private Connection openConnection() {
		// Connection is the only JDBC resource that we need
		// PreparedStatement and ResultSet are handled by jOOQ, internally
		try {
			Connection connection = DriverManager.getConnection(url, user, password);
			return connection;
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
	
	@Override
	public void addDataset(CardStapleInfo cardStapleInfo) throws IOException {
		List<Field<Integer>> fields = new ArrayList<>();
		Map<String, String> setMap = new HashMap<>();
		
		setMap.put("cardname", cardStapleInfo.getCardname());
		for(Format format : Format.values()) {
			int formatScore = cardStapleInfo.getFormatScore(format);
			if(formatScore < 0) {
				continue;
			}
			fields.add(field(format.name(), Integer.class, formatScore));
			setMap.put(format.name(), String.valueOf(formatScore));
		}
		
		Connection conn = openConnection();
		DSLContext context = DSL.using(conn, SQLDialect.MYSQL);
		
		InsertOnDuplicateSetMoreStep<Record> call = context
				.insertInto(table(table), fields)
				.onDuplicateKeyUpdate()
				.set(setMap);
		try {
			call.execute();
		} catch (Exception e) {
			log.severe("Sql statement failed: "+call.toString());
			e.printStackTrace();
		}
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public CardStapleInfo getCardStapleInfo(String cardname) {
		Connection conn = openConnection();
		DSLContext context = DSL.using(conn, SQLDialect.MYSQL);
		CardStapleInfo cardStapleInfo = context.selectFrom(table)
				.where(field(FIELD_CARDNAME)
				.eq(cardname)).fetchOneInto(CardStapleInfo.class);
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return cardStapleInfo;
	}
	
	@Override
	public List<CardStapleInfo> getCardsNotNeededAnymore(int daysAgo) {
		List<Condition> conditions = new ArrayList<>();
		// missing information
		for(String format : MtgStapleChecker.formats) {
			conditions.add(field(format).isNotNull());
		}
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_YEAR, -daysAgo);
		conditions.add(field(FIELD_TIMESTAMP).greaterThan(calendar.getTime()));
		Connection connection = openConnection();
		DSLContext context = DSL.using(connection, SQLDialect.MYSQL);
		List<CardStapleInfo> cardstapleinfos = context
				.selectFrom(table)
				.where(conditions)
				.fetchInto(CardStapleInfo.class);
		try {
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return cardstapleinfos;
	}

}
