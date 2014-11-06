package terminal.gui;

import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.JFrame;

import terminal.commands.CardCommunication;
import terminal.commands.PersonalizationCommands;
import terminal.utils.Log;
import terminal.crypto.EECKeyGenerator;

import java.awt.*;
import java.awt.event.*;

public class ReceptionTerminal extends JFrame {
	private static final long serialVersionUID = -3660099088414835331L;

	static final String TITLE = "ReceptionTerminal";
	
	private CardCommunication comm;
        
    public ReceptionTerminal() {
    	initUI();
    	comm = new CardCommunication();
    }
    
    /** Creates the GUI shown inside the frame's content pane. */
    private void initUI() {
        this.setTitle(TITLE);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel newContentPane = new JPanel(new BorderLayout());
        newContentPane.setOpaque(true); //content panes must be opaque
        setContentPane(newContentPane);
        
        // Create the components.
        JPanel setupPanel = createSetupPanel();
        JPanel commandsPanel = createCommandsPanel();
        JTextArea logArea = Log.getLoggingArea();
        logArea.setEditable(false);
        JScrollPane scrollLogArea = new JScrollPane(logArea);
        scrollLogArea.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollLogArea.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        //Lay them out.
        Border padding = BorderFactory.createEmptyBorder(20,20,5,20);
        setupPanel.setBorder(padding);
        commandsPanel.setBorder(padding);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Commands", null,
                          commandsPanel,
                          "Commands panel"); //tooltip text
        tabbedPane.addTab("Setup", null,
                		  setupPanel,
                		  "Setup panel"); //tooltip text
        
        newContentPane.add(tabbedPane, BorderLayout.CENTER);
        newContentPane.add(scrollLogArea, BorderLayout.PAGE_END);
        
        //frame.setEnabled(false);
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
				PersonalizationCommands cmds = new PersonalizationCommands(comm);
				cmds.doIssuance();
			}
		});

        JPanel pane = new JPanel(new BorderLayout());
        pane.add(box, BorderLayout.PAGE_START);
        // pane.add(showButton, BorderLayout.PAGE_END);
        return pane;
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
				Log.info("Pressed " + commandsList.getSelectedItem());
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

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
				ReceptionTerminal rT = new ReceptionTerminal();
				rT.pack();
				rT.setVisible(true);
            }
        });
    }
}