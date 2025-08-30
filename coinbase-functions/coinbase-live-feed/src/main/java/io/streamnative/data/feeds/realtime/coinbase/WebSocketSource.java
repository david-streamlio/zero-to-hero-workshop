package io.streamnative.data.feeds.realtime.coinbase;

import org.apache.pulsar.io.core.PushSource;
import org.apache.pulsar.io.core.SourceContext;

import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketSource extends PushSource<String> {

    public final static String WEBSOCKET_URI_PROPERTY = "websocketURI";
    public final static String SUBSCRIPTION_PROPERTY = "subscription";
    public final static String MAX_RECONNECT_ATTEMPTS_PROPERTY = "maxReconnectAttempts";
    public final static String RECONNECT_BASE_DELAY_PROPERTY = "reconnectBaseDelayMs";

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketSource.class);
    private static final int DEFAULT_MAX_RECONNECT_ATTEMPTS = 10;
    private static final long DEFAULT_RECONNECT_BASE_DELAY_MS = 1000;
    private static final long MAX_RECONNECT_DELAY_MS = 30000;

    private WebSocketContainer container;
    private WebSocketClient webSocketClient;
    private ScheduledExecutorService reconnectExecutor;
    private String websocketURI;
    private String subscription;
    private int maxReconnectAttempts;
    private long reconnectBaseDelayMs;
    private AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private AtomicBoolean isConnecting = new AtomicBoolean(false);
    private AtomicBoolean shouldReconnect = new AtomicBoolean(true);

    @Override
    public void open(Map<String, Object> config, SourceContext srcCtx) throws Exception {
        Map<String, Object> configs = srcCtx.getSourceConfig().getConfigs();
        
        websocketURI = configs.get(WEBSOCKET_URI_PROPERTY).toString();
        subscription = configs.get(SUBSCRIPTION_PROPERTY).toString();
        
        maxReconnectAttempts = configs.containsKey(MAX_RECONNECT_ATTEMPTS_PROPERTY) 
            ? Integer.parseInt(configs.get(MAX_RECONNECT_ATTEMPTS_PROPERTY).toString()) 
            : DEFAULT_MAX_RECONNECT_ATTEMPTS;
        
        reconnectBaseDelayMs = configs.containsKey(RECONNECT_BASE_DELAY_PROPERTY) 
            ? Long.parseLong(configs.get(RECONNECT_BASE_DELAY_PROPERTY).toString()) 
            : DEFAULT_RECONNECT_BASE_DELAY_MS;
        
        reconnectExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "WebSocket-Reconnect-Thread");
            t.setDaemon(true);
            return t;
        });
        
        container = ContainerProvider.getWebSocketContainer();
        webSocketClient = new WebSocketClient(this, subscription);
        
        connectToWebSocket();
    }

    @Override
    public void close() throws Exception {
        shouldReconnect.set(false);
        
        if (reconnectExecutor != null && !reconnectExecutor.isShutdown()) {
            reconnectExecutor.shutdown();
            try {
                if (!reconnectExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    reconnectExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                reconnectExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        if (webSocketClient != null) {
            webSocketClient.closeConnection();
        }
    }
    
    private void connectToWebSocket() {
        if (isConnecting.compareAndSet(false, true)) {
            try {
                LOG.info("Attempting to connect to WebSocket: {}", websocketURI);
                container.connectToServer(webSocketClient, URI.create(websocketURI));
                reconnectAttempts.set(0);
                LOG.info("Successfully connected to WebSocket");
            } catch (DeploymentException | IOException ex) {
                LOG.error("Failed to connect to WebSocket: {}", ex.getMessage());
                scheduleReconnect();
            } finally {
                isConnecting.set(false);
            }
        }
    }
    
    public void onConnectionLost() {
        LOG.warn("WebSocket connection lost, scheduling reconnect");
        scheduleReconnect();
    }
    
    private void scheduleReconnect() {
        if (!shouldReconnect.get()) {
            LOG.info("Reconnection disabled, not scheduling reconnect");
            return;
        }
        
        int currentAttempt = reconnectAttempts.incrementAndGet();
        
        if (currentAttempt > maxReconnectAttempts) {
            LOG.error("Maximum reconnection attempts ({}) exceeded, giving up", maxReconnectAttempts);
            return;
        }
        
        long delay = Math.min(
            reconnectBaseDelayMs * (long) Math.pow(2, currentAttempt - 1),
            MAX_RECONNECT_DELAY_MS
        );
        
        LOG.info("Scheduling reconnection attempt {} of {} in {}ms", 
                currentAttempt, maxReconnectAttempts, delay);
        
        reconnectExecutor.schedule(() -> {
            if (shouldReconnect.get()) {
                connectToWebSocket();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }
    
    public void onConnectionEstablished() {
        LOG.info("WebSocket connection established successfully");
        reconnectAttempts.set(0);
    }

}
