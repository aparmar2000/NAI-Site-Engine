package aparmar.naisiteengine.ui;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

import aparmar.naisiteengine.utils.StringPipeAppender;

public class JThreadMonitorPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private final JTextPane loggingPane;

	/**
	 * Create the panel.
	 */
	public JThreadMonitorPanel(Logger logger) {
		setLayout(new BorderLayout(0, 0));
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		add(scrollPane, BorderLayout.CENTER);
		
		
		loggingPane = new JTextPane();
		scrollPane.setViewportView(loggingPane);
		
		JPanel panel = new JPanel();
		add(panel, BorderLayout.EAST);
		
		logger.addAppender(new StringPipeAppender(new SimpleLayout(), this::updateLog));
	}
	
	private void updateLog(String logString) {
		try {
			Document logDocument = loggingPane.getDocument();
			logDocument.insertString(logDocument.getLength(), logString, null);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

}
