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

import java.time.Instant;
import java.util.Properties;
import java.util.Objects;
import java.time.LocalDateTime;

public class FlinkCoinbaseMovingAverage {

    public static void main(String[] args) throws Exception {
        // Read from environment variables (with defaults)
        final String pulsarServiceUrl = System.getenv().getOrDefault("PULSAR_SERVICE_URL",
                "pulsar+ssl://pc-d7412b62.azure-usw3-production-7kcsh.test.azure.sn2.dev:6651");

        final String pulsarAdminUrl = System.getenv().getOrDefault("PULSAR_ADMIN_URL",
                "https://pc-d7412b62.azure-usw3-production-7kcsh.test.azure.sn2.dev");

        final String pulsarTopicIn = System.getenv().getOrDefault("PULSAR_INPUT_TOPIC", "persistent://feeds/realtime/coinbase-ticker");
        final String pulsarTopicOut = System.getenv().getOrDefault("PULSAR_OUTPUT_TOPIC", "persistent://feeds/realtime/moving-averages");

        final String authPlugin = "org.apache.pulsar.client.impl.auth.AuthenticationToken";
        final String jwtToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjIzMTE0Y2I5LTFkZmQtNTY5Yi04ZWNmLTQ5MWIwZTZlYTEyNCIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsidXJuOnNuOnB1bHNhcjpvLW5vYzRuOmZsaW5rZGVtbyJdLCJleHAiOjE3NTgzODQ4NTYsImh0dHBzOi8vc3RyZWFtbmF0aXZlLmlvL3Njb3BlIjpbImFjY2VzcyJdLCJodHRwczovL3N0cmVhbW5hdGl2ZS5pby91c2VybmFtZSI6ImRhdmlkLWFkbWluQG8tbm9jNG4uYXV0aC50ZXN0LmNsb3VkLmdjcC5zdHJlYW1uYXRpdmUuZGV2IiwiaWF0IjoxNzU1NzkyODU3LCJpc3MiOiJodHRwczovL3BjLWQ3NDEyYjYyLmF6dXJlLXVzdzMtcHJvZHVjdGlvbi03a2NzaC50ZXN0LmF6dXJlLnNuMi5kZXYvYXBpa2V5cy8iLCJqdGkiOiIxNjMxNjEyNDQ4MzQ0ZmY3YWFkODZlMzI3YWY2NGI4MiIsInBlcm1pc3Npb25zIjpbXSwic3ViIjoiSmVaU1dzSUNpMTVrOVc0UDFNUlRYS3IxbVVWUU56cW5AY2xpZW50cyJ9.boVUtfzNJIcX8iC1-HLHlnkBEsxkSapLuzt-pTCSnvuMZb124mUx0H9gZeSNEPZDPDuQYKZaF9FPr9B_ceqF4d3w91n8qG3wMjLiiw_yfi8zwxou38nMed0oV9Ctw6BTA90m0p1PJMFVt3ElSSBMvg2WxD8qoErNVIe8dgr8TKW9xBrbr67uz4HDfbysnqVIGd-qKKRAiJNpWO1YsHDEJ6JsDgtIv8zHUgZefC-YO_IA54AzdfRIs3kQbsCkrSSkgAnssclV4oP262TCZe8fsEJhhAod_4n_NJeDThzpmiFuhwq0SJU6oCQGqGil4lNhLFiA-BxHRXXStT5PaV6ELw";
        final String authParams = String.format("{\"token\":\"%s\"}", jwtToken);

        System.out.printf("Using Pulsar service URL: %s%n", pulsarServiceUrl);
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

        // Pulsar source
        PulsarSource<Ticker> source = PulsarSource.builder()
                .setServiceUrl(pulsarServiceUrl)
                .setAdminUrl(pulsarAdminUrl)
                .setConfig(PulsarOptions.PULSAR_AUTH_PLUGIN_CLASS_NAME, authPlugin)
                .setConfig(PulsarOptions.PULSAR_AUTH_PARAMS, authParams)
                .setStartCursor(StartCursor.earliest())
                .setTopics(pulsarTopicIn)
                .setDeserializationSchema(sourceSchema)
                .setSubscriptionName("flink-moving-average-sub")
                .build();

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

        // Pulsar sink
        PulsarSink<MovingAverage> sink = PulsarSink.builder()
                .setServiceUrl(pulsarServiceUrl)
                .setAdminUrl(pulsarAdminUrl)
                .setConfig(PulsarOptions.PULSAR_AUTH_PLUGIN_CLASS_NAME, authPlugin)
                .setConfig(PulsarOptions.PULSAR_AUTH_PARAMS, authParams)
                .setTopics(pulsarTopicOut)
                .setSerializationSchema(pulsarSerializationSchema)
                .build();

        averages.sinkTo(sink);

        env.execute("Coinbase Moving Average with Pulsar");
    }
}
