package terminal.gui;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
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
		//Set up application window
		setTitle(TITLE);
		setSize(300, 200);
		setLocationRelativeTo(null);
		
		logArea = Log.getLoggingArea();
		
        JPanel panel = new JPanel(new BorderLayout());
        JPanel top = new JPanel();
       
        panel.add(top, BorderLayout.PAGE_START);
        
        //Define components
        startButton = new JButton("Start Car");
        JLabel kilometerLabel = new JLabel("Enter kilometer:");
        kilometer = new JTextField(8);

        stopButton = new JButton("Stop Car");
                              
        stopButton.setEnabled(false);
        
        //logArea = new JTextArea("==== CAR RENTAL LOG ====\n", 20, 60);
		//logArea.setEditable(false);
                
        //Add components to layout
		top.add(startButton);
		top.add(kilometerLabel);
		top.add(kilometer);
		top.add(stopButton);
           
		add(panel);


		add(logArea, BorderLayout.PAGE_END);
		logArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		
		//Add action listener
		startButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent action) {
				
				try{
					driveKilometers = Double.parseDouble(kilometer.getText());
					
					if(isKilometerFieldValid(driveKilometers)){
						driving = true;
						stopButton.setEnabled(true);
						startButton.setEnabled(false);
						kilometer.setEnabled(false);
						Log.info("Driving "+driveKilometers+" kilometers");
					}
					else{
						Log.info("Invalid value for kilometer");
						driveKilometers=0;
					}
					
					
				} catch(NumberFormatException e){
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
