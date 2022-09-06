package velox.api.layer0.live.advanced;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import velox.api.layer0.annotations.Layer0LiveModule;
import velox.api.layer0.live.DemoExternalRealtimeTradingProvider;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.data.InstrumentCoreInfo;
import velox.api.layer1.data.Layer1ApiProviderSupportedFeatures;
import velox.api.layer1.data.OrderSendParameters;
import velox.api.layer1.data.SimpleOrderSendParameters;
import velox.api.layer1.utils.SymbolMappingInfo;

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
                // Add cross trading from Random and Rithmic
                .setSymbolsMappingFunction(alternatives -> {
                    
                    Optional<InstrumentCoreInfo> crossTradingInstrument = alternatives.stream()
                            .filter(a -> a.type.endsWith("EXT:" + CrossPlatformTradingProvider.class.getName()))
                            .findAny();
                    
                    if (crossTradingInstrument.isPresent()) {
                        // This is our own instrument that we most likely just defined.
                        // Need to at least provide pips/multiplier for it
                        return new SymbolMappingInfo(Collections.emptySet(), Collections.emptySet(), 1, price -> 0.25);
                    } else {
                        Optional<InstrumentCoreInfo> sourceInstrument = alternatives.stream()
                            .filter(a -> a.type.endsWith("@RANDOM") || a.type.endsWith("@RITHMIC"))
                            .findAny();
                        Optional<SymbolMappingInfo> mappingInfo = sourceInstrument.map(instrument -> {
                                String crossTradingTargetType = instrument.type
                                        .replaceAll("RANDOM|RITHMIC", "EXT:" + CrossPlatformTradingProvider.class.getName());
                                InstrumentCoreInfo crossTradingTarget = new InstrumentCoreInfo(
                                        instrument.symbol, 
                                        instrument.exchange, 
                                        crossTradingTargetType);
                                // Note: in this case you can pass this instrument as either alternative or 
                                // a cross trading target (so as a either first or second parameter). This 
                                // will produce similar immediate effect, but will change the potential 
                                // effects later: alternative means that it's the same instrument, just in
                                // another connection syntax. Cross trading target (crossTradingTo field)
                                // means that it's a different symbol that we can cross trade to (like ES->MES)
                                return new SymbolMappingInfo(
                                    Collections.emptySet(),
                                    Set.of(crossTradingTarget),
                                    1, price -> 0.25);
                            });
                        return mappingInfo.orElse(null);
                    }
                })
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
