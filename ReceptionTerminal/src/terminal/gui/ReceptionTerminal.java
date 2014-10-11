package terminal.gui;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.JFrame;
import java.beans.*; //Property change stuff
import java.awt.*;
import java.awt.event.*;


public class ReceptionTerminal extends JPanel {
	static final String TITLE = "ReceptionTerminal";
	
    JTextArea logArea;
    JFrame frame;
    
    /** Creates the GUI shown inside the frame's content pane. */
    public ReceptionTerminal(JFrame frame) {
        super(new BorderLayout());
        this.frame = frame;

        // Create the components.
        JPanel setupPanel = createSetupPanel();
        JPanel commandsPanel = createCommandsPanel();
        logArea = new JTextArea("==== CAR RENTAL LOG ====\n", 20, 60);
        logArea.setEditable(false);

        //Lay them out.
        Border padding = BorderFactory.createEmptyBorder(20,20,5,20);
        setupPanel.setBorder(padding);
        commandsPanel.setBorder(padding);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Setup", null,
                          setupPanel,
                          "Setup panel"); //tooltip text
        tabbedPane.addTab("Commands", null,
                          commandsPanel,
                          "Commands panel"); //tooltip text

        add(tabbedPane, BorderLayout.CENTER);
        add(logArea, BorderLayout.PAGE_END);
        logArea.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
    }

    /** Creates the panel shown by the first tab. */
    private JPanel createSetupPanel() {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.PAGE_AXIS));
        
        JLabel label = new JLabel("Here you can personalize a smartcard and generate keys");
        //label.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(label);
        box.add(Box.createRigidArea(new Dimension(0,10)));

        JButton cardSetup = new JButton("Setup card");
        box.add(cardSetup);
        box.add(Box.createRigidArea(new Dimension(0,10)));
        
        cardSetup.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent action) {
				log("Pressed " + action.getActionCommand());
			}
		});
        
        JButton generateVehicleKey = new JButton("Generate vehicle key");
        box.add(generateVehicleKey);
        box.add(Box.createRigidArea(new Dimension(0,10)));
        
        generateVehicleKey.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent action) {
				log("Pressed " + action.getActionCommand());
			}
		});

        JPanel pane = new JPanel(new BorderLayout());
        pane.add(box, BorderLayout.PAGE_START);
        // pane.add(showButton, BorderLayout.PAGE_END);
        return pane;
    }
    
    public void log(String s) {
    	logArea.append(s + "\n");
    }

    /** Creates the panel shown by the second tab. */
    private JPanel createCommandsPanel() {        
        // Possible operations
        String CHECK_INUSE = "Check InUse flag";
        String ADD_VEHICLE = "Add certificate for vehicle";
        String REMOVE_VEHICLE = "Remove certificate from card";
        String GET_MILEAGE = "Get mileage counting";
        String RESET_MILEAGE = "Reset mileage counting";

        String[] commands = {CHECK_INUSE, ADD_VEHICLE, REMOVE_VEHICLE, GET_MILEAGE, RESET_MILEAGE};
        
        final JComboBox commandsList = new JComboBox(commands);

        JPanel cmdBox = new JPanel();
        cmdBox.setLayout(new BoxLayout(cmdBox, BoxLayout.X_AXIS));
        cmdBox.add(commandsList);
        cmdBox.add(Box.createRigidArea(new Dimension(20, 0)));
        
        JButton launchCommand = new JButton("Launch command");
        cmdBox.add(launchCommand);
        
        launchCommand.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent action) {
				log("Pressed " + commandsList.getSelectedItem());
			}
		});
        
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.PAGE_AXIS));
        
        JLabel label = new JLabel("Here you can send commands to the card");
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        box.add(label);
        box.add(Box.createRigidArea(new Dimension(0, 10)));
        
        box.add(cmdBox);
        
        JPanel pane = new JPanel(new BorderLayout());
        pane.add(box, BorderLayout.PAGE_START);
        // pane.add(launchCommand, BorderLayout.PAGE_END);
        return pane;
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame(TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        ReceptionTerminal newContentPane = new ReceptionTerminal(frame);
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}