package velox.api.layer0.live.advanced;

import java.util.Arrays;

import velox.api.layer0.annotations.Layer0LiveModule;
import velox.api.layer0.live.DemoExternalRealtimeProvider;
import velox.api.layer0.live.DemoExternalRealtimeTradingProvider;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.data.Layer1ApiProviderSupportedFeatures;
import velox.api.layer1.data.OrderSendParameters;
import velox.api.layer1.data.SimpleOrderSendParameters;

/**
 * <p>
 * This provider is an example of trading-only provider (designed to be used
 * with separate data source)
 * </p>
 * <p>
 * This demo won't look very nice when used and only exists to illustrate the
 * technical part. Some things to keep in mind:
 * <ul>
 * <li>Prices where trading happens will not match displayed data (because both
 * are independently generated randomly)</li>
 * <li>Position isn't reported to Bookmap (same as in parent class), this
 * prevents you from using built-in position close command.
 * </ul>
 * <p>
 */
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
@Layer0LiveModule(fullName = "Cross-platform trading demo", shortName = "DCT")
public class CrossPlatformTradingProvider extends DemoExternalRealtimeTradingProvider {
    
    @Override
    public Layer1ApiProviderSupportedFeatures getSupportedFeatures() {
        // Declaring cross-platform trading functionality.
        // Trading-related capabilities will be extracted from here. Parent class will
        // declare basic trading support.
        return super.getSupportedFeatures().toBuilder()
                // Note that in current version only the first valid pair will be activated (e.g. if
                // A trades from B and C you can trade either from B or from C at any point in
                // time, not from both).
                .setTradingFrom(Arrays.asList(
                     // Built-in random data provider
                     "RANDOM",
                     // Another demo from this package (data-only)
                     "EXT:" + DemoExternalRealtimeProvider.class.getName()
                ))
                .build();
    }
    
    @Override
    public void sendOrder(OrderSendParameters orderSendParameters) {

        // Since we use trading simulated by DemoExternalRealtimeTradingProvider we need to initiate subscription

        // Since we did not report OCO/OSO/Brackets support, this method can
        // only receive simple orders
        SimpleOrderSendParameters simpleParameters = (SimpleOrderSendParameters) orderSendParameters;
        String alias = simpleParameters.alias;

        // Normally we should understand alias and be able to map it to corresponding
        // instrument.
        // E.g. receiving order for ESZ9.CME from Rithmic would mean it should be sent
        // to an instrument with symbol ESZ9 and exchange CME.
        // In this demo we just create instrument with provided alias to start
        // (very unrealistic) trading simulation.
        final Instrument newInstrument = new Instrument(alias, 0.25);
        instruments.putIfAbsent(alias, newInstrument);

        super.sendOrder(orderSendParameters);
    }
}
