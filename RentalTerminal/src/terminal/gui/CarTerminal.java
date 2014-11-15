package terminal.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;

import terminal.commands.CardCommunication;
import terminal.crypto.ECCKeyGenerator;
import terminal.utils.Log;


// TODO Optional: Add option to simulate failure of kilometer writing

public class CarTerminal extends JFrame implements TerminalInterface {

	private static final long serialVersionUID = -3770099088414835331L;
	static final String TITLE = "CarTerminal";

	private boolean driving = false;
	JButton startButton;
	JButton stopButton;
	JTextField kilometerField;
	JComboBox carsList;

	int driveKilometers = 0;
	private KeyPair carKeyPair;

	JTextArea logArea;
	private CardCommunication comm;

	public CarTerminal() {
    	comm = new CardCommunication(this);
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
        scrollLogArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
        // Select car combobox
        JPanel selectCarBox = new JPanel();
        carsList = new JComboBox(new String[] {"car0", "car1", "car2", "car3", "car4", "car5"});
        selectCarBox.setLayout(new BoxLayout(selectCarBox, BoxLayout.X_AXIS));
        selectCarBox.add(new JLabel("Select car:"));
        selectCarBox.add(Box.createRigidArea(new Dimension(10, 0)));
        selectCarBox.add(carsList);
        Border padding = BorderFactory.createEmptyBorder(10,100,10,100);
        selectCarBox.setBorder(padding);
        
        // Define main commands
        JPanel mainCmdPanel = new JPanel();
        startButton = new JButton("Start Car");
        kilometerField = new JTextField(8);
        stopButton = new JButton("Stop Car");
        stopButton.setEnabled(false);
                
        // Add components to layout
		mainCmdPanel.add(startButton);
		mainCmdPanel.add(new JLabel("Driven kilometers:"));
		mainCmdPanel.add(kilometerField);
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
					carKeyPair = ECCKeyGenerator.loadKeys("keys/cars", carsList.getSelectedItem().toString());
				} catch (Exception e) {
					Log.error(e.getMessage());
					e.printStackTrace();
				}

				driving = true;
				carsList.setEnabled(false);
				startButton.setEnabled(false);
				stopButton.setEnabled(true);
				kilometerField.setEnabled(true);
			}
		});
		
		stopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent action) {
				try {
					driveKilometers = Integer.parseInt(kilometerField.getText());
				} catch (NumberFormatException e) {
					driveKilometers = 0;
				}
				
				if (isKilometerFieldValid(driveKilometers)) {
					Log.info(driveKilometers + " kilometers driven");
					driving = false;
					carsList.setEnabled(true);					
					startButton.setEnabled(true);
					stopButton.setEnabled(false);
					kilometerField.setEnabled(false);
				}
				else {
					Log.info("Invalid value for kilometer");
				}
			}
		});
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
	}
	
	// Verify that entered kilometers are valid
	boolean isKilometerFieldValid(int value){
		if(value >= 1 && value <= 99999)
			return true;
		else
			return false;
	}

	@Override
	public void cardInserted() {
		if (driving) {
			carsList.setEnabled(false);
			startButton.setEnabled(false);
			stopButton.setEnabled(true);
			kilometerField.setEnabled(true);
		}
		else {
			carsList.setEnabled(true);
			startButton.setEnabled(true);
			stopButton.setEnabled(false);
			kilometerField.setEnabled(false);
		}
	}

	@Override
	public void cardRemoved() {
		carsList.setEnabled(false);
		startButton.setEnabled(false);
		stopButton.setEnabled(false);
		kilometerField.setEnabled(false);
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
