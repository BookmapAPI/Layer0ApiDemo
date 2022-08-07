package velox.api.layer0.replay.advanced;

public class IndicatorsPack {

    private final IntrinsicPrice intrinsicPrice = new IntrinsicPrice();
    private final double[] intrinsicParams;
    private final DynamicAverage avgSize = new DynamicAverage();
    private final Ema[] emaBuy;
    private final Ema[] emaSell;

    public IndicatorsPack(double[] intrinsicParams, double[] emaParams) {
        this.intrinsicParams = intrinsicParams;
        int n = emaParams.length;
        emaBuy = new Ema[n];
        emaSell = new Ema[n];
        for (int i = 0; i < n; i++) {
            emaBuy[i] = new Ema(emaParams[i]);
            emaSell[i] = new Ema(emaParams[i]);
        }
    }

    public void onDepth(boolean isBuy, long price, long size) {
        intrinsicPrice.onDepth(isBuy, price, size);
        avgSize.update(size);
    }

    public void onTrade(long t, boolean isBuy, long size) {
        Ema[] ema = isBuy ? emaBuy : emaSell;
        for (Ema e : ema) {
            e.onUpdate(t, size);
        }
    }

    public double getIntrinsic(boolean isBid, int idx) {
        long hypotheticalMarketOrderSize = Math.round(intrinsicParams[idx] * avgSize.getAverage());
        double intrinsic = intrinsicPrice.calcIntrinsic(isBid, hypotheticalMarketOrderSize);
        return intrinsic;
    }

    public double getEma(long t, boolean isBuy, int idx) {
        double ema = (isBuy ? emaBuy : emaSell)[idx].getValue(t);
        return ema;
    }
}
