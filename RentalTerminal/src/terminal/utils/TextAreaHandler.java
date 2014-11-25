package terminal.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * Logging handler to print log messages in a TextArea
 * 
 * @author Federico Scrinzi
 *
 */
public class TextAreaHandler extends Handler {
    private JTextArea textArea = new JTextArea("==== CAR RENTAL LOG ====\n", 20, 60);

    @Override
    public void publish(final LogRecord record) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                StringWriter text = new StringWriter();
                PrintWriter out = new PrintWriter(text);
                out.println(textArea.getText());
                out.printf("[%s] [Thread-%d]: %s", record.getLevel(),
                        record.getThreadID(), record.getMessage());
                textArea.setText(text.toString());
            }
        });
    }

    public JTextArea getTextArea() {
        return this.textArea;
    }

	@Override
	public void close() throws SecurityException {}

	@Override
	public void flush() {}
}
