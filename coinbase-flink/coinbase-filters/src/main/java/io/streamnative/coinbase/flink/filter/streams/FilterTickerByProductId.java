package io.streamnative.coinbase.flink.filter.streams;

import io.streamnative.coinbase.schema.Ticker;
import io.streamnative.flink.utils.CoinbaseTopicProvider;
import io.streamnative.flink.utils.FlinkUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.core.fs.Path;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;


import java.util.concurrent.TimeUnit;

@Slf4j
public class FilterTickerByProductId extends CoinbaseTopicProvider {

    public FilterTickerByProductId(String brokerServiceUrl, String adminServiceUrl) {
        super(brokerServiceUrl, adminServiceUrl);
    }

    public static void main(String[] args) throws Exception {
        final ParameterTool parameterTool = ParameterTool.fromArgs(args);

        if (parameterTool.getNumberOfParameters() < 4) {
            System.err.println("Missing parameters!");
            return;
        }

        StreamExecutionEnvironment env = FlinkUtils.getEnvironment(parameterTool);
        String brokerServiceUrl = parameterTool.getRequired("broker-service-url");
        String adminServiceUrl = parameterTool.getRequired("admin-service-url");
        String symbol = parameterTool.getRequired("productId");
        String outputPath = parameterTool.getRequired("output-path");

        FilterTickerByProductId filter =
                new FilterTickerByProductId(brokerServiceUrl, adminServiceUrl);

        DataStream<Ticker> tickerDataStream = env.fromSource(filter.getTickerSource(),
                WatermarkStrategy.noWatermarks(),
                "coinbase-filter-ticker");

        final FileSink<String> sink = FileSink
                .forRowFormat(new Path(outputPath), new SimpleStringEncoder<String>("UTF-8"))
                .withRollingPolicy(
                        DefaultRollingPolicy.builder()
                                .withRolloverInterval(TimeUnit.MINUTES.toMillis(15))
                                .withInactivityInterval(TimeUnit.MINUTES.toMillis(5))
                                .withMaxPartSize(1024 * 1024 * 1024)
                                .build())
                .build();

        tickerDataStream.filter(ticker -> {
            boolean result = ticker.getProduct_id().equalsIgnoreCase(symbol);
            if (result) {
                log.info("Filtering ticker: {}", ticker);
            }
            return result;
        }).map(ticker -> ticker.toString()).sinkTo(sink);

        env.execute(String.format("Ticker Filter for %s", symbol));
    }
}
