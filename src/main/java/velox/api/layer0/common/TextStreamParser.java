package velox.api.layer0.common;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Base64;

import javax.imageio.ImageIO;

import com.google.gson.Gson;

import velox.api.layer0.data.FileEndReachedUserMessage;
import velox.api.layer0.data.FileNotSupportedUserMessage;
import velox.api.layer0.data.IndicatorDefinitionUserMessage;
import velox.api.layer0.data.IndicatorPointUserMessage;
import velox.api.layer0.data.OrderQueuePositionUserMessage;
import velox.api.layer0.data.ReadFileLoginData;
import velox.api.layer0.data.TextDataMessage;
import velox.api.layer0.replay.ExternalReaderBaseProvider;
import velox.api.layer1.Layer1ApiDataListener;
import velox.api.layer1.Layer1ApiListener;
import velox.api.layer1.data.BalanceInfo;
import velox.api.layer1.data.DisconnectionReason;
import velox.api.layer1.data.ExecutionInfo;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.LoginData;
import velox.api.layer1.data.LoginFailedReason;
import velox.api.layer1.data.MarketMode;
import velox.api.layer1.data.OrderInfoUpdate;
import velox.api.layer1.data.StatusInfo;
import velox.api.layer1.data.SystemTextMessageType;
import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.layers.Layer1ApiUpstreamRelay;

/**
 * Reads data from a stream that essentially describes sequence of calls to
 * {@link Layer1ApiListener} and sends to all subscribers.
 */
public class TextStreamParser extends Layer1ApiUpstreamRelay {
    
    public static class Event {
        public long time;
    }

    public static class EventInstrumentAdded extends Event {
        public String alias;
        public InstrumentInfo instrumentInfo;
    }

    public static class EventInstrumentRemoved extends Event {
        public String alias;
    }

    public static class EventInstrumentNotFound extends Event {
        public String symbol;
        public String exchange;
        public String type;
    }

    public static class EventInstrumentAlreadySubscribed extends Event {
        public String symbol;
        public String exchange;
        public String type;
    }

    public static class EventTrade extends Event {
        public String alias;
        public double price;
        public int size;
        public TradeInfo tradeInfo;
    }

    public static class EventDepth extends Event {
        public String alias;
        public boolean isBid;
        public int price;
        public int size;
    }

    public static class EventMboSend extends Event {
        public String alias;
        public String orderId;
        public boolean isBid;
        public int price;
        public int size;
    }

    public static class EventMboReplace extends Event {
        public String alias;
        public String orderId;
        public int price;
        public int size;
    }

    public static class EventMboCancel extends Event {
        public String alias;
        public String orderId;
    }

    public static class EventMarketMode extends Event {
        public String alias;
        public MarketMode marketMode;
    }

    public static class EventOrderUpdated extends Event {
        public OrderInfoUpdate orderInfoUpdate;
    }

    public static class EventOrderExecuted extends Event {
        public ExecutionInfo executionInfo;
    }

    public static class EventStatus extends Event {
        public StatusInfo statusInfo;
    }

    public static class EventBalance extends Event {
        public BalanceInfo balanceInfo;
    }

    public static class EventLoginFailed extends Event {
        public LoginFailedReason reason;
        public String message;
    }

    public static class EventLoginSuccessful extends Event {
    }

    public static class EventConnectionLost extends Event {
        public DisconnectionReason reason;
        public String message;
    }

    public static class EventConnectionRestored extends Event {
    }

    public static class EventSystemTextMessage extends Event {
        public String message;
        public SystemTextMessageType messageType;
    }
    
    public static class IndicatorDefinitionUserMessageEvent extends Event {
        public int id;
        public String alias;
        public String indicatorName;
        public short mainLineStyleMask;
        public short mainLineStyleMultiplier;
        public int mainLineWidth;
        public Color lineColor;
        public short rightLineStyleMask;
        public short rightLineStyleMultiplier;
        public int rightLineWidth;
        public String base64EndodedIcon;
        public int iconOffsetX;
        public int iconOffsetY;
        public boolean showOnMainChart;
        public String valueFormat;
    }
    
    public static class IndicatorPointUserMessageEvent extends Event {
        public int id;
        public double price;
    }
    
    public static class OrderQueuePositionUserMessageEvent extends Event {
        public String orderId;
        public int position;
    }

    public static class TextDataMessageEvent extends Event {
        public String alias;
        public String source;
        public double price;
        public double size;
        public Boolean isBid;
        public String data;
    }
    
    private final Gson gson = new Gson();

    private Thread readerThread;
    private long currentTime = 0;

    private boolean play = true;

    private BufferedReader reader;
    
    public TextStreamParser() {
    }
    
    public void start(InputStream inputStream) {

        try {
            reader = new BufferedReader(new InputStreamReader(inputStream));

            // Reading one line to guarantee that when we exit this method
            // getCurrentTime will return meaningful result.
            readLine();

            readerThread = new Thread(this::read);
            readerThread.start();
        } catch (@SuppressWarnings("unused") IOException e) {
            adminListeners.forEach(listener -> listener.onUserMessage(new FileNotSupportedUserMessage()));
        }
    }

    private void read() {
        try {
            while (!Thread.interrupted() && play) {
                readLine();
            }
        } catch (@SuppressWarnings("unused") IOException e) {
            reportFileEnd();
        }
    }

    public void reportFileEnd() {
        adminListeners.forEach(listener -> listener.onUserMessage(new FileEndReachedUserMessage()));
        play = false;
    }

    private void readLine() throws IOException {
        String line = reader.readLine();
        if (line == null && play) {
            reportFileEnd();
        } else {
            String[] tokens = line.split(" ", 2);
            String eventCode = tokens[0];
            String eventData = tokens[1];
            switch (eventCode) {
            case "InstrumentAdded": {
                EventInstrumentAdded event = gson.fromJson(eventData, EventInstrumentAdded.class);
                currentTime = event.time;
                onInstrumentAdded(event.alias, event.instrumentInfo);
                break;
            }
            case "InstrumentRemoved": {
                EventInstrumentRemoved event = gson.fromJson(eventData, EventInstrumentRemoved.class);
                currentTime = event.time;
                onInstrumentRemoved(event.alias);
                break;
            }
            case "InstrumentNotFound": {
                EventInstrumentNotFound event = gson.fromJson(eventData, EventInstrumentNotFound.class);
                currentTime = event.time;
                onInstrumentNotFound(event.symbol, event.exchange, event.type);
                break;
            }
            case "InstrumentAlreadySubscribed": {
                EventInstrumentAlreadySubscribed event = gson.fromJson(eventData, EventInstrumentAlreadySubscribed.class);
                currentTime = event.time;
                onInstrumentAlreadySubscribed(event.symbol, event.exchange, event.type);
                break;
            }
            case "Trade": {
                EventTrade event = gson.fromJson(eventData, EventTrade.class);
                currentTime = event.time;
                onTrade(event.alias, event.price, event.size, event.tradeInfo);
                break;
            }
            case "Depth": {
                EventDepth event = gson.fromJson(eventData, EventDepth.class);
                currentTime = event.time;
                onDepth(event.alias, event.isBid, event.price, event.size);
                break;
            }
            case "MboSend": {
                EventMboSend event = gson.fromJson(eventData, EventMboSend.class);
                currentTime = event.time;
                onMboSend(event.alias, event.orderId, event.isBid, event.price, event.size);
                break;
            }
            case "MboReplace": {
                EventMboReplace event = gson.fromJson(eventData, EventMboReplace.class);
                currentTime = event.time;
                onMboReplace(event.alias, event.orderId, event.price, event.size);
                break;
            }
            case "MboCancel": {
                EventMboCancel event = gson.fromJson(eventData, EventMboCancel.class);
                currentTime = event.time;
                onMboCancel(event.alias, event.orderId);
                break;
            }
            case "MarketMode": {
                EventMarketMode event = gson.fromJson(eventData, EventMarketMode.class);
                currentTime = event.time;
                onMarketMode(event.alias, event.marketMode);
                break;
            }
            case "OrderUpdated": {
                EventOrderUpdated event = gson.fromJson(eventData, EventOrderUpdated.class);
                currentTime = event.time;
                onOrderUpdated(event.orderInfoUpdate);
                break;
            }
            case "OrderExecuted": {
                EventOrderExecuted event = gson.fromJson(eventData, EventOrderExecuted.class);
                currentTime = event.time;
                onOrderExecuted(event.executionInfo);
                break;
            }
            case "Status": {
                EventStatus event = gson.fromJson(eventData, EventStatus.class);
                currentTime = event.time;
                onStatus(event.statusInfo);
                break;
            }
            case "Balance": {
                EventBalance event = gson.fromJson(eventData, EventBalance.class);
                currentTime = event.time;
                onBalance(event.balanceInfo);
                break;
            }
            case "LoginFailed": {
                EventLoginFailed event = gson.fromJson(eventData, EventLoginFailed.class);
                currentTime = event.time;
                onLoginFailed(event.reason, event.message);
                break;
            }
            case "LoginSuccessful": {
                EventLoginSuccessful event = gson.fromJson(eventData, EventLoginSuccessful.class);
                currentTime = event.time;
                onLoginSuccessful();
                break;
            }
            case "ConnectionLost": {
                EventConnectionLost event = gson.fromJson(eventData, EventConnectionLost.class);
                currentTime = event.time;
                onConnectionLost(event.reason, event.message);
                break;
            }
            case "ConnectionRestored": {
                EventConnectionRestored event = gson.fromJson(eventData, EventConnectionRestored.class);
                currentTime = event.time;
                onConnectionRestored();
                break;
            }
            case "SystemTextMessage": {
                EventSystemTextMessage event = gson.fromJson(eventData, EventSystemTextMessage.class);
                currentTime = event.time;
                onSystemTextMessage(event.message, event.messageType);
                break;
            }
            case "IndicatorDefinitionUserMessage": {
                IndicatorDefinitionUserMessageEvent event = gson.fromJson(eventData,
                        IndicatorDefinitionUserMessageEvent.class);
                currentTime = event.time;
                
                BufferedImage icon = null;
                if (event.base64EndodedIcon != null) {
                    byte[] iconBytes = Base64.getDecoder().decode(event.base64EndodedIcon);
                    icon = ImageIO.read(new ByteArrayInputStream(iconBytes));
                }
                
                onUserMessage(new IndicatorDefinitionUserMessage(event.id, event.alias, event.indicatorName,
                        event.mainLineStyleMask, event.mainLineStyleMultiplier, event.mainLineWidth, event.lineColor,
                        event.rightLineStyleMask, event.rightLineStyleMultiplier, event.rightLineWidth,
                        icon, event.iconOffsetX, event.iconOffsetY, event.showOnMainChart, event.valueFormat));
                break;
            }
            case "IndicatorPointUserMessage": {
                IndicatorPointUserMessageEvent event = gson.fromJson(eventData, IndicatorPointUserMessageEvent.class);
                currentTime = event.time;
                onUserMessage(new IndicatorPointUserMessage(event.id, event.price));
                break;
            }
            case "OrderQueuePositionUserMessage": {
                OrderQueuePositionUserMessageEvent event = gson.fromJson(eventData,
                        OrderQueuePositionUserMessageEvent.class);
                currentTime = event.time;
                onUserMessage(new OrderQueuePositionUserMessage(event.orderId, event.position));
                break;
            }
            case "TextDataMessage": {
                TextDataMessageEvent event = gson.fromJson(eventData, TextDataMessageEvent.class);
                currentTime = event.time;
                onUserMessage(new TextDataMessage(event.alias, event.source, event.isBid, event.price, event.size, event.data));
                break;
            }
            default:
                reportFileEnd();
                throw new RuntimeException("Unknown event code " + eventCode);
            }
        }
    }

    public long getCurrentTime() {
        return currentTime;
    }

    @Override
    public void close() {
        readerThread.interrupt();
        try {
            reader.close();
        } catch (@SuppressWarnings("unused") IOException e) {
        }
    }
}
