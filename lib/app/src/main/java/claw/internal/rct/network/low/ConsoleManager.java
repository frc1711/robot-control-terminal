package claw.internal.rct.network.low;

/**
 * An interface for managing input from and output to the driverstation console.
 */
public interface ConsoleManager {
    
    /**
     * Read a single line of input from the console.
     */
    String readInputLine ();
    
    /**
     * Returns {@code true} if there is user input waiting to be processed by the console.
     * This is useful for detecting if a user has hit enter in order to exit some continuously running
     * command.
     */
    boolean hasInputReady ();
    
    /**
     * Clear any submitted user input that is currently waiting.
     * This is useful to call so that input entered by the
     * user does not affect any input lines read long after.
     */
    void clearWaitingInputLines ();
    
    /**
     * Move up a given number of rows in the console (also moving to the first column).
     */
    void moveUp (int lines);
    
    /**
     * Clear all text in the current row in the console.
     */
    void clearLine ();
    
    /**
     * Save the cursor position so that it can be restored later with {@link ConsoleManager#restoreCursorPos()}.
     */
    void saveCursorPos ();
    
    /**
     * Restore the cursor position from the last {@link ConsoleManager#saveCursorPos()} call.
     */
    void restoreCursorPos ();
    
    /**
     * Print white text to the console with no newline.
     */
    void print (String msg);
    
    /**
     * Print white text to the console with a trailing newline.
     */
    default void println (String msg) {
        print(msg + "\n");
    }
    
    /**
     * Print red text to the console with no newline. This is not the same
     * as printing to {@code System.err}.
     */
    void printErr (String msg);
    
    /**
     * Print red text to the console with a trailing newline. This is not the same
     * as printing to {@code System.err}.
     */
    default void printlnErr (String msg) {
        printErr(msg + "\n");
    }
    
    /**
     * Print yellow/orange system text to the console with no newline.
     */
    void printSys (String msg);
    
    /**
     * Print yellow/orange system text to the console with a trailing newline.
     */
    default void printlnSys (String msg) {
        printSys(msg + "\n");
    }
    
    /**
     * Flush the console output.
     */
    void flush ();
    
    /**
     * Clear the console of all output.
     */
    void clear ();
    
}
