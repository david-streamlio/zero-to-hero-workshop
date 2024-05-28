package io.streamnative.coinbase.flink.filter.table;

import io.streamnative.flink.utils.CoinbaseTopicProvider;
import io.streamnative.flink.utils.FlinkUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

@Slf4j
public class FilterWithTableAPI {

    public static void main(String[] args) throws Exception {
        final ParameterTool parameterTool = ParameterTool.fromArgs(args);

        if (parameterTool.getNumberOfParameters() < 3) {
            System.err.println("Missing parameters!");
            return;
        }

        StreamExecutionEnvironment env = FlinkUtils.getEnvironment(parameterTool);
        String brokerServiceUrl = parameterTool.getRequired("broker-service-url");
        String adminServiceUrl = parameterTool.getRequired("admin-service-url");
        String productId = parameterTool.getRequired("product-id");

        StreamTableEnvironment tableEnv = FlinkUtils.getTableEnvironment(parameterTool);

        String sql = String.format("SELECT * from ticker where product_id = '%s'",productId);
        log.info("Executing: {}", sql);
        TableResult results = tableEnv.executeSql(sql);

        results.print();
    }
}
