package io.streamnative.data.feeds.realtime.coinbase;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Simple unit tests that validate the core reconnection functionality
 * without complex mocking dependencies.
 */
public class SimpleReconnectionTest {

    @Test
    public void testCoinbaseRecordCreation() {
        String jsonMessage = "{\"price\":\"50000.00\",\"volume\":\"1.5\"}";
        String messageType = "ticker";
        String productId = "BTC-USD";
        
        CoinbaseRecord record = new CoinbaseRecord(jsonMessage, messageType, productId);
        
        assertEquals("Should have correct value", jsonMessage, record.getValue());
        assertTrue("Should have correct key", record.getKey().isPresent());
        assertEquals("Should have correct message type", messageType, record.getKey().get());
        assertEquals("Should have correct product ID", productId, record.getProperties().get("product"));
    }

    @Test
    public void testCoinbaseRecordWithNullProduct() {
        String jsonMessage = "{\"price\":\"50000.00\"}";
        String messageType = "ticker";
        
        CoinbaseRecord record = new CoinbaseRecord(jsonMessage, messageType, null);
        
        assertEquals("Should have correct value", jsonMessage, record.getValue());
        assertEquals("Should have correct key", messageType, record.getKey().get());
        assertFalse("Should not have product property", record.getProperties().containsKey("product"));
    }

    @Test
    public void testCoinbaseRecordWithEmptyProduct() {
        String jsonMessage = "{\"price\":\"50000.00\"}";
        String messageType = "ticker";
        String productId = "";
        
        CoinbaseRecord record = new CoinbaseRecord(jsonMessage, messageType, productId);
        
        assertEquals("Should have correct value", jsonMessage, record.getValue());
        assertEquals("Should have correct key", messageType, record.getKey().get());
        assertFalse("Should not have product property for empty string", 
            record.getProperties().containsKey("product"));
    }

    @Test
    public void testWebSocketClientBasicFunctionality() {
        // Create a test source that tracks method calls
        TestWebSocketSource testSource = new TestWebSocketSource();
        String subscribeMessage = "{\"type\":\"subscribe\",\"channels\":[\"ticker\"]}";
        
        WebSocketClient client = new WebSocketClient(testSource, subscribeMessage);
        
        // Test initial state
        assertFalse("Client should start disconnected", client.isConnected());
        
        // Test error handling
        RuntimeException testError = new RuntimeException("Test error");
        client.processError(testError);
        assertEquals("Should call onConnectionLost on error", 1, testSource.getConnectionLostCount());
        
        // Test message processing when disconnected
        String testMessage = "{\"type\":\"ticker\",\"product_id\":\"BTC-USD\",\"price\":\"50000\"}";
        client.processMessage(testMessage);
        assertEquals("Should not process messages when disconnected", 
            0, testSource.getConsumedMessages().size());
    }

    @Test
    public void testReconnectionConfiguration() {
        // Test that configuration properties are correctly named
        assertEquals("WebSocket URI property should match", 
            "websocketURI", WebSocketSource.WEBSOCKET_URI_PROPERTY);
        assertEquals("Subscription property should match", 
            "subscription", WebSocketSource.SUBSCRIPTION_PROPERTY);
        assertEquals("Max reconnect attempts property should match", 
            "maxReconnectAttempts", WebSocketSource.MAX_RECONNECT_ATTEMPTS_PROPERTY);
        assertEquals("Reconnect base delay property should match", 
            "reconnectBaseDelayMs", WebSocketSource.RECONNECT_BASE_DELAY_PROPERTY);
    }

    /**
     * Test implementation of WebSocketSource that tracks method calls
     */
    private static class TestWebSocketSource extends WebSocketSource {
        private int connectionEstablishedCount = 0;
        private int connectionLostCount = 0;
        private java.util.List<CoinbaseRecord> consumedMessages = new java.util.ArrayList<>();

        @Override
        public void onConnectionEstablished() {
            connectionEstablishedCount++;
        }

        @Override
        public void onConnectionLost() {
            connectionLostCount++;
        }

        @Override
        public void consume(org.apache.pulsar.functions.api.Record<String> record) {
            if (record instanceof CoinbaseRecord) {
                consumedMessages.add((CoinbaseRecord) record);
            }
        }

        public int getConnectionEstablishedCount() {
            return connectionEstablishedCount;
        }

        public int getConnectionLostCount() {
            return connectionLostCount;
        }

        public java.util.List<CoinbaseRecord> getConsumedMessages() {
            return consumedMessages;
        }
    }
}