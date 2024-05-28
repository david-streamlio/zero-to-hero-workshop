# Hands-On Lab #4: Windowing Analytics

Prerequisites
------------

- [Lab1](Lab1-guide.md)
- [Lab2](Lab2-guide.md)
- [Lab3](Lab3-guide.md)



### 1️⃣ Build the Python-Based Function
First you need to build and deploy the Python-based Function,[stats.py](..%2F..%2Finfrastructure%2Fpulsar%2Ffunctions%2Fcoinbase-ticker-stats%2Fsrc%2Fstats.py)
that calculates various price metrics for the cryptocurrencies using the Pandas library. This can be done in two easy steps.
First, you need to package the python file along with its dependencies into a zip file using the following command.

```bash
sh ./bin/pulsar/build-ticker-stats.sh

updating: coinbase-ticker-stats/ (stored 0%)
updating: coinbase-ticker-stats/deps/ (stored 0%)
updating: coinbase-ticker-stats/deps/numpy-1.26.4-cp310-cp310-manylinux_2_17_x86_64.manylinux2014_x86_64.whl (deflated 1%)
updating: coinbase-ticker-stats/deps/python_dateutil-2.9.0.post0-py2.py3-none-any.whl (deflated 1%)
updating: coinbase-ticker-stats/deps/pytz-2024.1-py2.py3-none-any.whl (deflated 25%)
updating: coinbase-ticker-stats/deps/pandas-2.2.2-cp310-cp310-manylinux_2_17_x86_64.manylinux2014_x86_64.whl (deflated 2%)
updating: coinbase-ticker-stats/deps/tzdata-2024.1-py2.py3-none-any.whl (deflated 32%)
updating: coinbase-ticker-stats/deps/six-1.16.0-py2.py3-none-any.whl (deflated 4%)
updating: coinbase-ticker-stats/src/ (stored 0%)
updating: coinbase-ticker-stats/src/stats.py (deflated 71%)
```


### 2️⃣ Deploy the Pulsar Function


Next, you deploy it using the following command:

```bash
sh ./bin/pulsar/start-ticker-stats.sh
```

You can verify that the TickerStats Function is working by running the following command to consume messages from the
Function's configured output topic. If it is working properly, you should see output similar to that shown here:

````
docker exec -it pulsar-broker sh -c   "./bin/pulsar-client consume -n 0 -p Earliest -s my-sub persistent://feeds/realtime/coinbase-ticker-stats"

...
----- got message -----
key:[null], properties:[__pfn_input_msg_id__=CAwQ9B4YAA==, __pfn_input_topic__=persistent://feeds/realtime/coinbase-ticker-partition-0], content:{
 "sequence": 1231277600,
 "product_id": "USDT-USD",
 "price": 1.0005,
 "latest_emwa": 1.000498239928398,
 "latest_std": 4.0152407931263944e-06,
 "latest_variance": 1.6122158626786277e-11,
 "rolling_mean": 1.0004979999999999,
 "rolling_std": 4.472135954755798e-06,
 "rolling_variance": 1.999999999781955e-11,
 "time": "2024-04-15T16:14:21.919683Z",
 "millis": 1713197661919
}
----- got message -----
key:[null], properties:[__pfn_input_msg_id__=CAwQ9R4YAA==, __pfn_input_topic__=persistent://feeds/realtime/coinbase-ticker-partition-0], content:{
 "sequence": 78286110141,
 "product_id": "BTC-USD",
 "price": 64619.98,
 "latest_emwa": 64615.50239556808,
 "latest_std": 2.684163338194109,
 "latest_variance": 7.204732826105343,
 "rolling_mean": 64615.042,
 "rolling_std": 3.143019249066717,
 "rolling_variance": 9.878570000003908,
 "time": "2024-04-15T16:14:21.988357Z",
 "millis": 1713197661988
}
````
