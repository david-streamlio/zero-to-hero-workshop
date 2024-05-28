package io.streamnative.coinbase.flink.joins.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.streamnative.coinbase.schema.TickerFeatures;
import io.streamnative.flink.utils.FlinkUtils;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.pulsar.source.PulsarSource;
import org.apache.flink.connector.pulsar.source.enumerator.cursor.StartCursor;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink;
import org.apache.flink.streaming.api.functions.sink.filesystem.bucketassigners.DateTimeBucketAssigner;
import org.apache.pulsar.client.api.Schema;

import java.util.Map;

public class CsvStream {

    public static void main(String[] args) throws Exception {

        final ParameterTool parameterTool = ParameterTool.fromArgs(args);

        if (parameterTool.getNumberOfParameters() < 2) {
            System.err.println("Missing parameters!");
            return;
        }

        StreamExecutionEnvironment env = FlinkUtils.getEnvironment(parameterTool);
        Schema<TickerFeatures> schema = Schema.JSON(TickerFeatures.class);

        // Create a Pulsar source using PulsarUtils
        PulsarSource<TickerFeatures> featureSource = PulsarSource.builder()
                .setServiceUrl("pulsar://localhost:6650")
                .setAdminUrl("http://localhost:8080")
                .setStartCursor(StartCursor.earliest())
                .setTopics("my-topic")
                .setDeserializationSchema(schema, TickerFeatures.class)
                .setSubscriptionName("my-subscription")
                .build();

        DataStream<TickerFeatures> featureStream =
                env.fromSource(featureSource, WatermarkStrategy.noWatermarks(), "Pulsar Feature Source")
                        .name("pulsarFeatureSource")
                        .uid("pulsarFeatureSource");

        // Convert JSON strings to CSV format
        DataStream<String> csvStream = featureStream.map(new MapFunction<TickerFeatures, String>() {

            private ObjectMapper objectMapper;

            @Override
            public String map(TickerFeatures features) throws Exception {
                // Implement JSON to CSV conversion logic here
                String jsonString = objectMapper.writeValueAsString(features);
                JsonNode jsonNode = objectMapper.readTree(jsonString);
                Map<String, Object> jsonData = objectMapper.convertValue(jsonNode, Map.class);
                return writeCsvRow(jsonData);
            }

            private String writeCsvRow(Map<String, Object> data) {
                StringBuilder rowBuilder = new StringBuilder();
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    rowBuilder.append(String.valueOf(entry.getValue()));
                    rowBuilder.append(",");
                }
                // Remove the trailing comma
                if (rowBuilder.length() > 0) {
                    rowBuilder.deleteCharAt(rowBuilder.length() - 1);
                }
                return rowBuilder.toString();
            }

        });

        // Write CSV data to files using a StreamingFileSink
        StreamingFileSink<String> fileSink = StreamingFileSink
                .forRowFormat(new org.apache.flink.core.fs.Path("home/david/Downloads/features.csv"),
                        new SimpleStringEncoder())
                .withBucketAssigner(new DateTimeBucketAssigner("yyyy-MM-dd--HH"))
                .build();

        csvStream.addSink(fileSink);

        // Execute the Flink job
        env.execute("Convert JSON to CSV");
    }
}
