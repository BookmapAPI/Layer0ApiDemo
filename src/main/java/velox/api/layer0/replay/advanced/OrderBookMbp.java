package velox.api.layer0.replay.advanced;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class OrderBookMbp {
	public TreeMap<Long, Long> bids = new TreeMap<>(Collections.reverseOrder());
	public TreeMap<Long, Long> asks = new TreeMap<>();

	public void onDepth(boolean isBid, long price, long size) {
		Map<Long, Long> book = isBid ? bids : asks;
		if (size == 0)
			book.remove(price);
		else
			book.put(price, size);
	}
}
