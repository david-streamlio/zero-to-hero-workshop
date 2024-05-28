package io.streamnative.flink.utils;

import io.streamnative.coinbase.schema.Ticker;
import io.streamnative.coinbase.schema.TickerFeatures;
import io.streamnative.coinbase.schema.TickerStats;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.pulsar.sink.PulsarSink;
import org.apache.flink.connector.pulsar.source.PulsarSource;
import org.apache.flink.connector.pulsar.source.enumerator.cursor.StartCursor;
import org.apache.flink.connector.pulsar.source.enumerator.cursor.StopCursor;
import org.apache.pulsar.client.api.Schema;

import java.util.UUID;

public class CoinbaseTopicProvider {

    private String brokerServiceUrl, adminServiceUrl;
    private UUID uuid = UUID.randomUUID();

    public CoinbaseTopicProvider(String brokerServiceUrl, String adminServiceUrl) {
        this.adminServiceUrl = adminServiceUrl;
        this.brokerServiceUrl = brokerServiceUrl;
    }

    public PulsarSource<Ticker> getTickerSource() {
        return PulsarSource.builder()
                .setServiceUrl(this.brokerServiceUrl)
                .setAdminUrl(this.adminServiceUrl)
                .setStartCursor(StartCursor.latest())
                .setUnboundedStopCursor(StopCursor.never())
                .setTopics("persistent://feeds/realtime/coinbase-ticker")
                .setDeserializationSchema(Schema.JSON(Ticker.class), Ticker.class)
                .setSubscriptionName(getSubName())
                .setConsumerName(getConsumerName())
                .build();
    }

    public PulsarSource<TickerStats> getTickerStatsSource() {
        return PulsarSource.builder()
                .setServiceUrl(this.brokerServiceUrl)
                .setAdminUrl(this.adminServiceUrl)
                .setStartCursor(StartCursor.latest())
                .setUnboundedStopCursor(StopCursor.never())
                .setTopics("persistent://feeds/realtime/coinbase-ticker-stats")
                .setDeserializationSchema(Schema.JSON(TickerStats.class), TickerStats.class)
                .setSubscriptionName(getSubName())
                .setConsumerName(getConsumerName())
                .build();
    }

    public PulsarSink<TickerFeatures> getTickerFeaturesSink() {
        return PulsarSink.builder()
                .setServiceUrl(this.brokerServiceUrl)
                .setAdminUrl(this.adminServiceUrl)
                .setTopics("persistent://feeds/realtime/ticker-features")
                .setProducerName("flink-ticker-features-sink-%s")
                .setSerializationSchema(Schema.JSON(TickerFeatures.class), TickerFeatures.class)
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();
    }

    private String getSubName() {
        return String.format("flink-source-%s", uuid.toString().substring(0, 10));
    }

    private String getConsumerName() {
        return String.format("flink-consumer-%s", uuid.toString().substring(0, 10));
    }
}
