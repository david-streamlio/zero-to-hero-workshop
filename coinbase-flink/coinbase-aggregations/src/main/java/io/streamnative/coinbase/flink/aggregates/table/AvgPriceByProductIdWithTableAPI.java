package io.streamnative.coinbase.flink.aggregates.table;

import io.streamnative.flink.utils.FlinkUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

@Slf4j
public class AvgPriceByProductIdWithTableAPI {

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

        String sql = String.format("SELECT\n" +
                "    product_id,\n" +
                "    TUMBLE_START(`time`, INTERVAL '1' MINUTE) AS window_start,\n" +
                "    TUMBLE_END(`time`, INTERVAL '1' MINUTE) AS window_end," +
                "    AVG(price) AS avg_price,\n" +
                "    MIN(price) AS min_price,\n" +
                "    MAX(price) AS max_price\n" +
                "FROM ticker \n" +
                "GROUP BY product_id," +
                "TUMBLE(`time`, INTERVAL '1' MINUTE)");
        log.info("Executing: {}", sql);
        TableResult results = tableEnv.executeSql(sql);

        results.print();
    }
}
