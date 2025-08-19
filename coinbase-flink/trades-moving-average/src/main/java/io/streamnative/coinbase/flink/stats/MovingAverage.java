package io.streamnative.coinbase.flink.stats;

public class MovingAverage {
    public String symbol;
    public double averagePrice;
    public long windowEnd;

    public MovingAverage() { }

    public MovingAverage(String symbol, double averagePrice, long windowEnd) {
        this.symbol = symbol;
        this.averagePrice = averagePrice;
        this.windowEnd = windowEnd;
    }
}