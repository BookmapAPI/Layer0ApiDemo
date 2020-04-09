package velox.api.layer0.live;

import velox.api.layer1.data.TradeInfo;

class Instrument {
    /** Number of depth levels that will be generated on each side */
    private static final int DEPTH_LEVELS_COUNT = 10;

    private final DemoExternalRealtimeProvider demoExternalRealtimeProvider;
    protected final String alias;
    protected final double pips;

    private int basePrice;

    public Instrument(DemoExternalRealtimeProvider demoExternalRealtimeProvider, String alias, double pips) {
        this.demoExternalRealtimeProvider = demoExternalRealtimeProvider;
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
            demoExternalRealtimeProvider.dataListeners.forEach(l -> l.onDepth(alias, true, bestBid - levelsOffset, getRandomSize()));
            demoExternalRealtimeProvider.dataListeners.forEach(l -> l.onDepth(alias, false, bestAsk + levelsOffset, getRandomSize()));
        }

        // Currently Bookmap does not visualize OTC trades, so you will
        // mostly want isOtc=false
        final boolean isOtc = false;
        // Trade on best bid, ask aggressor
        demoExternalRealtimeProvider.dataListeners.forEach(l -> l.onTrade(alias, bestBid, 1, new TradeInfo(isOtc, false)));
        // Trade on best ask, bid aggressor
        demoExternalRealtimeProvider.dataListeners.forEach(l -> l.onTrade(alias, bestAsk, 1, new TradeInfo(isOtc, true)));

        // With 10% chance change BBO
        if (Math.random() < 0.1) {
            // 50% chance to move up, 50% to move down
            if (Math.random() > 0.5) {
                // Moving up - erasing best ask, erasing last reported bid
                // level (emulating exchange only reporting few levels)
                ++basePrice;
                demoExternalRealtimeProvider.dataListeners.forEach(l -> l.onDepth(alias, false, bestAsk, 0));
                demoExternalRealtimeProvider.dataListeners.forEach(l -> l.onDepth(alias, true, bestBid - (DEPTH_LEVELS_COUNT - 1), 0));
                // Could also populate new best bid and add last best ask,
                // but this can be omitted - those will be populated during
                // next simulation step
            } else {
                // Moving down - erasing best bid, erasing last reported ask
                // level (emulating exchange only reporting few levels)
                --basePrice;
                demoExternalRealtimeProvider.dataListeners.forEach(l -> l.onDepth(alias, true, bestBid, 0));
                demoExternalRealtimeProvider.dataListeners.forEach(l -> l.onDepth(alias, false, bestAsk + (DEPTH_LEVELS_COUNT - 1), 0));
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
