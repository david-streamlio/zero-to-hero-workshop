FROM localhost:32000/flink:1.18.1-stream5-scala_2.12-java17

# Copy your Flink job JAR
COPY --chown=flink:flink maven/ /opt/flink/usrlib/flink-job.jar

# (Optional) Copy custom Flink configuration
# COPY flink-conf.yaml /opt/flink/conf/flink-conf.yaml

# Set the entrypoint to run your Flink job
# ENTRYPOINT ["flink", "run", "-c", "io.streamnative.coinbase.flink.stats.FlinkCoinbaseMovingAverage", "/opt/flink/usrlib/flink-job.jar"]
