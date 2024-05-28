package io.streamnative.coinbase.flink.filter.streams;

import io.streamnative.coinbase.schema.Ticker;
import io.streamnative.coinbase.schema.TickerStats;
import io.streamnative.flink.utils.CoinbaseTopicProvider;
import io.streamnative.flink.utils.FlinkUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;

import java.util.concurrent.TimeUnit;

@Slf4j
public class MultiFilterTickerStats extends CoinbaseTopicProvider {
    public MultiFilterTickerStats(String brokerServiceUrl, String adminServiceUrl) {
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
        String outputPath = parameterTool.getRequired("output-path");
        Float price = Float.parseFloat(parameterTool.get("price"));
        Float variance = Float.parseFloat(parameterTool.get("variance"));

        MultiFilterTickerStats filter =
                new MultiFilterTickerStats(brokerServiceUrl, adminServiceUrl);

        DataStream<TickerStats> tickerStatsDataStream =
                env.fromSource(filter.getTickerStatsSource(),
                WatermarkStrategy.noWatermarks(),
                "coinbase-filter-ticker-stats");

        final FileSink<String> sink = FileSink
                .forRowFormat(new Path(outputPath), new SimpleStringEncoder<String>("UTF-8"))
                .withRollingPolicy(
                        DefaultRollingPolicy.builder()
                                .withRolloverInterval(TimeUnit.MINUTES.toMillis(15))
                                .withInactivityInterval(TimeUnit.MINUTES.toMillis(5))
                                .withMaxPartSize(1024 * 1024 * 1024)
                                .build())
                .build();

        tickerStatsDataStream.filter(stats -> {
            boolean result = (stats.getPrice() > price)
                    && (stats.getLatest_variance() > variance);
            if (result) {
                log.info("Filtering stats: {}", stats);
            }
            return result;
        }).map(stats -> stats.toString()).sinkTo(sink);

        env.execute(String.format("TickerStats Filter for price %.3f%n and variance %.3f%n", price, variance));
    }
}
