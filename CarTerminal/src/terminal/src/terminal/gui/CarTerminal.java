package terminal.gui;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

// TODO Add dialog such that user can "select" driven kilometers
// TODO Optional: Add option to simulate failure of kilometer writing

public class CarTerminal extends JFrame {

	private static final long serialVersionUID = -3770099088414835331L;
	static final String TITLE = "CarTerminal";

	JTextArea logArea;

	public CarTerminal() {
		initUI();
	}
	
	private void initUI(){
		//Set up application window
		setTitle(TITLE);
		setSize(300, 200);
		setLocationRelativeTo(null);
		
        JPanel panel = new JPanel(new BorderLayout());
        JPanel top = new JPanel();
       
        panel.add(top, BorderLayout.PAGE_START);

        //Define components
        JButton startButton = new JButton("Start Car");
        JButton stopButton = new JButton("Stop Car");
        
        logArea = new JTextArea("==== CAR RENTAL LOG ====\n", 20, 60);
		logArea.setEditable(false);
                
        //Add components to layout
		top.add(startButton);
		top.add(stopButton);
           
		add(panel);


		add(logArea, BorderLayout.PAGE_END);
		logArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		
		//Add action listener
		startButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent action) {
				log("Pressed " + action.getActionCommand());
			}
		});
		
		stopButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent action) {
				log("Pressed " + action.getActionCommand());
			}
		});
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
	}

	public void log(String s) {
		logArea.append(s + "\n");
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
