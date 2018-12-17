package velox.api.layer0.replay.advanced;

import java.text.SimpleDateFormat;
import java.util.HashMap;

import velox.api.layer1.data.InstrumentInfo;

public class HandlerBookmapSimple extends HandlerBase {

	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm:ss.SSS");
	protected final HashMap<Integer, InstrumentInfo> instruments = new HashMap<>();

	public HandlerBookmapSimple(HandlerListener listener, String fin) throws Exception {
		super(listener, fin);
	}

	private long getNanoseconds(String s) throws Exception {
		String strMillis = s.substring(0, 21);
		String strNanos = s.substring(21);
		long t = 1_000_000L * sdf.parse(strMillis).getTime() + Long.parseLong(strNanos);
		return t;
	}

	@Override
	protected void processLine(String line) throws Exception {
		String[] s = line.split(",");
		long t = getNanoseconds(s[0]);
		int instrID = Integer.parseInt(s[1]);
		String eventType = s[2];
		if (eventType.equals("Quote")) {
			onDepth(t, instrID, s[3].equals("Buy"), Long.parseLong(s[4]), Long.parseLong(s[5]));
		} else if (eventType.equals("BBO")) {
		} else if (eventType.equals("Trade")) {
			onTrade(t, instrID, s[3].equals("Buy"), Double.parseDouble(s[4]), Long.parseLong(s[5]));
		} else if (eventType.equals("InstrumentAdded")) {
			onInstrument(t, instrID, s[3].split("=")[1], Double.parseDouble(s[4].split("=")[1]),
					Double.parseDouble(s[5].split("=")[1]));
		} else if (eventType.equals("InstrumentRemoved")) {
		} else {
			throw new Exception("HandlerBookmapSimple: unrecognized event type: " + eventType);
		}
	}

	protected void onDepth(long t, int id, boolean isBuy, long price, long size) throws Exception {
		InstrumentInfo instrumentInfo = instruments.get(id);
        listener.onDepth(t, instrumentInfo.symbol, isBuy, (int)price, (int)size);
	}

	protected void onTrade(long t, int id, boolean isBuy, double price, long size) throws Exception {
		InstrumentInfo instrumentInfo = instruments.get(id);
		listener.onTrade(t, instrumentInfo.symbol, price, (int)size, isBuy);
	}

	protected void onInstrument(long t, int id, String alias, double pips, double multiplier) throws Exception {
		InstrumentInfo instrumentInfo = new InstrumentInfo(alias, null, null, pips, multiplier, alias, true);
		instruments.put(id, instrumentInfo);
		listener.onInstrument(t, instrumentInfo);
	}
}
