package velox.api.layer0.replay.advanced;


import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import velox.api.layer0.annotations.Layer0ReplayModule;
import velox.api.layer0.data.FileEndReachedUserMessage;
import velox.api.layer0.data.FileNotSupportedUserMessage;
import velox.api.layer0.data.IndicatorDefinitionUserMessage;
import velox.api.layer0.data.IndicatorPointUserMessage;
import velox.api.layer0.data.ReadFileLoginData;
import velox.api.layer0.data.TextDataMessage;
import velox.api.layer0.replay.ExternalReaderBaseProvider;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.LoginData;
import velox.api.layer1.data.TradeInfo;

/**
 * <p>
 * Reads demo file that you can download and generates some indicators while
 * reading it.
 * <p>
 * <p>
 * Typically you would have indicators already generated in same or some other
 * file (e.g. as a result of simulation)
 * <p>
 */
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
@Layer0ReplayModule
public class DemoAdvancedReplayProvider extends ExternalReaderBaseProvider implements HandlerListener {

    private Thread readerThread;
    private volatile long currentTime = 0;
    private CountDownLatch timeReadLatch = new CountDownLatch(1);

    private HandlerBookmapIndicators handler;

    @Override
    public void login(LoginData loginData) {
        ReadFileLoginData fileData = (ReadFileLoginData) loginData;

        try {
            // You can download sample file at https://bookmap.com/shared/feeds/BookmapRecorderDemo_ES-CL_20181002.zip
            // Extract before loading.
            // Only loading that demo file.
            if (!fileData.file.getName().equals("BookmapRecorderDemo_ES-CL_20181002.txt")) {
                throw new IOException("File extension not supported");
            } else {

                handler = new HandlerBookmapIndicators(this, fileData.file.getAbsolutePath());

                readerThread = new Thread(this::read);
                readerThread.start();
                
                timeReadLatch.await();
            }
        } catch (@SuppressWarnings("unused") Exception e) {
            adminListeners.forEach(listener -> listener.onUserMessage(new FileNotSupportedUserMessage()));
        }
    }

    private void read() {
        try {
            handler.run();
        } catch (@SuppressWarnings("unused") Exception e) {
            // We could also report unsupported file if no callback were invoked yet
            // In this case bookmap would try other reader modules
            reportFileEnd();
        }
    }

    public void reportFileEnd() {
        adminListeners.forEach(listener -> listener.onUserMessage(new FileEndReachedUserMessage()));
        
        // In case no time was read - there will be no new time updates after this anyway
        timeReadLatch.countDown();
    }

    @Override
    public long getCurrentTime() {
        return currentTime;
    }

    @Override
    public String getSource() {
        // String identifying where data came from.
        // For example you can use that later in your indicator.
        return "advanced example data";
    }

    @Override
    public void close() {
        handler.stop();
        try {
            readerThread.join();
        } catch (InterruptedException e) {
            Log.error("Interrupted while waiting for reader to stop", e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void onFileEnd() {
        reportFileEnd();
    }

    private void setCurrentTime(long t) {
        currentTime = t;
        timeReadLatch.countDown();
    }
    
    @Override
    public void onDepth(long t, String alias, boolean isBuy, int price, int size) {
        setCurrentTime(t);
        dataListeners.forEach(l -> l.onDepth(alias, isBuy, price, size));
    }

    @Override
    public void onTrade(long t, String alias, double price, int size, boolean isBidAggressor) {
        setCurrentTime(t);
        dataListeners.forEach(l -> l.onTrade(alias, price, size, new TradeInfo(false, isBidAggressor)));
    }

    @Override
    public void onInstrument(long t, InstrumentInfo instrumentInfo) {
        setCurrentTime(t);
        instrumentListeners.forEach(l -> l.onInstrumentAdded(instrumentInfo.symbol, instrumentInfo));
    }

    @Override
    public void onTextData(long t, TextDataMessage textDataMessage) {
        setCurrentTime(t);
        adminListeners.forEach(l -> l.onUserMessage(textDataMessage));
    }

    @Override
    public void onIndicatorDefinition(long t, IndicatorDefinitionUserMessage indicatorDefinitionUserMessage) {
        setCurrentTime(t);
        adminListeners.forEach(l -> l.onUserMessage(indicatorDefinitionUserMessage));
    }

    @Override
    public void onIndicatorPoint(long t, IndicatorPointUserMessage indicatorPointUserMessage) {
        setCurrentTime(t);
        adminListeners.forEach(l -> l.onUserMessage(indicatorPointUserMessage));
    }

}
