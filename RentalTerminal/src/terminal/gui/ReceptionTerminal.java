package terminal.gui;


import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.JFrame;

import terminal.commands.CardCommunication;
import terminal.commands.PersonalizationCommands;
import terminal.commands.ReceptionCommands;
import terminal.utils.Log;
import terminal.crypto.ECCKeyGenerator;

import java.awt.*;
import java.awt.event.*;
import java.security.interfaces.ECPublicKey;

public class ReceptionTerminal extends JFrame implements TerminalInterface {
	private static final long serialVersionUID = -3660099088414835331L;

	static final String TITLE = "ReceptionTerminal";
	
	JComboBox commandsList;
	JButton initialize;
	JButton launchCommand;
	JButton cardSetup;
	
	private CardCommunication comm;
	JFrame frame = this;
        
    public ReceptionTerminal() {
    	comm = new CardCommunication(this);
    	initUI();
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
                          "Commands panel"); // tooltip text
        tabbedPane.addTab("Setup", null,
                		  setupPanel,
                		  "Setup panel"); // tooltip text
        
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

        cardSetup = new JButton("Setup card");
        cardSetup.setEnabled(false);
        box.add(cardSetup);
        box.add(Box.createRigidArea(new Dimension(0,10)));
        
        cardSetup.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent action) {
				PersonalizationCommands cmds = new PersonalizationCommands(comm);
				try {
					cmds.doIssuance();
				} catch (Exception e) {
					Log.error(e.getMessage());
				}
			}
		});

        JPanel pane = new JPanel(new BorderLayout());
        pane.add(box, BorderLayout.PAGE_START);
        //pane.add(showButton, BorderLayout.PAGE_END);
        return pane;
    }

    /** Creates the panel shown by the second tab. */
    private JPanel createCommandsPanel() {        
        // Possible operations
        final String CHECK_INUSE = "Check InUse flag";
        final String ADD_VEHICLE = "Add certificate for vehicle";
        final String REMOVE_VEHICLE = "Remove certificate from card";
        final String GET_KM = "Get kilometer counting";
        final String RESET_KM = "Reset kilometer counting";

        String[] commands = {CHECK_INUSE, ADD_VEHICLE, REMOVE_VEHICLE, GET_KM, RESET_KM};
        
        commandsList = new JComboBox(commands);

        JPanel cmdBox = new JPanel();
        cmdBox.setLayout(new BoxLayout(cmdBox, BoxLayout.X_AXIS));
      
        initialize = new JButton("Initialize");
        launchCommand = new JButton("Launch command");

        launchCommand.setEnabled(false);
        commandsList.setEnabled(false);
        initialize.setEnabled(false);
        
        cmdBox.add(initialize);
        cmdBox.add(Box.createRigidArea(new Dimension(40, 0)));
        
		final ReceptionCommands receptionCmds = new ReceptionCommands(comm);
        initialize.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent action) {
				try {
					receptionCmds.sendInitNonce();
				} catch (Exception e) {
					Log.error("Error: " + e.getMessage());
					e.printStackTrace();
					return;
				}
				
		        launchCommand.setEnabled(true);
		        commandsList.setEnabled(true);
		        initialize.setEnabled(false);
			}
		});
        
        cmdBox.add(commandsList);
        cmdBox.add(Box.createRigidArea(new Dimension(20, 0)));
        cmdBox.add(launchCommand);
        
        launchCommand.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent action) {
				if (commandsList.getSelectedItem().toString().equals(GET_KM)) {
					try {
						long kilometerOnCard = receptionCmds.getKilometers();
						Log.info("Read out "+kilometerOnCard+" driven kilometers.");
					} catch (Exception e) {
						Log.error("Could not read out kilometers");
						e.printStackTrace();
					}
				}
				if (commandsList.getSelectedItem().toString().equals(RESET_KM)) {
					try {
						receptionCmds.resetKilometers();
						Log.info("Kilometers on card were reset");
					} catch (Exception e) {
						Log.error("Could not reset kilometers");
						e.printStackTrace();
					}
				}
				if (commandsList.getSelectedItem().toString().equals(CHECK_INUSE)) {
					try {
						boolean inUseFlag = receptionCmds.checkInUseFlag();
						Log.info("InUseFlag on card is set to: "+ inUseFlag);
					} catch (Exception e) {
						Log.error("Could not check inUseFlag");
						e.printStackTrace();
					}
				}
				if (commandsList.getSelectedItem().toString().equals(ADD_VEHICLE)) {
					try {
						Object[] cars = {"car0", "car1", "car2", "car3", "car4", "car5"};
						String carID = (String) JOptionPane.showInputDialog(
							frame,
							"Please choose a vehicle",
						    "Choose vehicle",
						    JOptionPane.PLAIN_MESSAGE,
						    null,
						    cars,
						    cars[0]
						);

						ECPublicKey key = (ECPublicKey) ECCKeyGenerator.loadPublicKey("keys/cars", carID);
						receptionCmds.addVehicleCert(key);
						Log.info("The card is now associated with the vehicle");
					} catch (Exception e) {
						Log.error("Could not associate card with vehicle");
						e.printStackTrace();
					}
				}
				if (commandsList.getSelectedItem().toString().equals(REMOVE_VEHICLE)) {
					try {
						receptionCmds.deleteVehicleCert();
						Log.info("Vehicle de-associated with card");
					} catch (Exception e) {
						Log.error("Could not de-associate vehicle from card");
						e.printStackTrace();
					}
				}
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
        //pane.add(launchCommand, BorderLayout.PAGE_END);
        return pane;
    }
    
	@Override
	public void cardInserted() {
		launchCommand.setEnabled(false);
        commandsList.setEnabled(false);
        initialize.setEnabled(true);
        cardSetup.setEnabled(true);
	}

	@Override
	public void cardRemoved() {
		launchCommand.setEnabled(false);
        commandsList.setEnabled(false);
        initialize.setEnabled(false);
        cardSetup.setEnabled(false);
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