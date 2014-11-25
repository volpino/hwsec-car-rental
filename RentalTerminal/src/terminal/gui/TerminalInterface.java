package terminal.gui;

/**
 * Interface for terminals.
 * cardInserted() and cardRemoved() methods are callbacks that are called when
 * the card is inserted or removed
 * 
 * @author Federico Scrinzi
 *
 */
public interface TerminalInterface {
	public void cardInserted();
	public void cardRemoved();
}
