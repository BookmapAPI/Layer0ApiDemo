package velox.api.layer0.replay.advanced;

import velox.api.layer0.data.IndicatorDefinitionUserMessage;
import velox.api.layer0.data.IndicatorPointUserMessage;
import velox.api.layer0.data.TextDataMessage;
import velox.api.layer1.data.InstrumentInfo;

public interface HandlerListener {

    void onFileEnd();

    void onDepth(long t, String alias, boolean isBuy, int price, int size);

    void onTrade(long t, String alias, double price, int size, boolean isBidAggressor);

    void onInstrument(long t, InstrumentInfo instrumentInfo);

    void onTextData(long t, TextDataMessage textDataMessage);
    
    void onIndicatorDefinition(long t, IndicatorDefinitionUserMessage indicatorDefinitionUserMessage);
    void onIndicatorPoint(long t, IndicatorPointUserMessage indicatorPointUserMessage);
}

