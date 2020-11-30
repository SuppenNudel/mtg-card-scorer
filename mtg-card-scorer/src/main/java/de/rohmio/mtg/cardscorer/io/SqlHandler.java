package de.rohmio.mtg.cardscorer.io;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.InsertOnDuplicateSetMoreStep;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import de.rohmio.mtg.cardscorer.MtgCardScorer;
import de.rohmio.mtg.cardscorer.config.DatabaseConfig;
import de.rohmio.mtg.cardscorer.model.CardStapleInfo;
import de.rohmio.mtg.scryfall.api.model.Format;

public class SqlHandler implements IOHandler {

	private static Logger log = Logger.getLogger(SqlHandler.class.getName());

	private String host;
	private int port;
	private String user;
	private String password;
	private String database;
	private String table;

	private String url;

	public SqlHandler(DatabaseConfig config) {
		this(config.getHost(), config.getPort(), config.getUser(), config.getPassword(), config.getDatabase(),
				config.getTable());
	}

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

	private <T> T doOperation(Function<DSLContext, T> function) {
		// Connection is the only JDBC resource that we need
		// PreparedStatement and ResultSet are handled by jOOQ, internally
		T result = null;
		try {
			Connection connection = DriverManager.getConnection(url, user, password);
			DSLContext context = DSL.using(connection, SQLDialect.MYSQL);
			result = function.apply(context);
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public void addDataset(CardStapleInfo cardStapleInfo) throws IOException {
		Map<Field<Object>, Object> setMap = new HashMap<>();

		setMap.put(DSL.field("cardname"), cardStapleInfo.getCardname());
		setMap.put(DSL.field("timestamp"), cardStapleInfo.getTimestamp().getTime());

		for (Format format : Format.values()) {
			Integer formatScore = cardStapleInfo.getFormatScore(format);
			if (formatScore == null) {
				continue;
			}
			setMap.put(DSL.field(format.name()), formatScore);
		}

		doOperation(context -> {
			InsertOnDuplicateSetMoreStep<Record> call = context.insertInto(DSL.table(table)).set(setMap).onDuplicateKeyUpdate().set(setMap);
			try {
				call.execute();
				System.out.println("Added Dataset to Database: " + cardStapleInfo);
			} catch (Exception e) {
				log.severe("Sql statement failed: " + call.toString());
				e.printStackTrace();
			}
			return null;
		});
	}

	@Override
	public List<CardStapleInfo> getCardStapleInfos(Collection<String> cardnames) {
		Object[] cardnamesArray = cardnames.toArray(new String[cardnames.size()]);
		return doOperation(context -> {
			List<CardStapleInfo> result = context
					.selectFrom(table)
					.where(DSL.field(FIELD_CARDNAME).eq(DSL.any(cardnamesArray)))
					.fetchInto(CardStapleInfo.class);
			return result;
		});
	}

	@Override
	public CardStapleInfo getCardStapleInfo(String cardname) {
		return doOperation(context -> {
			CardStapleInfo cardStapleInfo = context.selectFrom(table).where(DSL.field(FIELD_CARDNAME).eq(cardname))
					.fetchOneInto(CardStapleInfo.class);
			return cardStapleInfo;
		});
	}

	@Override
	public List<CardStapleInfo> getCardsNotNeeded(int daysAgo) {
		List<Condition> conditions = new ArrayList<>();
		// missing information
		for (String format : MtgCardScorer.formats) {
			conditions.add(DSL.field(format).isNotNull());
		}
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_YEAR, -daysAgo);
		conditions.add(DSL.field(FIELD_TIMESTAMP).greaterThan(calendar.getTime()));

		return doOperation(context -> {
			List<CardStapleInfo> cardstapleinfos = context.selectFrom(table).where(conditions)
					.fetchInto(CardStapleInfo.class);
			return cardstapleinfos;
		});

	}

}
