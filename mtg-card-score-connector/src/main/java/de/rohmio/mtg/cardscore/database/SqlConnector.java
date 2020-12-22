package de.rohmio.mtg.cardscore.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import org.jooq.Table;
import org.jooq.impl.DSL;

import de.rohm.io.mtg.mtgtop8.api.model.MtgTop8Format;

public class SqlConnector implements StorageConnector {

	private static Logger log = Logger.getLogger(SqlConnector.class.getName());

	private String user;
	private String password;
	private String url;
	private Table<Record> TABLE;

	public SqlConnector(DatabaseConfig config) {
		this(config.getHost(),
			config.getPort(),
			config.getUser(),
			config.getPassword(),
			config.getDatabase(),
			config.getTable());
	}

	public SqlConnector(String host, int port, String user, String password, String database, String tableName) {
		this.user = user;
		this.password = password;

		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		TABLE = DSL.table(tableName);
		url = String.format("jdbc:mysql://%s:%s/%s?serverTimezone=GMT", host, port, database);
	}

	// as callback so the actual method does not have to deal with closing the connection
	private <T> T doOperation(Function<DSLContext, T> function) {
		T result = null;
		try (Connection connection = DriverManager.getConnection(url, user, password)) {
			DSLContext context = DSL.using(connection, SQLDialect.MYSQL);
			result = function.apply(context);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public void addDataset(CardStapleInfo cardStapleInfo) {
		Map<Field<?>, Object> setMap = new HashMap<>();
		setMap.put(CardStapleInfo.CARDNAME, cardStapleInfo.getCardName());
		setMap.put(CardStapleInfo.TIMESTAMP, cardStapleInfo.getTimestamp());
		for(MtgTop8Format format : MtgTop8Format.values()) {
			Integer formatScore = cardStapleInfo.getFormatScore(format);
			if(formatScore != null) {
				setMap.put(CardStapleInfo.FORMAT_FIELD.get(format), formatScore);
			}
		}
		
		doOperation(context -> {
			InsertOnDuplicateSetMoreStep<Record> call = context
					.insertInto(TABLE)
					.set(setMap)
					.onDuplicateKeyUpdate()
					.set(setMap);
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
		String[] cardnamesArray = cardnames.toArray(new String[cardnames.size()]);
		return doOperation(context -> {
			List<CardStapleInfo> result = context
					.selectFrom(TABLE)
					.where(CardStapleInfo.CARDNAME.eq(DSL.any(cardnamesArray)))
					.fetchInto(CardStapleInfo.class);
			return result;
		});
	}

	@Override
	public CardStapleInfo getCardStapleInfo(String cardname) {
		return doOperation(context -> {
			CardStapleInfo cardStapleInfo = context
					.selectFrom(TABLE)
					.where(CardStapleInfo.CARDNAME.eq(cardname))
					.fetchOneInto(CardStapleInfo.class);
			return cardStapleInfo;
		});
	}

	@Override
	public List<CardStapleInfo> getCardsNotNeeded(int daysAgo, List<MtgTop8Format> formats) {
		List<Condition> conditions = new ArrayList<>();
		// missing information
		for (MtgTop8Format format : formats) {
			Field<Integer> field = CardStapleInfo.FORMAT_FIELD.get(format);
			conditions.add(field.isNotNull());
		}
		LocalDateTime dateTime = LocalDateTime.now();
		dateTime.minusDays(daysAgo);
		
		conditions.add(CardStapleInfo.TIMESTAMP.greaterThan(dateTime));

		return doOperation(context -> {
			List<CardStapleInfo> cardstapleinfos = context
					.selectFrom(TABLE)
					.where(conditions)
					.fetchInto(CardStapleInfo.class);
			return cardstapleinfos;
		});

	}

}
