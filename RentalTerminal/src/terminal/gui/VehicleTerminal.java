package terminal.gui;

import game.Quickgame;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;

import terminal.commands.CardCommunication;
import terminal.commands.VehicleCommands;
import terminal.utils.Log;


/**
 * GUI for vehicle terminal
 * 
 * @author Mortiz Muller
 * @author Federico Scrinzi
 *
 */
public class VehicleTerminal extends JFrame implements TerminalInterface {

	private static final long serialVersionUID = -3770099088414835331L;
	static final String TITLE = "VehicleTerminal";

	private boolean driving = false;
	JButton startButton;
	JButton stopButton;
	JTextField kilometerField;
	JComboBox carsList;

	long driveKilometers = 0;

	JTextArea logArea;
	private CardCommunication comm;
	VehicleTerminal frame = this;


	public VehicleTerminal() {
		initUI();
    	comm = new CardCommunication(this);
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

		carsList.setEnabled(false);
        startButton.setEnabled(false);
        stopButton.setEnabled(false);
        kilometerField.setEnabled(false);
        
                
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
				String carID = carsList.getSelectedItem().toString();
				try {
					VehicleCommands vehicleCmds = new VehicleCommands(comm, carID);
					vehicleCmds.startVehicle();
					Log.info("Started car: " + carID);
					Quickgame g = new Quickgame(frame);
					g.startCar();

				} catch (IllegalStateException e) {  // inUse flag popup error
					Log.error(e.getMessage());
					
					JOptionPane.showMessageDialog(frame, e.getMessage());					
				} catch (Exception e) {
					Log.error(e.getMessage());
					e.printStackTrace();
					return;
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
				String carID = carsList.getSelectedItem().toString();

				try {
					driveKilometers = Long.parseLong(kilometerField.getText());
				} catch (NumberFormatException e) {
					driveKilometers = 0;
				}
				
				if (isKilometerFieldValid(driveKilometers)) {
					try {
						VehicleCommands vehicleCmds = new VehicleCommands(comm, carID);
						vehicleCmds.stopVehicle(driveKilometers);
						kilometerField.setText("");
						Log.info("Stopped car: " + carID + " and written " + driveKilometers + "km to card.");
					} catch (Exception e) {
						Log.error(e.getMessage());
						e.printStackTrace();
						return;
					}
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
	
	/**
	 * Verify that entered kilometers are valid
	 * @param value the km value to verify
	 * @return true if it is valid, false otherwise
	 */
	boolean isKilometerFieldValid(long value){
		if(value >= 0 && value <= Long.MAX_VALUE)
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
				VehicleTerminal cT = new VehicleTerminal();
				cT.setVisible(true);
			}
		});
	}

	public void setKilometers(long km) {
		kilometerField.setText(Long.toString(km));
	}
}
