package velox.api.layer0.replay;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import com.google.gson.Gson;

import velox.api.layer0.annotations.Layer0ReplayModule;
import velox.api.layer0.data.FileEndReachedUserMessage;
import velox.api.layer0.data.FileNotSupportedUserMessage;
import velox.api.layer0.data.ReadFileLoginData;
import velox.api.layer1.Layer1ApiListener;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.LoginData;
import velox.api.layer1.data.TradeInfo;

/**
 * Allows reading simple text format (that mimics {@link Layer1ApiListener}
 * methods) to be replayed by Bookmap.
 */
@Layer0ReplayModule
public class DemoTextDataReplayProvider extends ExternalReaderBaseProvider {

    private final Gson gson = new Gson();

    private Thread readerThread;
    private long currentTime = 0;

    private boolean play = true;

    private BufferedReader reader;

    @Override
    public void login(LoginData loginData) {
        ReadFileLoginData fileData = (ReadFileLoginData) loginData;

        try {
            // For demo purposes let's just check the extension.
            // Usually you will want to take a look at file content here to
            // ensure it's expected file format
            if (!fileData.file.getName().endsWith(".simpleformat.txt")) {
                throw new IOException("File extension not supported");
            } else {

                reader = new BufferedReader(new FileReader(fileData.file));

                // Reading one line to guarantee that when we exit this method
                // getCurrentTime will return meaningful result.
                // Alternative is to wait for first line to be read by
                // readerThread
                // Reading it here also allows a bit of extra validation, since
                // in case of error it's still possible to report that file is
                // not supported
                readLine();

                readerThread = new Thread(this::read);
                readerThread.start();
            }
        } catch (@SuppressWarnings("unused") IOException e) {
            adminListeners.forEach(listener -> listener.onUserMessage(new FileNotSupportedUserMessage()));
        }
    }

    private void read() {
        try {
            while (!Thread.interrupted() && play) {
                readLine();
            }
        } catch (@SuppressWarnings("unused") IOException e) {
            reportFileEnd();
        }
    }

    public void reportFileEnd() {
        adminListeners.forEach(listener -> listener.onUserMessage(new FileEndReachedUserMessage()));
        play = false;
    }

    private void readLine() throws IOException {
        String line = reader.readLine();
        if (line == null && play) {
            reportFileEnd();
        } else {
            String[] tokens = line.split(";;;");
            currentTime = Long.parseLong(tokens[0]);
            String eventCode = tokens[1];
            switch (eventCode) {
            case "onInstrumentAdded": {
                String alias = tokens[2];
                InstrumentInfo instrumentInfo = gson.fromJson(tokens[3], InstrumentInfo.class);
                instrumentListeners.forEach(
                        l -> l.onInstrumentAdded(alias, instrumentInfo));
                break;
            }
            case "onTrade": {
                String alias = tokens[2];
                double price = Double.parseDouble(tokens[3]);
                int size = Integer.parseInt(tokens[4]);
                TradeInfo tradeInfo = gson.fromJson(tokens[5], TradeInfo.class);
                dataListeners.forEach(
                        l -> l.onTrade(alias, price, size, tradeInfo));
                break;
            }
            case "onDepth": {
                String alias = tokens[2];
                boolean isBid = Boolean.parseBoolean(tokens[3]);
                int price = Integer.parseInt(tokens[4]);
                int size = Integer.parseInt(tokens[5]);

                dataListeners.forEach(
                        l -> l.onDepth(alias, isBid, price, size));
                break;
            }

            default:
                reportFileEnd();
                throw new RuntimeException("Unknown event code " + eventCode);
            }
        }
    }

    @Override
    public long getCurrentTime() {
        return currentTime;
    }

    @Override
    public String getSource() {
        // String identifying where data came from.
        // For example you can use that later in your indicator.
        return "simple example data";
    }

    @Override
    public void close() {
        readerThread.interrupt();
        try {
            reader.close();
        } catch (@SuppressWarnings("unused") IOException e) {
        }
    }

}
