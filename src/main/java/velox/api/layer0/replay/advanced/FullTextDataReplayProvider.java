package velox.api.layer0.replay.advanced;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import velox.api.layer0.annotations.Layer0ReplayModule;
import velox.api.layer0.common.TextStreamParser;
import velox.api.layer0.data.FileNotSupportedUserMessage;
import velox.api.layer0.data.ReadFileLoginData;
import velox.api.layer0.replay.DemoTextDataReplayProvider;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.common.ListenableHelper;
import velox.api.layer1.data.Layer1ApiProviderSupportedFeatures;
import velox.api.layer1.data.Layer1ApiProviderSupportedFeaturesBuilder;
import velox.api.layer1.data.LoginData;
import velox.api.layer1.layers.Layer1ApiRelay;

/**
 * <p>
 * Similar to {@link DemoTextDataReplayProvider} but supports more detailed
 * format essentially allowing to use it instead of Recorder API.
 * </p>
 * <p>
 * It is generally similar to {@link DemoTextDataReplayProvider} but is more
 * complicated/supports more events, so that might be a better place to look if
 * you are just getting started
 * </p>
 * <p>
 * You can download sample file <a href="https://bookmap.com/shared/feeds/FullTextDataReplayProviderDemo-1.bmtext.gz">here</a>
 * </p>
 */
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
@Layer0ReplayModule
public class FullTextDataReplayProvider extends Layer1ApiRelay {

    TextStreamParser parser;

    public FullTextDataReplayProvider() {
        super(null);
    }
    
    @Override
    public void login(LoginData loginData) {
        ReadFileLoginData fileData = (ReadFileLoginData) loginData;

        try {
            File file = fileData.file;
            String fileName = file.getName();
            boolean isRawBmtext = fileName.endsWith(".bmtext");
            boolean isGzippedBmtext = fileName.endsWith(".bmtext.gz");
            if (!isRawBmtext && !isGzippedBmtext) {
                throw new IOException("File extension not supported");
            } else {

                parser = new TextStreamParser();
                ListenableHelper.addListeners(parser, this);
                
                InputStream inputStream = new FileInputStream(file);
                if (isGzippedBmtext) {
                    inputStream = new GZIPInputStream(inputStream);
                }
                parser.start(inputStream);
            }
        } catch (@SuppressWarnings("unused") IOException e) {
            adminListeners.forEach(listener -> listener.onUserMessage(new FileNotSupportedUserMessage()));
        }
    }
    
    @Override
    public Layer1ApiProviderSupportedFeatures getSupportedFeatures() {
        return new Layer1ApiProviderSupportedFeaturesBuilder().build();
    }

    @Override
    public long getCurrentTime() {
        return parser.getCurrentTime();
    }

    @Override
    public String getSource() {
        return "Advanced text format";
    }

    @Override
    public void close() {
        if (parser != null) {
            parser.close();
        }
    }

}
