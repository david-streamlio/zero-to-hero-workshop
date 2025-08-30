package io.streamnative.flink.utils;

import io.streamnative.coinbase.schema.Ticker;
import io.streamnative.coinbase.schema.TickerFeatures;
import io.streamnative.coinbase.schema.TickerStats;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.pulsar.common.config.PulsarOptions;
import org.apache.flink.connector.pulsar.sink.PulsarSink;
import org.apache.flink.connector.pulsar.source.PulsarSource;
import org.apache.flink.connector.pulsar.source.enumerator.cursor.StartCursor;
import org.apache.flink.connector.pulsar.source.enumerator.cursor.StopCursor;
import org.apache.pulsar.client.api.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class CoinbaseTopicProvider {

    private static final Logger LOG = LoggerFactory.getLogger(CoinbaseTopicProvider.class);
    
    private final PulsarConfig pulsarConfig;
    private final ConfigurationProvider configProvider;
    private final UUID uuid = UUID.randomUUID();

    public CoinbaseTopicProvider(ConfigurationProvider configProvider) {
        this.configProvider = configProvider;
        this.pulsarConfig = configProvider.getPulsarConfig();
        LOG.info("Initialized CoinbaseTopicProvider with config: {}", pulsarConfig);
    }
    
    // Backward compatibility constructor
    @Deprecated
    public CoinbaseTopicProvider(String brokerServiceUrl, String adminServiceUrl) {
        this.pulsarConfig = new PulsarConfig(brokerServiceUrl, adminServiceUrl, 
                null, null, "feeds", "realtime");
        this.configProvider = null;
        LOG.warn("Using deprecated constructor. Consider migrating to ConfigurationProvider-based constructor.");
    }

    public PulsarSource<Ticker> getTickerSource() {
        var builder = PulsarSource.builder()
                .setServiceUrl(pulsarConfig.getServiceUrl())
                .setAdminUrl(pulsarConfig.getAdminUrl())
                .setStartCursor(StartCursor.latest())
                .setUnboundedStopCursor(StopCursor.never())
                .setTopics(getTopicName("pulsar.topics.ticker", "coinbase-ticker"))
                .setDeserializationSchema(Schema.JSON(Ticker.class), Ticker.class)
                .setSubscriptionName(getSubName())
                .setConsumerName(getConsumerName());
        
        // Add authentication if configured
        if (pulsarConfig.hasAuth()) {
            builder.setConfig(PulsarOptions.PULSAR_AUTH_PLUGIN_CLASS_NAME, pulsarConfig.getAuthPlugin())
                   .setConfig(PulsarOptions.PULSAR_AUTH_PARAMS, pulsarConfig.getAuthParams());
        }
        
        return builder.build();
    }

    public PulsarSource<TickerStats> getTickerStatsSource() {
        var builder = PulsarSource.builder()
                .setServiceUrl(pulsarConfig.getServiceUrl())
                .setAdminUrl(pulsarConfig.getAdminUrl())
                .setStartCursor(StartCursor.latest())
                .setUnboundedStopCursor(StopCursor.never())
                .setTopics(getTopicName("pulsar.topics.ticker.stats", "coinbase-ticker-stats"))
                .setDeserializationSchema(Schema.JSON(TickerStats.class), TickerStats.class)
                .setSubscriptionName(getSubName())
                .setConsumerName(getConsumerName());
        
        // Add authentication if configured
        if (pulsarConfig.hasAuth()) {
            builder.setConfig(PulsarOptions.PULSAR_AUTH_PLUGIN_CLASS_NAME, pulsarConfig.getAuthPlugin())
                   .setConfig(PulsarOptions.PULSAR_AUTH_PARAMS, pulsarConfig.getAuthParams());
        }
        
        return builder.build();
    }

    public PulsarSink<TickerFeatures> getTickerFeaturesSink() {
        var builder = PulsarSink.builder()
                .setServiceUrl(pulsarConfig.getServiceUrl())
                .setAdminUrl(pulsarConfig.getAdminUrl())
                .setTopics(getTopicName("pulsar.topics.ticker.features", "ticker-features"))
                .setProducerName(getProducerName("ticker-features"))
                .setSerializationSchema(Schema.JSON(TickerFeatures.class), TickerFeatures.class)
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE);
        
        // Add authentication if configured
        if (pulsarConfig.hasAuth()) {
            builder.setConfig(PulsarOptions.PULSAR_AUTH_PLUGIN_CLASS_NAME, pulsarConfig.getAuthPlugin())
                   .setConfig(PulsarOptions.PULSAR_AUTH_PARAMS, pulsarConfig.getAuthParams());
        }
        
        return builder.build();
    }


    private String getTopicName(String propertyKey, String defaultTopic) {
        String topicName;
        if (configProvider != null) {
            topicName = configProvider.getProperty(propertyKey, null, defaultTopic);
        } else {
            topicName = defaultTopic;
        }
        
        // Return full topic name with tenant/namespace prefix
        return pulsarConfig.getTopicPrefix() + topicName;
    }
    
    private String getSubName() {
        String prefix = configProvider != null 
            ? configProvider.getProperty("pulsar.consumer.subscription.prefix", null, "flink-consumer")
            : "flink-consumer";
        return String.format("%s-%s", prefix, uuid.toString().substring(0, 10));
    }

    private String getConsumerName() {
        String prefix = configProvider != null 
            ? configProvider.getProperty("pulsar.consumer.name.prefix", null, "flink-consumer")
            : "flink-consumer";
        return String.format("%s-%s", prefix, uuid.toString().substring(0, 10));
    }
    
    private String getProducerName(String topic) {
        String prefix = configProvider != null 
            ? configProvider.getProperty("pulsar.producer.name.prefix", null, "flink-producer")
            : "flink-producer";
        return String.format("%s-%s-%s", prefix, topic, uuid.toString().substring(0, 10));
    }
    
    public PulsarConfig getPulsarConfig() {
        return pulsarConfig;
    }
}
