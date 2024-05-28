# Hands-On Lab #2: Real-time Data Stream Ingestion

Prerequisites
------------

- [Lab1.md](Lab1-guide.md)


### 1️⃣ Start Ingesting Crypto Market Data

Apache Pulsar provides a serverless computing framework known as [I/O connectors](https://pulsar.apache.org/docs/next/io-overview/),
that allows us to easily ingest data from external sources into Apache Pulsar. The next step in the demo is to start our
[custom Pulsar source connector](..%2F..%2Fcoinbase-functions%2Fcoinbase-live-feed) that consumes data from the
[Coinbase Websocket API](https://docs.cloud.coinbase.com/exchange/docs/websocket-overview) and publishes it onto a Pulsar
topic.

```bash
sh ./bin/pulsar/start-coinbase-feed.sh
```

This command starts the connector using the configuration details specified in the
[coinbase-feed.yaml](..%2F..%2Finfrastructure%2Fpulsar%2Ffunctions%2Fconf%2Fcoinbase-feed.yaml) configuration file.
By default, the configuration will request information for `ETH-USD and BTC-USD` from the following channels:
`ticker, rfq_matches, and auctionfeed`. See the [Coinbase Websocket API](https://docs.cloud.coinbase.com/exchange/docs/websocket-overview)
for more details.

You can verify that the Source connector is working by running the following command to consume messages from the source's
configured output topic. If it is working properly, you should see output similar to that shown here:

````bash
docker exec -it pulsar-broker sh -c   "./bin/pulsar-client consume -n 0 -p Earliest -s my-sub persistent://feeds/realtime/coinbase-livefeed"

key:[ticker], properties:[product=BTC-USD], content:{"sequence":77240199610,"product_id":"BTC-USD","price":"66050.9","open_24h":"65957.46","volume_24h":"14538.28486134","low_24h":"64500","high_24h":"66944.06","volume_30d":"649666.78865346","best_bid":"66050.90","best_bid_size":"0.02475322","best_ask":"66053.11","best_ask_size":"0.00013000","side":"sell","time":"2024-04-03T19:04:05.198757Z","trade_id":626114466,"last_size":"0.00565955"}
----- got message -----
key:[ticker], properties:[product=BTC-USD], content:{"sequence":77240199612,"product_id":"BTC-USD","price":"66050.9","open_24h":"65957.46","volume_24h":"14538.28514778","low_24h":"64500","high_24h":"66944.06","volume_30d":"649666.78893990","best_bid":"66050.90","best_bid_size":"0.02446678","best_ask":"66053.11","best_ask_size":"0.00013000","side":"sell","time":"2024-04-03T19:04:05.198757Z","trade_id":626114467,"last_size":"0.00028644"}
----- got message -----
key:[ticker], properties:[product=BTC-USD], content:{"sequence":77240199619,"product_id":"BTC-USD","price":"66053.11","open_24h":"65957.46","volume_24h":"14538.28527778","low_24h":"64500","high_24h":"66944.06","volume_30d":"649666.78906990","best_bid":"66050.90","best_bid_size":"0.02446678","best_ask":"66055.99","best_ask_size":"0.00271222","side":"buy","time":"2024-04-03T19:04:05.200346Z","trade_id":626114468,"last_size":"0.00013"}
----- got message -----
key:[ticker], properties:[product=BTC-USD], content:{"sequence":77240199928,"product_id":"BTC-USD","price":"66055.99","open_24h":"65957.46","volume_24h":"14538.28537778","low_24h":"64500","high_24h":"66944.06","volume_30d":"649666.78916990","best_bid":"66053.21","best_bid_size":"0.00077910","best_ask":"66055.99","best_ask_size":"0.00261222","side":"buy","time":"2024-04-03T19:04:05.262708Z","trade_id":626114469,"last_size":"0.0001"}
...
````

Now that we have confirmed that the raw Crypto market data feed is bringing data into Pulsar, the next step is to
utilize Pulsar Functions to perform lightweight processing on the incoming cryptocurrency data streams.
