package terminal.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import terminal.utils.Log;

// TODO Add dialog such that user can "select" driven kilometers
// TODO Optional: Add option to simulate failure of kilometer writing

public class CarTerminal extends JFrame {

	private static final long serialVersionUID = -3770099088414835331L;
	static final String TITLE = "CarTerminal";

	private boolean driving = false;
	JButton startButton;
	JButton stopButton;
	JTextField kilometer;

	double driveKilometers = 0;

	JTextArea logArea;

	public CarTerminal() {
		initUI();
	}

	private void initUI(){
		// Set up application window
		setTitle(TITLE);
		setSize(300, 200);
		setLocationRelativeTo(null);

		// Logging area
        JTextArea logArea = Log.getLoggingArea();
        logArea.setEditable(false);
        JScrollPane scrollLogArea = new JScrollPane(logArea);
        scrollLogArea.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollLogArea.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		
        // Select car combobox
        JPanel selectCarBox = new JPanel();
        final JComboBox carsList = new JComboBox(new String[] {"car1", "car2", "car3", "car4", "car5"});
        selectCarBox.setLayout(new BoxLayout(selectCarBox, BoxLayout.X_AXIS));
        selectCarBox.add(new JLabel("Select car:"));
        selectCarBox.add(Box.createRigidArea(new Dimension(10, 0)));
        selectCarBox.add(carsList);
        Border padding = BorderFactory.createEmptyBorder(10,100,10,100);
        selectCarBox.setBorder(padding);
        
        // Define main commands
        JPanel mainCmdPanel = new JPanel();
        startButton = new JButton("Start Car");
        kilometer = new JTextField(8);
        stopButton = new JButton("Stop Car");
        stopButton.setEnabled(false);
                
        // Add components to layout
		mainCmdPanel.add(startButton);
		mainCmdPanel.add(new JLabel("Driven kilometers:"));
		mainCmdPanel.add(kilometer);
		mainCmdPanel.add(stopButton);
		
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(selectCarBox, BorderLayout.PAGE_START);
        panel.add(mainCmdPanel);
		add(panel);
		add(scrollLogArea, BorderLayout.PAGE_END);		
		
		//Add action listener
		startButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent action) {
				try {
					driveKilometers = Double.parseDouble(kilometer.getText());
					
					if(isKilometerFieldValid(driveKilometers)) {
						driving = true;
						stopButton.setEnabled(true);
						startButton.setEnabled(false);
						kilometer.setEnabled(false);
						Log.info("Driving "+driveKilometers+" kilometers");
					}
					else {
						Log.info("Invalid value for kilometer");
						driveKilometers=0;
					}
				} catch(NumberFormatException e) {
					Log.info("Invalid value for kilometer");
					driveKilometers=0;
				}
				Log.info("Pressed " + action.getActionCommand());
			}
		});
		
		stopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent action) {
				driving = false;
				stopButton.setEnabled(false);
				startButton.setEnabled(true);
				kilometer.setEnabled(true);
				Log.info("Pressed " + action.getActionCommand());
			}
		});
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
	}
	
	// Verify that entered kilometers are valid
	boolean isKilometerFieldValid(Double value){
		if(value >= 1 && value <= 99999999)
			return true;
		else
			return false;
	}

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				CarTerminal cT = new CarTerminal();
				cT.setVisible(true);
			}
		});
	}
}
