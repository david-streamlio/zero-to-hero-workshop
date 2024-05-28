package io.streamnative.coinbase.flink.joins.stream;

import io.streamnative.coinbase.schema.Ticker;
import io.streamnative.coinbase.schema.TickerFeatures;
import io.streamnative.coinbase.schema.TickerStats;
import io.streamnative.flink.utils.CoinbaseTopicProvider;
import io.streamnative.flink.utils.FlinkUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;

@Slf4j
public class TickerFeatureExtractionJob extends CoinbaseTopicProvider {

    public TickerFeatureExtractionJob(String brokerServiceUrl, String adminServiceUrl) {
        super(brokerServiceUrl, adminServiceUrl);
    }

    public static void main(String[] args) throws Exception {
        final ParameterTool parameterTool = ParameterTool.fromArgs(args);

        if (parameterTool.getNumberOfParameters() < 2) {
            System.err.println("Missing parameters!");
            return;
        }

        // Create execution environment
        StreamExecutionEnvironment env = FlinkUtils.getEnvironment(parameterTool);
        String brokerServiceUrl = parameterTool.getRequired("broker-service-url");
        String adminServiceUrl = parameterTool.getRequired("admin-service-url");

        TickerFeatureExtractionJob job = new TickerFeatureExtractionJob(brokerServiceUrl, adminServiceUrl);

        DataStream<Ticker> tickerDataStream = env.fromSource(job.getTickerSource(),
                WatermarkStrategy.noWatermarks(),
                "coinbase-ticker");

        DataStream<TickerStats> tickerStatsDataStream = env.fromSource(job.getTickerStatsSource(),
                WatermarkStrategy.noWatermarks(),
                "coinbase-ticker-stats");

        DataStream<TickerStats> keyedTickerStatsStream = tickerStatsDataStream.keyBy(TickerStats::getSequence);

        // Define a state descriptor for the broadcast state
        MapStateDescriptor<Long, Ticker> broadcastStateDescriptor = new MapStateDescriptor<>(
                "broadcastState",
                TypeInformation.of(Long.class),
                TypeInformation.of(Ticker.class)
        );

        // Broadcast
        BroadcastStream<Ticker> tickerBroadcastStream = tickerDataStream.broadcast(broadcastStateDescriptor);

        DataStream<TickerFeatures> joinedStream = keyedTickerStatsStream
                .connect(tickerBroadcastStream)
                .process(new KeyedBroadcastProcessFunction<Long, TickerStats, Ticker, TickerFeatures>() {

                    private transient MapState<Long, Ticker> broadcastState;
                    private transient ValueState<TickerStats> keyedState;

                    @Override
                    public void open(Configuration parameters) throws Exception {
                        broadcastState = getRuntimeContext().getMapState(broadcastStateDescriptor);
                        ValueStateDescriptor<TickerStats> descriptor = new ValueStateDescriptor<>(
                                "keyedState",
                                TypeInformation.of(TickerStats.class)
                        );

                        keyedState = getRuntimeContext().getState(descriptor);
                    }

                    @Override
                    public void processElement(TickerStats tickerStats,
                                               KeyedBroadcastProcessFunction<Long, TickerStats, Ticker, TickerFeatures>.ReadOnlyContext readOnlyContext,
                                               Collector<TickerFeatures> collector) throws Exception {

                        log.info("Processing: {}", tickerStats);
                        keyedState.update(tickerStats);

                        // Retrieve the corresponding value from the broadcast state
                        Ticker ticker = broadcastState.get(tickerStats.getSequence());

                        if (ticker != null) {
                            log.info("Combining {} with: {}", tickerStats, ticker);
                            collector.collect(combine(ticker, tickerStats));
                        }

                    }

                    @Override
                    public void processBroadcastElement(Ticker ticker,
                                                        KeyedBroadcastProcessFunction<Long, TickerStats, Ticker, TickerFeatures>.Context context,
                                                        Collector<TickerFeatures> collector) throws Exception {

                        log.info("Processing Broadcast element: {}", ticker);
                        broadcastState.put(ticker.getSequence(), ticker);

                        for (Long key : broadcastState.keys()) {
                            TickerStats tickerStats = keyedState.value();
                            if (tickerStats != null && key.equals(ticker.getSequence())) {
                                log.info("Combining {} with: {}", ticker, tickerStats);
                                collector.collect(combine(ticker, tickerStats));
                            }
                        }
                    }
                });

        // Write the combined records to a Pulsar topic
        joinedStream.sinkTo(job.getTickerFeaturesSink());

        env.execute("Ticker join TickerStats to Sink");
    }

    private static TickerFeatures combine(Ticker ticker, TickerStats tickerStats) {
        TickerFeatures combined =
                TickerFeatures.builder()
                        .best_ask(ticker.getBest_ask())
                        .best_ask_size(ticker.getBest_ask_size())
                        .best_bid(ticker.getBest_bid())
                        .best_bid_size(ticker.getBest_bid_size())
                        .high_24h(ticker.getHigh_24h())
                        .latest_emwa(tickerStats.getLatest_emwa())
                        .latest_std(tickerStats.getLatest_std())
                        .latest_variance(tickerStats.getLatest_variance())
                        .low_24h(ticker.getLow_24h())
                        .open_24h(ticker.getOpen_24h())
                        .price(ticker.getPrice())
                        .product_id(ticker.getProduct_id())
                        .rolling_mean(tickerStats.getRolling_mean())
                        .rolling_std(tickerStats.getRolling_std())
                        .rolling_variance(tickerStats.getRolling_variance())
                        .sequence(ticker.getSequence())
                        .side(ticker.getSide())
                        .type(ticker.getType())
                        .volume_24h(ticker.getVolume_24h())
                        .volume_30d(ticker.getVolume_30d())
                        .build();
        return combined;
    }


}
