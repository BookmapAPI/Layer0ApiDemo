package velox.api.layer0.live;

import java.util.HashMap;

import velox.api.layer1.Layer1ApiAdminListener;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.LoginData;
import velox.api.layer1.data.LoginFailedReason;
import velox.api.layer1.data.OrderSendParameters;
import velox.api.layer1.data.OrderUpdateParameters;
import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.data.UserPasswordDemoLoginData;

/**
 * <p>
 * This a demo provider that generates data instead of actually receiving it.
 * </p>
 */
public class DemoExternalRealtimeProvider extends ExternalLiveBaseProvider {

    protected class Instrument {
        /** Number of depth levels that will be generated on each side */
        private static final int DEPTH_LEVELS_COUNT = 10;

        protected final String alias;
        protected final double pips;

        private int basePrice;

        public Instrument(String alias, double pips) {
            this.alias = alias;
            this.pips = pips;

            // Pick random price that will be used to generate the data
            // This is an integer representation of a price (before multiplying
            // by pips)
            this.basePrice = (int) (Math.random() * 10000 + 1000);
        }

        public void generateData() {

            // Determining best bid/ask
            int bestBid = getBestBid();
            int bestAsk = getBestAsk();

            // Populating 10 levels to each side of best bid/best ask with
            // random data
            for (int i = 0; i < DEPTH_LEVELS_COUNT; ++i) {
                final int levelsOffset = i;
                dataListeners.forEach(l -> l.onDepth(alias, true, bestBid - levelsOffset, getRandomSize()));
                dataListeners.forEach(l -> l.onDepth(alias, false, bestAsk + levelsOffset, getRandomSize()));
            }

            // Currently Bookmap does not visualize OTC trades, so you will
            // mostly want isOtc=false
            final boolean isOtc = false;
            // Trade on best bid, ask agressor
            dataListeners.forEach(l -> l.onTrade(alias, bestBid, 1, new TradeInfo(isOtc, false)));
            // Trade on best ask, bid agressor
            dataListeners.forEach(l -> l.onTrade(alias, bestAsk, 1, new TradeInfo(isOtc, true)));

            // With 10% chance change BBO
            if (Math.random() < 0.1) {
                // 50% chance to move up, 50% to move down
                if (Math.random() > 0.5) {
                    // Moving up - erasing best ask, erasing last reported bid
                    // level (emulating exchange only reporting few levels)
                    ++basePrice;
                    dataListeners.forEach(l -> l.onDepth(alias, false, bestAsk, 0));
                    dataListeners.forEach(l -> l.onDepth(alias, true, bestBid - (DEPTH_LEVELS_COUNT - 1), 0));
                    // Could also populate new best bid and add last best ask,
                    // but this can be omitted - those will be populated during
                    // next simulation step
                } else {
                    // Moving down - erasing best bid, erasing last reported ask
                    // level (emulating exchange only reporting few levels)
                    --basePrice;
                    dataListeners.forEach(l -> l.onDepth(alias, true, bestBid, 0));
                    dataListeners.forEach(l -> l.onDepth(alias, false, bestAsk + (DEPTH_LEVELS_COUNT - 1), 0));
                    // Could also populate new best ask and add last best bid,
                    // but this can be omitted - those will be populated during
                    // next simulation step
                }
            }
        }

        public int getBestAsk() {
            return basePrice;
        }

        public int getBestBid() {
            return getBestAsk() - 1;
        }

        private int getRandomSize() {
            return (int) (1 + Math.random() * 10);
        }

    }

    protected HashMap<String, Instrument> instruments = new HashMap<>();

    // This thread will perform data generation.
    private Thread connectionThread = null;

    /**
     * <p>
     * Generates alias from symbol, exchange and type of the instrument. Alias
     * is a unique identifier for the instrument, but it's also used in many
     * places in UI, so it should also be easily readable.
     * </p>
     * <p>
     * Note, that you don't have to use all 3 fields. You can just ignore some
     * of those, for example use symbol only.
     * </p>
     */
    private static String createAlias(String symbol, String exchange, String type) {
        return symbol + "/" + exchange + "/" + type;
    }

    @Override
    public void subscribe(String symbol, String exchange, String type) {
        String alias = createAlias(symbol, exchange, type);
        // Since instruments also will be accessed from the data generation
        // thread, synchronization is required
        //
        // No need to worry about calling listener from synchronized block,
        // since those will be processed asynchronously
        synchronized (instruments) {
            if (instruments.containsKey(alias)) {
                instrumentListeners.forEach(l -> l.onInstrumentAlreadySubscribed(symbol, exchange, type));
            } else {
                // We are performing subscription synchronously for simplicity,
                // but if subscription process takes long it's better to do it
                // asynchronously (e.g use Executor)

                // Randomly determining pips. In reality it will be received
                // from external source
                double pips = Math.random() > 0.5 ? 0.5 : 0.25;

                final Instrument newInstrument = new Instrument(alias, pips);
                instruments.put(alias, newInstrument);

                final InstrumentInfo instrumentInfo = new InstrumentInfo(
                        symbol, exchange, type, newInstrument.pips, 1, "", false);

                instrumentListeners.forEach(l -> l.onInstrumentAdded(alias, instrumentInfo));
            }
        }
    }

    @Override
    public void unsubscribe(String alias) {
        synchronized (instruments) {
            if (instruments.remove(alias) != null) {
                instrumentListeners.forEach(l -> l.onInstrumentRemoved(alias));
            }
        }
    }

    @Override
    public String formatPrice(String alias, double price) {
        // Use default Bookmap price formatting logic for simplicity.
        // Values returned by this method will be used on price axis and in few
        // other places.

        double pips;
        synchronized (instruments) {
            pips = instruments.get(alias).pips;
        }

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
        UserPasswordDemoLoginData userPasswordDemoLoginData = (UserPasswordDemoLoginData) loginData;

        // If connection process takes a while then it's better to do it in
        // separate thread
        connectionThread = new Thread(() -> handleLogin(userPasswordDemoLoginData));
        connectionThread.start();
    }

    private void handleLogin(UserPasswordDemoLoginData userPasswordDemoLoginData) {
        // With real connection provider would attempt establishing connection
        // here.
        boolean isValid = "pass".equals(userPasswordDemoLoginData.password)
                && "user".equals(userPasswordDemoLoginData.user) && userPasswordDemoLoginData.isDemo == true;

        if (isValid) {
            // Report succesful login
            adminListeners.forEach(Layer1ApiAdminListener::onLoginSuccessful);

            // Generate some events each second
            while (!Thread.interrupted()) {
                
                // Generate some data changes
                simulate();

                // Waiting a bit before generating more data
                try {
                    Thread.sleep(1000);
                } catch (@SuppressWarnings("unused") InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } else {
            // Report failed login
            adminListeners.forEach(l -> l.onLoginFailed(LoginFailedReason.WRONG_CREDENTIALS,
                    "This provider only acepts following credentials:\n"
                            + "username: user\n"
                            + "password: pass\n"
                            + "is demo: checked"));
        }
    }

    protected void simulate() {
        // Generating some data for each of the instruments
        synchronized (instruments) {
            instruments.values().forEach(Instrument::generateData);
        }
    }

    @Override
    public String getSource() {
        // String identifying where data came from.
        // For example you can use that later in your indicator.
        return "realtime demo";
    }

    @Override
    public void close() {
        // Stop events generation
        connectionThread.interrupt();
    }

}
