package io.streamnative.flink.utils;

import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.client.program.rest.RestClusterClient;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.MemorySize;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Scanner;

import static org.apache.flink.configuration.TaskManagerOptions.*;
import static org.apache.flink.configuration.TaskManagerOptions.MANAGED_MEMORY_SIZE;

public class FlinkUtils {

    public static Configuration getFlinkConfiguration() {
        Configuration flinkConfig = new Configuration();

        flinkConfig.set(CPU_CORES, 1.0);
        flinkConfig.set(TASK_HEAP_MEMORY, MemorySize.ofMebiBytes(1024));
        flinkConfig.set(TASK_OFF_HEAP_MEMORY, MemorySize.ofMebiBytes(256));
        flinkConfig.setString("jobmanager.rpc.address", "localhost");
        flinkConfig.setInteger("jobmanager.rpc.port", 8081);
        return flinkConfig;
    }

    public static StreamExecutionEnvironment getEnvironment(ParameterTool parameterTool) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment
                .getExecutionEnvironment(getFlinkConfiguration());

        env.getConfig().setRestartStrategy(RestartStrategies
                .fixedDelayRestart(4, 15000));

        env.getConfig().setGlobalJobParameters(parameterTool);

        env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);
        // env.enableCheckpointing(20000);
        return env;
    }

    public static StreamTableEnvironment getTableEnvironment(ParameterTool parameterTool) {

        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .inStreamingMode()
                .withConfiguration(getFlinkConfiguration())
                .build();

        StreamTableEnvironment tableEnv = StreamTableEnvironment
                .create(getEnvironment(parameterTool), settings);

        tableEnv.executeSql(getDDL("/00-create-ticker-table.sql"));
        tableEnv.executeSql(getDDL("/01-create-ticker-stats-table.sql"));

        // Print registered tables
        System.out.println("Registered Tables: ");
        System.out.println("===========================");
        Arrays.stream(tableEnv.listTables()).sequential().forEach(s -> {System.out.println(s);});
        System.out.println("===========================\n\n");

        return tableEnv;
    }

    private static String getDDL(String ddlResource) {
        InputStream inputStream = FlinkUtils.class.getResourceAsStream(ddlResource);
        return new Scanner(inputStream, "UTF-8").useDelimiter("\\A").next();
    }
}
