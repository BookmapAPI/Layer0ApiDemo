package velox.api.layer0.replay.advanced;

import java.io.BufferedReader;
import java.io.FileReader;

public abstract class HandlerBase {

    protected final HandlerListener listener;
    private final String filenameIn;
    protected boolean skipFirstLine = false;
    private volatile boolean shouldStop = false;

    public HandlerBase(HandlerListener listener, String fin) {
        this.listener = listener;
        this.filenameIn = fin;
    }

    public void run() throws Exception {
        String line;
        BufferedReader reader = new BufferedReader(new FileReader(filenameIn));
        if (skipFirstLine) {
            reader.readLine();
        }
        int n = 0;
        while ((line = reader.readLine()) != null && !shouldStop) {
            processLine(line);
            n++;
        }
        System.out.println("Lines processed: " + n);
        listener.onFileEnd();
        reader.close();
    }
    
    public void stop() {
        shouldStop = true;
    }

    protected abstract void processLine(String line) throws Exception;
}
