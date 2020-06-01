package velox.api.layer0.live;

import java.util.HashMap;

import velox.api.layer0.annotations.Layer0LiveModule;
import velox.api.layer1.Layer1ApiAdminListener;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.LoginData;
import velox.api.layer1.data.OrderSendParameters;
import velox.api.layer1.data.OrderUpdateParameters;
import velox.api.layer1.data.SubscribeInfo;
import velox.api.layer1.data.UserPasswordDemoLoginData;

import static velox.api.layer1.data.LoginFailedReason.WRONG_CREDENTIALS;

/**
 * <p>
 * This a demo provider that generates data instead of actually receiving it.
 * </p>
 */
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
@Layer0LiveModule(fullName = "Demo external realtime", shortName = "DE")
public class DemoExternalRealtimeProvider extends ExternalLiveBaseProvider {

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
    public void subscribe(SubscribeInfo subscribeInfo) {
        String symbol = subscribeInfo.symbol;
        String exchange = subscribeInfo.exchange;
        String type = subscribeInfo.type;

        String alias = createAlias(symbol, exchange, type);
        Log.info("Subscribe to symbol " + symbol);
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

                final Instrument newInstrument = new Instrument(this, alias, pips);
                instruments.put(alias, newInstrument);

                final InstrumentInfo instrumentInfo = new InstrumentInfo(
                        symbol, exchange, type, newInstrument.pips, 1, "", false);

                instrumentListeners.forEach(l -> l.onInstrumentAdded(alias, instrumentInfo));
            }
        }
    }

    @Override
    public void unsubscribe(String alias) {
        Log.info("Unsubscribe from the symbol " + alias);
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
                && "user".equals(userPasswordDemoLoginData.user) && userPasswordDemoLoginData.isDemo;

        if (isValid) {
            // Report successful login
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
            adminListeners.forEach(l -> l.onLoginFailed(WRONG_CREDENTIALS,
                    "This provider only accepts following credentials:\n"
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
