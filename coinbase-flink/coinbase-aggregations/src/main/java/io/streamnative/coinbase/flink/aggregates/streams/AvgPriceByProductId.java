package io.streamnative.coinbase.flink.aggregates.streams;

import io.streamnative.coinbase.schema.Ticker;
import io.streamnative.flink.utils.CoinbaseTopicProvider;
import io.streamnative.flink.utils.FlinkUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.util.concurrent.TimeUnit;

public class AvgPriceByProductId extends CoinbaseTopicProvider {

    public AvgPriceByProductId(String brokerServiceUrl, String adminServiceUrl) {
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
        Integer timeWindow = Integer.parseInt(parameterTool.get("time-window"));
        String outputPath = parameterTool.getRequired("output-path");

        AvgPriceByProductId aggregate = new AvgPriceByProductId(brokerServiceUrl, adminServiceUrl);

        final FileSink<String> sink = FileSink
                .forRowFormat(new Path(outputPath),
                        new SimpleStringEncoder<String>("UTF-8"))
                .withRollingPolicy(
                        DefaultRollingPolicy.builder()
                                .withRolloverInterval(TimeUnit.MINUTES.toMillis(15))
                                .withInactivityInterval(TimeUnit.MINUTES.toMillis(5))
                                .withMaxPartSize(1024 * 1024 * 1024)
                                .build())
                .build();

        DataStream<Ticker> tickerDataStream = env.fromSource(aggregate.getTickerSource(),
                WatermarkStrategy.<Ticker>forMonotonousTimestamps()
                        .withTimestampAssigner((Ticker t, long recordTimestamp) -> t.getMillis()),
                "coinbase-ticker");

        tickerDataStream.
                keyBy(Ticker::getProduct_id)
                .timeWindow(Time.minutes(timeWindow))
                .aggregate(new AveragePriceAggregate())
                .map(tuple -> tuple.toString())
                .sinkTo(sink);

        env.execute("Average Price by Product");
    }

    @Slf4j
    // Inner class for the aggregation function
    public static class AveragePriceAggregate implements AggregateFunction<Ticker, Tuple2<Double, Integer>, Tuple2<String, Double>> {
        @Override
        public Tuple2<Double, Integer> createAccumulator() {
            return new Tuple2<>(0.0, 0);
        }

        @Override
        public Tuple2<Double, Integer> add(Ticker value, Tuple2<Double, Integer> accumulator) {
            log.info("Adding: {}", value);
            return new Tuple2<>(accumulator.f0 + value.getPrice(), accumulator.f1 + 1);
        }

        @Override
        public Tuple2<String, Double> getResult(Tuple2<Double, Integer> accumulator) {
            log.info("Result for: {}", accumulator);
            return new Tuple2<>("product_id", accumulator.f0 / accumulator.f1); // Adjust as needed to include symbol
        }

        @Override
        public Tuple2<Double, Integer> merge(Tuple2<Double, Integer> a, Tuple2<Double, Integer> b) {
            return new Tuple2<>(a.f0 + b.f0, a.f1 + b.f1);
        }
    }

}
