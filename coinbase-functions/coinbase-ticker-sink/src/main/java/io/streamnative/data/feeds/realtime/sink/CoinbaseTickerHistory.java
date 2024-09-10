package io.streamnative.data.feeds.realtime.sink;

import org.apache.pulsar.functions.api.Context;
import org.apache.pulsar.functions.api.Function;
import org.slf4j.Logger;
import java.sql.*;
import io.streamnative.data.feeds.realtime.coinbase.channels.Ticker;

public class CoinbaseTickerHistory implements Function<Ticker, Void> {
    private Connection conn;
    private Logger LOG;
    private String jdbcUrl;
    private String sql;
    @Override
    public void initialize(Context ctx) throws Exception {
        Function.super.initialize(ctx);
        this.LOG = ctx.getLogger();
        this.jdbcUrl = (String) ctx.getUserConfigValue("jdbcUrl").get();
        this.sql = (String) ctx.getUserConfigValue("sql").get();
        if (jdbcUrl == null || sql == null) {
            throw new RuntimeException("Invalid configuration");
        }
    }

    @Override
    public Void process(Ticker input, Context context) throws Exception {

        PreparedStatement st = null;

        try {
            st = getConn().prepareStatement(sql);

            st.setString(1, input.getProduct_id());
            st.setTimestamp(2, new Timestamp(input.getMillis()));
            st.setFloat(3, input.getPrice());

            st.executeUpdate();
            st.close();
            LOG.info("Input completed: " + input);

        } catch (final SQLException e) {
            LOG.error("SQL error occurred: " + e.getMessage());
        }

        return null;
    }

    @Override
    public void close() throws Exception {
        Function.super.close();
    }

    private Connection getConn() throws SQLException {
        if (conn == null) {
            conn = DriverManager.getConnection(jdbcUrl);
            conn.setAutoCommit(true);
        }

        return conn;
    }
}
