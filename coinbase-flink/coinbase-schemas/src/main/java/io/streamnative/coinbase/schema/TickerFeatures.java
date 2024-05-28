package io.streamnative.coinbase.schema;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TickerFeatures {
    private String type;
    private long sequence;
    private String product_id;
    private float price;
    private float open_24h;
    private float volume_24h;
    private float low_24h;
    private float high_24h;
    private double volume_30d;
    private float best_bid;
    private float best_bid_size;
    private float best_ask;
    private float best_ask_size;
    private String side;
    private float latest_emwa;
    private float latest_std;
    private float latest_variance;
    private float rolling_mean;
    private float rolling_std;
    private float rolling_variance;
}
