package velox.api.layer0.live.advanced;

import velox.api.layer0.annotations.Layer0LiveModule;
import velox.api.layer0.live.DemoExternalRealtimeProvider;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.reading.UserDataUserMessage;

import java.math.BigInteger;

/**
 * This provider generates data according to same rules as parent provider, but also generates UserDataUserMessages
 * for related demo (see UserDataUserMessageDemo from the <b><a href="https://github.com/BookmapAPI/DemoStrategies">DemoStrategies</a></b>repo.).
 */
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
@Layer0LiveModule(fullName = "Demo external user message", shortName = "DU")
public class DemoExternalRealtimeUserMessageProvider extends DemoExternalRealtimeProvider {

    private static final String RANDOM_DATA_TAG = "RandomData";

    @Override
    protected void simulate() {
        // Perform data changes simulation
        synchronized (instruments) {
            instruments.forEach((key, value) -> {
                value.generateData();
                simulateRandomData(key, value);
            });
        }
    }

    private void simulateRandomData(String alias, Instrument instrument) {
        if (Math.random() > 0.9) {
            // In 10% cases we send UserDataUserMessage with tag RandomData.
            // We send a random integer number not far from BBO as a byte array.
            int medium = (instrument.getBestAsk() + instrument.getBestBid()) / 2;
            BigInteger randomResult = BigInteger.valueOf(medium + (int) (Math.random() * 20) - 10);
            byte[] data = randomResult.toByteArray();

            // In 30% cases we send global user message (alias = null), in other cases - aliased user message.
            String aliasResult = Math.random() > 0.3 ? alias : null;

            adminListeners.forEach(l -> l.onUserMessage(
                    new UserDataUserMessage(RANDOM_DATA_TAG, aliasResult, data)
            ));
        }
    }
}
