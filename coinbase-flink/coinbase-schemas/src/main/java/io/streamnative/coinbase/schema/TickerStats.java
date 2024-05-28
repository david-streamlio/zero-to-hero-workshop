package io.streamnative.coinbase.schema;

import lombok.Data;

@Data
public class TickerStats {
    private long sequence;
    private String product_id;
    private float price;
    private float latest_emwa;
    private float latest_std;
    private float latest_variance;
    private float rolling_mean;
    private float rolling_std;
    private float rolling_variance;
    private String time;
}
