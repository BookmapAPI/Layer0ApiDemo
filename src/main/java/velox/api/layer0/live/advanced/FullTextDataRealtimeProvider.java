package velox.api.layer0.live.advanced;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import velox.api.layer0.annotations.Layer0LiveModule;
import velox.api.layer0.common.TextStreamParser;
import velox.api.layer0.data.FileEndReachedUserMessage;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.common.ListenableHelper;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.Layer1ApiProviderSupportedFeatures;
import velox.api.layer1.data.Layer1ApiProviderSupportedFeaturesBuilder;
import velox.api.layer1.data.LoginData;
import velox.api.layer1.data.LoginFailedReason;
import velox.api.layer1.data.OrderSendParameters;
import velox.api.layer1.data.OrderUpdateParameters;
import velox.api.layer1.data.SubscribeInfo;
import velox.api.layer1.data.UserPasswordDemoLoginData;
import velox.api.layer1.layers.Layer1ApiRelay;
import velox.api.layer1.providers.helper.PriceFormatHelper;

/**
 * <p>
 * This a an example of how you can interface bookmap with external executable using standard input/output.
 * </p>
 */
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
@Layer0LiveModule(fullName = "Text external realtime", shortName = "TE")
public class FullTextDataRealtimeProvider extends Layer1ApiRelay {
    
    /** Subprocess that will do all the job */
    private Process childProcess = null;
    private OutputStream outputStream = null;
    
    /** Parser for the data received from the subprocess */
    private TextStreamParser parser;
    
    /** Used to provide price formatting on Java side (for simplicity). */
    private final Map<String, Double> pipsMap = new ConcurrentHashMap<>();    
    
    public FullTextDataRealtimeProvider() {
        super(null);
    }

    @Override
    public void subscribe(SubscribeInfo subscribeInfo) {
        send("subscribe",
                subscribeInfo.symbol,
                subscribeInfo.exchange,
                subscribeInfo.type);
    }

    @Override
    public void unsubscribe(String alias) {
        send("unsubscribe", alias);
    }

    @Override
    public void login(LoginData loginData) {
        UserPasswordDemoLoginData userPasswordDemoLoginData = (UserPasswordDemoLoginData) loginData;

        // If startup process takes a while then it's better to do it in a separate thread
        // When doing in same thread UI can block until this method returns
        handleLogin(userPasswordDemoLoginData);
    }

    private void handleLogin(UserPasswordDemoLoginData userPasswordDemoLoginData) {
        
        if (childProcess == null) {
            try {
                // Starting the adapter itself.
                // Let's start batch file that will run actual provider
                // (so you can edit the file without rebuilding the jar).
                // Running executable directly might be preferred in real use case.
                childProcess = Runtime.getRuntime().exec("adapter.bat");
                outputStream = childProcess.getOutputStream();
            } catch (IOException e) {
                Log.error("Failed to start adapter", e);
                // Report failed login
                adminListeners.forEach(l -> l.onLoginFailed(LoginFailedReason.FATAL,
                        "Failed to start the adapter executable, see log for details.\n"
                        + "To run this demo you need to compile a provider from"
                        + " cpp/FullTextDataRealtimeProvider subfolder\n"
                        + "and create adapter.bat pointing to it in Config folder first\n"
                        + "\n"
                        + "Error: " + e.getMessage()));
            }
        }
        
        if (childProcess != null) {
            send("login", 
                    userPasswordDemoLoginData.user,
                    userPasswordDemoLoginData.password,
                    userPasswordDemoLoginData.isDemo ? "true" : "false");

            // Parser might be already initialized from the previous attempt
            if (parser == null) {
                parser = new TextStreamParser();
                ListenableHelper.addListeners(parser, this);
                parser.start(childProcess.getInputStream());
            }            
        }
    }
    
    @Override
    public void onUserMessage(Object data) {
        if (data instanceof FileEndReachedUserMessage) {
            // Ignore it - this means stream end was reached
        } else {
            super.onUserMessage(data);
        }
    }

    @Override
    public String getSource() {
        return "External executable";
    }
    
    @Override
    public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
        // Intercepting instrument additions and remembering minimal increment to use later
        pipsMap.put(alias, instrumentInfo.pips);
        
        super.onInstrumentAdded(alias, instrumentInfo);
    }
    
    @Override
    public String formatPrice(String alias, double price) {
        // Formatting could be moved into native code too
        // if advanced logic is needed.
        // One way to do it would be assigning request some ID and then waiting
        // for a response marked by the same ID to be printed by the executable
        return PriceFormatHelper.formatPriceDefault(pipsMap.get(alias), price);
    }
    
    /**
     * Send string to executable. We'll use primitive format just to simplify
     * parsing (generating JSON is much simpler than parsing it). It's a bit
     * inconsistent, but easier to use this way. Feel free to replace this by more
     * advanced format if it suits your code better.
     */
    private void send(String... lines) {
        try {
            StringBuilder builder = new StringBuilder();
            for (String line : lines) {
                builder.append(line);
                builder.append('\n');
            }
            byte[] data = builder.toString().getBytes(StandardCharsets.US_ASCII);
            outputStream.write(data);
            outputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        if (childProcess != null) {
            send("close");
            try {
                childProcess.waitFor();
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while waiting"
                        + " for the subprocess to terminate", e);
            }
        }
        if (parser != null) {
            parser.close();
        }
    }
    
    @Override
    public Layer1ApiProviderSupportedFeatures getSupportedFeatures() {
        return new Layer1ApiProviderSupportedFeaturesBuilder().build();
    }

    @Override
    public void sendOrder(OrderSendParameters orderSendParameters) {
        // This method will not be called because this adapter does not report
        // trading capabilities
        // It could be forwarded into the executable in a similar way, but it would be
        // important to define a protocol to not lose types in the process
        // (e.g. print types along with other data)
        throw new UnsupportedOperationException("Not trading capable");
    }

    @Override
    public void updateOrder(OrderUpdateParameters orderUpdateParameters) {
        // This method will not be called because this adapter does not report
        // trading capabilities
        // It could be forwarded into the executable in a similar way, but it would be
        // (e.g. print types along with other data)
        throw new UnsupportedOperationException("Not trading capable");
    }
}
