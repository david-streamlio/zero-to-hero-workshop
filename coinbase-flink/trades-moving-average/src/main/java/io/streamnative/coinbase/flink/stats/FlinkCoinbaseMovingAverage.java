package io.streamnative.coinbase.flink.stats;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.connector.source.Source;
import org.apache.flink.api.connector.sink.Sink;
import org.apache.flink.connector.pulsar.common.config.PulsarOptions;
import org.apache.flink.connector.pulsar.source.PulsarSource;
import org.apache.flink.connector.pulsar.source.enumerator.cursor.StartCursor;
import org.apache.flink.connector.pulsar.source.reader.deserializer.PulsarDeserializationSchema;
import org.apache.flink.connector.pulsar.sink.PulsarSink;
import org.apache.flink.connector.pulsar.sink.writer.serializer.PulsarSerializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.flink.connector.pulsar.source.reader.deserializer.PulsarDeserializationSchemaWrapper;
import org.apache.flink.formats.json.JsonSerializationSchema;
import org.apache.flink.formats.json.JsonDeserializationSchema;

import org.apache.flink.connector.pulsar.sink.writer.serializer.PulsarSerializationSchema;
import org.apache.flink.connector.pulsar.sink.writer.serializer.PulsarSerializationSchemaWrapper;

import io.streamnative.coinbase.flink.stats.serde.LocalDateTimeKryoSerializer;
import io.streamnative.data.feeds.realtime.coinbase.channels.Ticker;
import io.streamnative.flink.utils.ConfigurationProvider;
import io.streamnative.flink.utils.PulsarConfig;

import java.time.Instant;
import java.util.Properties;
import java.util.Objects;
import java.time.LocalDateTime;

public class FlinkCoinbaseMovingAverage {

    public static void main(String[] args) throws Exception {
        // Initialize uniform configuration provider
        ConfigurationProvider configProvider = new ConfigurationProvider(args);
        PulsarConfig pulsarConfig = configProvider.getPulsarConfig();
        
        // Get configurable topics with defaults
        final String pulsarTopicIn = pulsarConfig.getTopicPrefix() + 
            configProvider.getProperty("pulsar.topics.ticker", "PULSAR_INPUT_TOPIC", "coinbase-ticker");
        final String pulsarTopicOut = pulsarConfig.getTopicPrefix() + 
            configProvider.getProperty("pulsar.topics.moving.averages", "PULSAR_OUTPUT_TOPIC", "moving-averages");

        System.out.printf("Using Pulsar service URL: %s%n", pulsarConfig.getServiceUrl());
        System.out.printf("Using Pulsar input topic: %s%n", pulsarTopicIn);
        System.out.printf("Using Pulsar output topic: %s%n", pulsarTopicOut);

        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.getConfig().addDefaultKryoSerializer(
                LocalDateTime.class,
                LocalDateTimeKryoSerializer.class
        );

        TypeInformation<Ticker> sourceTypeInfo = TypeExtractor.getForClass(Ticker.class);

        // Wrap the Flink JSON deserialization schema
        PulsarDeserializationSchemaWrapper<Ticker> sourceSchema = new PulsarDeserializationSchemaWrapper<>(
                new JsonDeserializationSchema<>(sourceTypeInfo)
        );

        // Pulsar source with unified configuration
        PulsarSource.Builder<Ticker> sourceBuilder = PulsarSource.builder()
                .setServiceUrl(pulsarConfig.getServiceUrl())
                .setAdminUrl(pulsarConfig.getAdminUrl())
                .setStartCursor(StartCursor.earliest())
                .setTopics(pulsarTopicIn)
                .setDeserializationSchema(sourceSchema)
                .setSubscriptionName("flink-moving-average-sub");
        
        // Add authentication if configured
        if (pulsarConfig.hasAuth()) {
            sourceBuilder.setConfig(PulsarOptions.PULSAR_AUTH_PLUGIN_CLASS_NAME, pulsarConfig.getAuthPlugin())
                         .setConfig(PulsarOptions.PULSAR_AUTH_PARAMS, pulsarConfig.getAuthParams());
        }
        
        PulsarSource<Ticker> source = sourceBuilder.build();

        DataStream<Ticker> tickers = env
                .fromSource(source, WatermarkStrategy
                        .<Ticker>forBoundedOutOfOrderness(java.time.Duration.ofSeconds(5))
                        .withTimestampAssigner((event, timestamp) -> event.getMillis()), "Pulsar Source");

        DataStream<MovingAverage> averages = tickers
                .keyBy(t -> t.getProduct_id())
                .window(SlidingEventTimeWindows.of(Time.seconds(30), Time.seconds(10)))
                .apply(new WindowFunction<Ticker, MovingAverage, String, TimeWindow>() {
                    @Override
                    public void apply(String key, TimeWindow window, Iterable<Ticker> tickers, Collector<MovingAverage> out) {
                        int count = 0;
                        double sum = 0;

                        for (Ticker t : tickers) {
                            sum += t.getPrice();
                            count++;
                        }

                        double avg = count == 0 ? 0 : sum / count;
                        MovingAverage result = new MovingAverage(key, avg, window.getEnd());
                        out.collect(result);
                    }
                });

        JsonSerializationSchema<MovingAverage> jsonSerializationSchema = new JsonSerializationSchema<>();

        PulsarSerializationSchema<MovingAverage> pulsarSerializationSchema =
                new PulsarSerializationSchemaWrapper<>(jsonSerializationSchema);

        // Pulsar sink with unified configuration
        PulsarSink.Builder<MovingAverage> sinkBuilder = PulsarSink.builder()
                .setServiceUrl(pulsarConfig.getServiceUrl())
                .setAdminUrl(pulsarConfig.getAdminUrl())
                .setTopics(pulsarTopicOut)
                .setSerializationSchema(pulsarSerializationSchema);
        
        // Add authentication if configured
        if (pulsarConfig.hasAuth()) {
            sinkBuilder.setConfig(PulsarOptions.PULSAR_AUTH_PLUGIN_CLASS_NAME, pulsarConfig.getAuthPlugin())
                       .setConfig(PulsarOptions.PULSAR_AUTH_PARAMS, pulsarConfig.getAuthParams());
        }
        
        PulsarSink<MovingAverage> sink = sinkBuilder.build();

        averages.sinkTo(sink);

        env.execute("Coinbase Moving Average with Pulsar");
    }
}
