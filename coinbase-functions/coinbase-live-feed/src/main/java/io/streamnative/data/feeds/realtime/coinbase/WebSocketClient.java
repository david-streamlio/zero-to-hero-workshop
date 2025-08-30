package io.streamnative.data.feeds.realtime.coinbase;

import javax.websocket.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.pulsar.io.core.PushSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClientEndpoint
public class WebSocketClient {

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketClient.class);

    private WebSocketSource webSocketSource;
    private String subscribeMsg;
    private Gson gson = new Gson();
    private Session currentSession;
    private AtomicBoolean isConnected = new AtomicBoolean(false);

    public WebSocketClient(WebSocketSource webSocketSource, String subscribeMsg) {
        this.webSocketSource = webSocketSource;
        this.subscribeMsg = subscribeMsg;
    }

    @OnOpen
    public void onOpen(Session session) {
        this.currentSession = session;
        isConnected.set(true);
        LOG.info("WebSocket connection opened: {}", session.getId());
        
        try {
            LOG.info("Sending subscription message: {}", subscribeMsg);
            session.getBasicRemote().sendText(subscribeMsg);
            webSocketSource.onConnectionEstablished();
        } catch (IOException ex) {
            LOG.error("Failed to send subscription message to Coinbase WebSocket endpoint", ex);
            isConnected.set(false);
        }
    }

    @OnMessage
    public void processMessage(String msg) {
        if (!isConnected.get()) {
            LOG.warn("Received message on disconnected WebSocket, ignoring: {}", msg);
            return;
        }
        
        try {
            LOG.debug("Received WebSocket message: {}", msg);
            JsonObject body = gson.fromJson(msg, JsonObject.class);

            String type = body.get("type") != null ? body.get("type").getAsString() : "";
            String product_id = body.get("product_id") != null ? body.get("product_id").getAsString() : null;
            
            body.remove("type");
            webSocketSource.consume(new CoinbaseRecord(body.toString(), type, product_id));
        } catch (Exception ex) {
            LOG.error("Error processing WebSocket message: {}", msg, ex);
        }
    }

    @OnError
    public void processError(Throwable t) {
        LOG.error("WebSocket error occurred", t);
        isConnected.set(false);
        webSocketSource.onConnectionLost();
    }
    
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        isConnected.set(false);
        currentSession = null;
        
        LOG.warn("WebSocket connection closed - Code: {}, Reason: {}, Session: {}", 
                closeReason.getCloseCode(), closeReason.getReasonPhrase(), session.getId());
        
        if (closeReason.getCloseCode() != CloseReason.CloseCodes.NORMAL_CLOSURE) {
            webSocketSource.onConnectionLost();
        }
    }
    
    public void closeConnection() {
        isConnected.set(false);
        if (currentSession != null && currentSession.isOpen()) {
            try {
                LOG.info("Closing WebSocket connection: {}", currentSession.getId());
                currentSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Source shutdown"));
            } catch (IOException ex) {
                LOG.warn("Error closing WebSocket connection", ex);
            }
        }
    }
    
    public boolean isConnected() {
        return isConnected.get() && currentSession != null && currentSession.isOpen();
    }
}
