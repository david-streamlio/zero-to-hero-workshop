package io.streamnative.coinbase.flink.joins.table;

import io.streamnative.flink.utils.FlinkUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

public class JoinTickerAndTickerStatsWithTableAPI {

    public static void main(String[] args) throws Exception {
        final ParameterTool parameterTool = ParameterTool.fromArgs(args);

        if (parameterTool.getNumberOfParameters() < 2) {
            System.err.println("Missing parameters!");
            return;
        }

        StreamExecutionEnvironment env = FlinkUtils.getEnvironment(parameterTool);
        String brokerServiceUrl = parameterTool.getRequired("broker-service-url");
        String adminServiceUrl = parameterTool.getRequired("admin-service-url");

        StreamTableEnvironment tableEnv = FlinkUtils.getTableEnvironment(parameterTool);

        String sql = String.format("select * from ticker T " +
                "LEFT JOIN ticker_stats S ON T.sequence = S.sequence " +
                "LIMIT 100");
        TableResult results = tableEnv.executeSql(sql);

        results.print();
    }
}
