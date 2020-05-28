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

public class SqlHandler implements IOHandler {
	
	private static Logger log = Logger.getLogger(SqlHandler.class.getName());

	private String host;
	private String port;
	private String user;
	private String password;
	private String database;
	private String table;

	private DSLContext context;
	
	public SqlHandler(String host, String port, String user, String password, String database, String table) {
		this.host = host;
		this.port = port;
		this.user = user;
		this.password = password;
		this.database = database;
		this.table = table;
	}

	@Override
	public void init(List<String> titles) throws IOException {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} 
		String url = String.format("jdbc:mysql://%s:%s/%s?serverTimezone=GMT", host, port, database);

		// Connection is the only JDBC resource that we need
		// PreparedStatement and ResultSet are handled by jOOQ, internally
		try {
			Connection connection = DriverManager.getConnection(url, user, password);
			context = DSL.using(connection, SQLDialect.MYSQL);
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public void addDataset(Map<String, String> values) throws IOException {
		Map<Field<Object>, Object> setMap = new HashMap<>();
		for(String key : values.keySet()) {
			String value = values.get(key);
			if(value == null) {
				continue;
			}
			Field<Object> field = field(key);
			setMap.put(field, value);
		}
		
		@SuppressWarnings("unchecked")
		Field<Object>[] fieldsArr = setMap.keySet().toArray(new Field[setMap.size()]);
		Object[] valuesArr = setMap.values().toArray(new Object[setMap.size()]);
		InsertOnDuplicateSetMoreStep<Record> call = context.insertInto(table(table), fieldsArr).values(valuesArr).onDuplicateKeyUpdate().set(setMap);
		try {
			call.execute();
		} catch (Exception e) {
			log.severe("Sql statement failed: "+call.toString());
			e.printStackTrace();
		}
	}
	
	@Override
	public CardStapleInfo getCardStapleInfo(String cardname) {
		CardStapleInfo cardStapleInfo = context.selectFrom(table)
				.where(field(MtgStapleChecker.FIELD_CARDNAME)
				.eq(cardname)).fetchOneInto(CardStapleInfo.class);
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
		conditions.add(field(MtgStapleChecker.FIELD_TIMESTAMP).greaterThan(calendar.getTime()));
		List<CardStapleInfo> cardstapleinfos = context
				.selectFrom(table)
				.where(conditions)
				.fetchInto(CardStapleInfo.class);
		return cardstapleinfos;
	}

}
