package aparmar.naisiteengine.utils;

import java.util.function.Consumer;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

public class StringPipeAppender extends AppenderSkeleton {
	private final Consumer<String> consumer;
	
    public StringPipeAppender(Layout layout, Consumer<String> consumer) {
    	this.layout = layout;
        this.consumer = consumer;
    }

	@Override
	public void close() { this.closed = true; }

	@Override
	public boolean requiresLayout() { return true; }

	@Override
	protected void append(LoggingEvent event) {
		if (checkEntryConditions()) {
			subAppend(event);
		}
	}
	
    protected boolean checkEntryConditions() {
        if (this.closed) {
            LogLog.warn("Not allowed to write to a closed appender.");
            return false;
        }

        if (this.layout == null) {
            errorHandler.error("No layout set for the appender named [" + name + "].");
            return false;
        }
        return true;
    }
    
    protected void subAppend(LoggingEvent event) {
        String nextOut = this.layout.format(event);

        if (layout.ignoresThrowable()) {
            String[] s = event.getThrowableStrRep();
            if (s != null) {
                int len = s.length;
                for (int i = 0; i < len; i++) {
                	nextOut += s[i];
                	nextOut += Layout.LINE_SEP;
                }
            }
        }

        consumer.accept(nextOut);
    }

}
