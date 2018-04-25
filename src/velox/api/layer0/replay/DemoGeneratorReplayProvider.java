package velox.api.layer0.replay;


import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import velox.api.layer0.annotations.Layer0ReplayModule;
import velox.api.layer0.data.FileEndReachedUserMessage;
import velox.api.layer0.data.IndicatorDefinitionUserMessage;
import velox.api.layer0.data.IndicatorPointUserMessage;
import velox.api.layer0.data.OrderQueuePositionUserMessage;
import velox.api.layer0.live.DemoExternalRealtimeTradingProvider;
import velox.api.layer1.data.ExecutionInfo;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.LoginData;
import velox.api.layer1.data.OrderDuration;
import velox.api.layer1.data.OrderInfoBuilder;
import velox.api.layer1.data.OrderStatus;
import velox.api.layer1.data.OrderType;
import velox.api.layer1.data.TradeInfo;

/**
 * <b>In order for this example to work you should copy icon_accept.gif to
 * working directory, which is by default C:\Bookmap\Config </b>
 * <p>
 * Instead of actually reading the file generates data, so you can select any
 * file with this one loaded.
 * </p>
 * <p>
 * Illustrates how to manipulate order queue position and display legacy API
 * indicators.
 * </p>
 * <p>
 * This should simplify transition for those who used "Recorder API". API used
 * in this example will be removed in the future in favor of L2 API based
 * solution.
 * </p>
 * <p>
 * It's newer version of BookmapRecorderDemo. Main differences are:
 * <ul>
 * <li>Depth update and trade update prices are divided by pips now</li>
 * <li>Indicator updates and order updates are now sent differently.</li>
 * </ul>
 * </p>
 * <p>
 * For more details on working with orders see
 * {@link DemoExternalRealtimeTradingProvider}
 * </p>
 */
@Layer0ReplayModule
public class DemoGeneratorReplayProvider extends ExternalReaderBaseProvider {
    /**
     * Some point in time, just for convenience (nanoseconds)
     */
    private static final long INITIAL_TIME = 1400000000_000000000L;

    /**
     * Number of nanoseconds in one second
     */
    private static final long NS_IN_SEC = 1_000_000_000L;
    
    private Thread readerThread;
    private long currentTime = 0;

    @Override
    public void login(LoginData loginData) {
        // We are not going to really read the file - just launch the reader thread.
        readerThread = new Thread(this::read);
        readerThread.start();
        
        currentTime = INITIAL_TIME;
    }

    /**
     * 
     */
    private void read() {
        // instrument1 defined 1 second after feed is started
        currentTime += 1 * NS_IN_SEC;
        InstrumentInfo instrument1 = new InstrumentInfo("Test instrument", null, null, 25, 1, "Test instrument - full name", false);
        instrumentListeners.forEach(l -> l.onInstrumentAdded("Test instrument", instrument1));
        
        // And the second instrument is defined at the same point in time
        InstrumentInfo instrument2 = new InstrumentInfo("Test instrument 2", null, null, 10, 1, "Test instrument 2 - full name", false);
        instrumentListeners.forEach(l -> l.onInstrumentAdded("Test instrument 2", instrument2));

        currentTime += NS_IN_SEC;

        // Let's generate 10 bid + 10 ask levels for 1'st instrument, and 5+5 for second.
        for (int i = 1; i <= 10; ++i) {
            // Defining final version of i to allow using it inside lambda
            final int q = i;
            final int sizeBid = i * 22;
            dataListeners.forEach(l -> l.onDepth("Test instrument", true, 40 - q, sizeBid));

            final int sizeAsk = i * 15;
            dataListeners.forEach(l -> l.onDepth("Test instrument", false, 40 + q, sizeAsk));
        }
        for (int i = 1; i <= 5; ++i) {
            // Defining final version of i to allow using it inside lambda
            final int q = i;
            final int sizeBid = i * 2;
            dataListeners.forEach(l -> l.onDepth("Test instrument 2", true, 500 - q, sizeBid));

            final int sizeAsk = i * 1;
            dataListeners.forEach(l -> l.onDepth("Test instrument 2", false, 500 + q, sizeAsk));
        }

        // Advance time 1 sec forward.
        currentTime += NS_IN_SEC;

        // Now let's start changing the data (for both instruments)
        for (int i = 0; i <= 50; ++i) {
            // Defining final version of i to allow using it inside lambda
            final int q = i;
            
            // Remove old level
            currentTime += NS_IN_SEC / 20;
            dataListeners.forEach(l -> l.onDepth("Test instrument", false, 40 + (q + 1), 0));
            // Add new level
            final int sizeBid1 = q * 5 + 100;
            currentTime += NS_IN_SEC / 20;
            dataListeners.forEach(l -> l.onDepth("Test instrument", false, 40 + (q + 1 + 10), sizeBid1));

            // Remove old level
            currentTime += NS_IN_SEC / 20;
            dataListeners.forEach(l -> l.onDepth("Test instrument", true, 40 + (q - 1 - 10), 0));
            // Add new level
            final int sizeAsk1 = q * 10 + 100;
            currentTime += NS_IN_SEC / 20;
            dataListeners.forEach(l -> l.onDepth("Test instrument", true, 40 + (q - 1), sizeAsk1));

            // Remove old level
            currentTime += NS_IN_SEC / 20;
            dataListeners.forEach(l -> l.onDepth("Test instrument 2", false, 500 + (-q + 1 + 5), 0));
            // Add new level
            final int sizeBid2 = q * 5 + 100;
            currentTime += NS_IN_SEC / 20;
            dataListeners.forEach(l -> l.onDepth("Test instrument 2", false, 500 + (-q + 1), sizeBid2));

            // Remove old level
            currentTime += NS_IN_SEC / 20;
            dataListeners.forEach(l -> l.onDepth("Test instrument 2", true, 500 + (-q - 1), 0));
            // Add new level
            final int sizeAsk2 = q * 10 + 100;
            currentTime += NS_IN_SEC / 20;
            dataListeners.forEach(l -> l.onDepth("Test instrument 2", true, 500 + (-q - 1 - 5), sizeAsk2));
        }
        
        
        BufferedImage icon;
        try {
            icon = ImageIO.read(new File("icon_accept.gif"));
        } catch (IOException e) {
            throw new RuntimeException("failed to load icon", e);
        }
        // Line and icons
        currentTime += NS_IN_SEC / 10;
        IndicatorDefinitionUserMessage indicatorDefinitionMessage = new IndicatorDefinitionUserMessage(
                1, "Test instrument 2",
                (short)0xFFFF, (short)1, 1, Color.ORANGE, 
                (short)0xFF08, (short)1, 2, Color.GREEN, 
                icon, -icon.getWidth() / 2, -icon.getHeight() / 2);
        // No line, only icons
//        IndicatorDefinitionUserMessage indicatorDefinitionMessage = new IndicatorDefinitionUserMessage(
//                1, "Test instrument 2",
//                (short)0x0000, (short)1, 1, Color.ORANGE,
//                (short)0x0000, (short)1, 2, Color.GREEN,
//                icon, -icon.getWidth() / 2, -icon.getHeight() / 2);
        // No icon, different line style
//        IndicatorDefinitionUserMessage indicatorDefinitionMessage = new IndicatorDefinitionUserMessage(
//                1, "Test instrument 2",
//                (short)0x5555, (short)20, 5, Color.ORANGE,
//                (short)0x5555, (short)40, 10, Color.GREEN,
//                null, 0, 0);
        adminListeners.forEach(l -> l.onUserMessage(indicatorDefinitionMessage));
        currentTime += NS_IN_SEC / 10;
        adminListeners.forEach(l -> l.onUserMessage(new IndicatorPointUserMessage(1, 4440.0)));
        currentTime += NS_IN_SEC;
        adminListeners.forEach(l -> l.onUserMessage(new IndicatorPointUserMessage(1, 4450.0)));
        currentTime += NS_IN_SEC;
        adminListeners.forEach(l -> l.onUserMessage(new IndicatorPointUserMessage(1, Double.NaN)));
        currentTime += NS_IN_SEC;
        adminListeners.forEach(l -> l.onUserMessage(new IndicatorPointUserMessage(1, 4450.0)));
        

        // Let's create a trade for the 2'nd instrument. We won't update depth data for simplicity.
        // Price is 4500.0 (pips is 10), size is 150, agressor is bid (last parameter of TradeInfo).
        dataListeners.forEach(l -> l.onTrade("Test instrument 2", 4500.0 / 10, 150, new TradeInfo(false, true)));

        
        //  Let's create an order
        currentTime += NS_IN_SEC;
        OrderInfoBuilder order = new OrderInfoBuilder("Test instrument 2", "order1", false, OrderType.LMT, "client-id-1", false);
        order
            .setLimitPrice(4580)
            .setUnfilled(5)
            .setDuration(OrderDuration.GTC)
            .setStatus(OrderStatus.PENDING_SUBMIT);
        
        tradingListeners.forEach(l -> l.onOrderUpdated(order.build()));
        order.markAllUnchanged();
        
        order.setStatus(OrderStatus.WORKING);
        tradingListeners.forEach(l -> l.onOrderUpdated(order.build()));
        order.markAllUnchanged();
        
        // Let's record order position data. If you comment this out BookMap will compute position using built-in algorithms
        for (int position = 310 /* 315 is the size on the order's price level, order size is 5, so initially there are 310 shares before our order*/;
                position > 100; --position) {
            // Decreasing order position - will look like it advances to the head of the queue
            currentTime += NS_IN_SEC / 30;
            final OrderQueuePositionUserMessage positionMessage = new OrderQueuePositionUserMessage("order1", position);
            adminListeners.forEach(l -> l.onUserMessage(positionMessage));
        }
        
        // Let's decrease the price
        currentTime += NS_IN_SEC;
        order.setLimitPrice(4480);
        tradingListeners.forEach(l -> l.onOrderUpdated(order.build()));
        order.markAllUnchanged();

        // Let's execute the order
        currentTime += NS_IN_SEC;
        ExecutionInfo executionInfo = new ExecutionInfo(
                order.orderId,
                5, 
                4480, 
                "execution-id-1",
                // Execution time in milliseconds. Used only in account information.
                currentTime / 1000_000);
        tradingListeners.forEach(l -> l.onOrderExecuted(executionInfo));
        
        // And mark it as filled
        order.setFilled(5);
        order.setUnfilled(0);
        order.setStatus(OrderStatus.FILLED);
        tradingListeners.forEach(l -> l.onOrderUpdated(order.build()));
        order.markAllUnchanged();
        
        // Report file end
        reportFileEnd();
    }

    public void reportFileEnd() {
        adminListeners.forEach(listener -> listener.onUserMessage(new FileEndReachedUserMessage()));
    }

    @Override
    public long getCurrentTime() {
        return currentTime;
    }

    @Override
    public String getSource() {
        // String identifying where data came from.
        // For example you can use that later in your indicator.
        return "generated example data";
    }

    @Override
    public void close() {
        readerThread.interrupt();
    }

}
