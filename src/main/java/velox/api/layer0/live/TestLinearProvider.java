package velox.api.layer0.live;

import velox.api.layer0.annotations.Layer0LiveModule;
import velox.api.layer1.Layer1ApiAdminListener;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.*;

import java.util.HashMap;

/**
 * <p>
 * This a test provider that generates data instead of actually receiving it.
 * You can specify a symbol like `LIN.2.5` and this will create a test symbol with
 * 5 levels where first level starts from price 2 and size 5. Each next level will be with price and size incremented,
 * </p>
 */
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
@Layer0LiveModule(fullName = "Test provider", shortName = "TEST")
public class TestLinearProvider extends ExternalLiveBaseProvider {
    private static final int DEPTH_LEVELS_COUNT = 5;
    private final HashMap<String, InstrumentInfo> instruments = new HashMap<>();

    // This thread will perform data generation.
    private Thread connectionThread = null;

    @Override
    public void subscribe(SubscribeInfo subscribeInfo) {
        String symbol = subscribeInfo.symbol;
        String exchange = subscribeInfo.exchange;
        String type = subscribeInfo.type;
        String alias = symbol;
        Log.info("Subscribe to symbol " + symbol);
        if (instruments.containsKey(alias)) {
            instrumentListeners.forEach(l -> l.onInstrumentAlreadySubscribed(symbol, exchange, type));
        } else {
            double pips = 1;
            final InstrumentInfo newInstrument = new InstrumentInfo(symbol, exchange, type, pips, 1, "", false);
            instruments.put(alias, newInstrument);
            instrumentListeners.forEach(l -> l.onInstrumentAdded(alias, newInstrument));
        }
    }

    @Override
    public void unsubscribe(String alias) {
        Log.info("Unsubscribe from the symbol " + alias);
        if (instruments.remove(alias) != null) {
            instrumentListeners.forEach(l -> l.onInstrumentRemoved(alias));
        }
    }

    @Override
    public String formatPrice(String alias, double price) {
        double pips = instruments.get(alias).pips;
        return formatPriceDefault(pips, price);
    }

    @Override
    public void sendOrder(OrderSendParameters orderSendParameters) {
        // This method will not be called because this adapter does not report
        // trading capabilities
        throw new RuntimeException("Not trading capable");
    }

    @Override
    public void updateOrder(OrderUpdateParameters orderUpdateParameters) {
        // This method will not be called because this adapter does not report
        // trading capabilities
        throw new RuntimeException("Not trading capable");
    }

    @Override
    public void login(LoginData loginData) {
        adminListeners.forEach(Layer1ApiAdminListener::onLoginSuccessful);
        connectionThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                // Generate some data changes
                simulate();
                // Waiting a bit before generating more data
                try {
                    Thread.sleep(5000);
                } catch (@SuppressWarnings("unused") InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        connectionThread.start();
    }

    private void simulate() {
        instruments.values().forEach(this::generateData);
    }


    @Override
    public String getSource() {
        // String identifying where data came from.
        // For example you can use that later in your indicator.
        return "test provider";
    }

    @Override
    public void close() {
        // Stop events generation
        connectionThread.interrupt();
    }

    private void generateData(InstrumentInfo instrumentInfo) {
        String alias = instrumentInfo.symbol;
        String[] splitted = alias.split("\\.");
        int priceBase = splitted.length >= 2 ? Integer.parseInt(splitted[1]) : 0;
        int sizeBase = splitted.length >= 3 ? Integer.parseInt(splitted[2]) : 2;
        // Determining best bid/ask
        int bestBidPrice = DEPTH_LEVELS_COUNT + priceBase;
        int bestAskPrice = DEPTH_LEVELS_COUNT + priceBase + 1;

        // Populating levels to each side of best bid/best ask with test data
        for (int i = 0; i < DEPTH_LEVELS_COUNT; i++) {
            final int levelsOffset = i;
            dataListeners.forEach(l -> l.onDepth(alias, true, bestBidPrice - levelsOffset, sizeBase + levelsOffset));
            dataListeners.forEach(l -> l.onDepth(alias, false, bestAskPrice + levelsOffset, sizeBase + levelsOffset));
        }
    }
}
