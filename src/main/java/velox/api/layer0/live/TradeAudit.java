package velox.api.layer0.live;

import velox.api.layer1.data.ExecutionInfo;

import java.math.BigDecimal;
import java.math.MathContext;

import static java.math.BigDecimal.valueOf;

/**
 * A container for a position and P&L information
 */
class TradeAudit {
    int position;
    int volume;
    double realizedPnl;
    double averagePrice = Double.NaN;
    
    private BigDecimal position() {
        return valueOf(position);
    }
    
    private BigDecimal realizedPnl() {
        return valueOf(realizedPnl);
    }
    
    private BigDecimal averagePrice() {
        return valueOf(averagePrice);
    }
    
    /**
     * Unrealized P&L can be calculated as: 
     * (Theoretical Exit Price – Average Open Price) * Position
     */
    double getUnrealizedPnl(double theoreticalExitPrice) {
        if (position == 0) {
            return 0;
        }
        
        BigDecimal diff = valueOf(theoreticalExitPrice).subtract(averagePrice());
        return diff.multiply(position()).doubleValue();
    }
    
    /**
     * Recalculates trade audit information on each order execution 
     * 
     * @param isBuy Side of execution (buy/sell)
     * @param info Information about order execution
     */
    void recalculateInfo(boolean isBuy, ExecutionInfo info) {
        volume += info.size;
        
        double oldPosition = position;
        position += isBuy ? info.size : -info.size;
        
        if (oldPosition == 0) {
            // open a position
            averagePrice = info.price;
        } else if (oldPosition > 0 && isBuy || oldPosition < 0 && !isBuy) {
            // the case when increasing the existing position long or short
            BigDecimal totalPrice = valueOf(oldPosition).abs()
                    .multiply(averagePrice(), MathContext.DECIMAL64)
                    .add(valueOf(info.size).multiply(valueOf(info.price)), MathContext.DECIMAL64);
            
            averagePrice = totalPrice
                    .divide(position().abs(), MathContext.DECIMAL64)
                    .doubleValue();
        } else {
            // the case when reducing the existing position,
            // also making a check if a counter-side position was opened
            // e.g. the current position is 2, we're selling 3 contracts 
            // and now the position is -1
            double oldPosAbs = Math.abs(oldPosition);
            double minQty = Math.min(oldPosAbs, info.size);
            
            // PnL realized += (Sell Price - Buy Price) * Qty
            BigDecimal sellPrice = isBuy ? averagePrice() : BigDecimal.valueOf(info.price);
            BigDecimal buyPrice = isBuy ? BigDecimal.valueOf(info.price) : averagePrice();
            BigDecimal priceDiff = sellPrice.subtract(buyPrice);
            
            realizedPnl = realizedPnl()
                    .add(priceDiff.multiply(valueOf(minQty), MathContext.DECIMAL64))
                    .doubleValue();
            if (oldPosAbs >= info.size) {
                // avg open price does not change since we're reducing the position
                // but set it to NaN if the position is closed (0)
                averagePrice = position == 0 ? Double.NaN : averagePrice;
            } else {
                // for a counter-side position,
                // the average price will be the latest execution price
                averagePrice = info.price;
            }
        }
    }
}