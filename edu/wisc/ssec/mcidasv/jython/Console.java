package edu.wisc.ssec.mcidasv.jython;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.python.core.Py;
import org.python.core.PyFrame;
import org.python.core.PyJavaInstance;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyStringMap;
import org.python.core.PyTuple;
import org.python.core.__builtin__;
import org.python.util.InteractiveConsole;

public class Console implements Runnable, KeyListener {

    // TODO(jon): is this going to work on windows?
    private static final String PYTHON_HOME = "~/.mcidasv/jython";

    /** Offset array used when actual offsets cannot be determined. */
    private static final int[] BAD_OFFSETS = { -1, -1 };

    /** Color of the Jython text as it is being entered. */
    private static final Color TXT_NORMAL = Color.BLACK;

    /** Color of text coming from &quot;stdout&quot;. */
    private static final Color TXT_GOOD = Color.BLUE;

    /** Not used just yet... */
    private static final Color TXT_WARN = Color.ORANGE;

    /** Color of text coming from &quot;stderr&quot;. */
    private static final Color TXT_ERROR = Color.RED;

    /** Normal jython prompt. */
    private static final String PS1 = ">>> ";

    /** Prompt that indicates more input is needed. */
    private static final String PS2 = "... ";

    /** Not used yet. */
    private static final String BANNER = InteractiveConsole.getDefaultBanner();

    /** All text will appear in this font. */
    private static final Font FONT = new Font("Monospaced", Font.PLAIN, 14);

    /** Thread that handles Jython command execution. */
    private Runner jythonRunner;

    /** A hook that allows external classes to respond to events. */
    private ConsoleCallback callback;

    /** Where the user interacts with the Jython interpreter. */
    private JTextPane textPane;

    /** {@link #textPane}'s internal representation. */
    private Document document;

    /** Panel that holds {@link #textPane}. */
    private JPanel panel;
    
    private String windowTitle = "Super Happy Jython Fun Console";

    public Console() {
        this(Collections.<String>emptyList());
    }

    public Console(final List<String> initialCommands) {
        if (initialCommands == null)
            throw new NullPointerException("List of initial commands cannot be null");
        jythonRunner = new Runner(initialCommands);
        jythonRunner.start();
        panel = new JPanel(new BorderLayout());
        textPane = new JTextPane();
        document = textPane.getDocument();
        panel.add(BorderLayout.CENTER, new JScrollPane(textPane));
        setCallbackHandler(new DummyCallbackHandler());
        try {
            showBanner(); 
            document.createPosition(document.getLength() - 1);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        new EndAction(this, Actions.END);
        new EnterAction(this, Actions.ENTER);
        new DeleteAction(this, Actions.DELETE);
        new HomeAction(this, Actions.HOME);
//        new UpAction(this, Actions.UP);
//        new DownAction(this, Actions.DOWN);

        JTextComponent.addKeymap("jython", textPane.getKeymap());

        textPane.setFont(FONT);
        textPane.addKeyListener(this);
    }

    /**
     * Returns the panel containing the various UI components.
     */
    public JPanel getPanel() {
        return panel;
    }

    /** 
     * Returns the {@link JTextPane} used by the console.
     */
    protected JTextPane getTextPane() {
        return textPane;
    }

    /**
     * Inserts the specified object into Jython's local namespace using the
     * specified name.
     * 
     * <p><b>Example:</b><br/> 
     * {@code console.injectObject("test", new PyJavaInstance("a test"))}<br/>
     * Allows the interpreter to refer to the {@link String} {@code "a test"}
     * as {@code test}.
     * 
     * @param name Object name as it will appear within the interpreter.
     * @param pyObject Object to place in the interpreter's local namespace.
     */
    public void injectObject(final String name, final PyObject pyObject) {
        jythonRunner.queueObject(this, name, pyObject);
    }

    public void eval(final String jython) {
        jythonRunner.queueEval(this, jython);
    }

    /**
     * Runs the file specified by {@code path} in the {@link Interpreter}.
     * 
     * @param name {@code __name__} attribute to use for loading {@code path}.
     * @param path The path to the Jython file.
     */
    public void runFile(final String name, final String path) {
        jythonRunner.queueFile(this, name, path);
    }

    /**
     * Displays non-error output.
     * 
     * @param text The message to display.
     */
    public void result(final String text) {
        insert(TXT_GOOD, "\n" + text);
    }

    /**
     * Displays an error.
     * 
     * @param text The error message.
     */
    public void error(final String text) {
        insert(TXT_ERROR, "\n" + text);
    }

    /**
     * Shows the normal Jython prompt.
     */
    public void prompt() {
        insert(TXT_NORMAL, "\n" + PS1);
    }

    /**
     * Shows the prompt that indicates more input is needed.
     */
    public void moreInput() {
        insert(TXT_NORMAL, "\n" + PS2);
    }

    /**
     * Will eventually display an initial greeting to the user.
     * 
     * @throws BadLocationException Upon attempting to clear out an invalid 
     * portion of the document.
     */
    private void showBanner() throws BadLocationException {
        document.remove(0, document.getLength());
        prompt();
        textPane.requestFocus();
    }

    /**
     * Does the actual work of displaying color-coded messages in 
     * {@link #textPane}.
     * 
     * @param color The color of the message.
     * @param text The actual message.
     */
    private void insert(final Color color, final String text) {
        SimpleAttributeSet style = new SimpleAttributeSet();
        style.addAttribute(StyleConstants.Foreground, color);
        try {
            document.insertString(document.getLength(), text, style);
            textPane.setCaretPosition(document.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return Number of lines in the document.
     */
    public int getLineCount() {
        return document.getRootElements()[0].getElementCount();
    }

    // TODO(jon): Rethink some of these methods names, especially getLineOffsets and getOffsetLine!!

    public int getLineOffsetStart(final int lineNumber) {
        return document.getRootElements()[0].getElement(lineNumber).getStartOffset();
    }

    public int getLineOffsetEnd(final int lineNumber) {
        return document.getRootElements()[0].getElement(lineNumber).getEndOffset();
    }

    public int[] getLineOffsets(final int lineNumber) {
        if (lineNumber >= getLineCount())
            return BAD_OFFSETS;

        int start = getLineOffsetStart(lineNumber);
        int end = getLineOffsetEnd(lineNumber);
        return new int[] { start, end };
    }

    /**
     * Returns the line number that contains the specified offset.
     * 
     * @param offset Offset whose line number you want.
     * 
     * @return Line number.
     */
    public int getOffsetLine(final int offset) {
        return document.getRootElements()[0].getElementIndex(offset);
    }

    /**
     * Returns the offsets of the beginning and end of the last line.
     */
    private int[] locateLastLine() {
        return getLineOffsets(getLineCount() - 1);
    }

    /**
     * Determines whether or not the caret is on the last line.
     */
    private boolean onLastLine() {
        int[] offsets = locateLastLine();
        int position = textPane.getCaretPosition();
        return (position >= offsets[0] && position <= offsets[1]);
    }

    /**
     * @return The line number of the caret's offset within the text.
     */
    public int getCaretLine() {
        return getOffsetLine(textPane.getCaretPosition());
    }

    /**
     * Returns the line of text that occupies the specified line number.
     * 
     * @param lineNumber Line number whose text is to be returned.
     * 
     * @return Either the line of text or null if there was an error.
     */
    public String getLineText(final int lineNumber) {
        int start = getLineOffsetStart(lineNumber);
        int stop = getLineOffsetEnd(lineNumber);
        String line = null;
        try {
            line = document.getText(start, stop - start);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        return line;
    }

    /**
     * Returns the line of Jython that occupies a specified line number. 
     * This is different than {@link #getLineText(int)} in that both 
     * {@link #PS1} and {@link #PS2} are removed from the returned line.
     * 
     * @param lineNumber Line number whose text is to be returned.
     * 
     * @return Either the line of Jython or null if there was an error.
     */
    public String getLineJython(final int lineNumber) {
        String text = getLineText(lineNumber);
        if (text == null)
            return null;

        int start = getPromptLength(text);

        return text.substring(start, text.length() - 1);
    }

    /**
     * Returns the length of {@link #PS1} or {@link #PS2} depending on the 
     * contents of the specified line.
     * 
     * @param line The line in question.
     * 
     * @return Either the prompt length or zero if there was none.
     */
    public static int getPromptLength(final String line) {
        if (line.startsWith(PS1))
            return PS1.length();
        else if (line.startsWith(PS2))
            return PS2.length();
        else
            return 0;
    }

    /**
     * Registers a new callback handler with the console. Note that to maximize
     * utility, this method also registers the same handler with 
     * {@link #jythonRunner}.
     * 
     * @param newHandler The new callback handler.
     * 
     * @throws NullPointerException if the new handler is null.
     */
    public void setCallbackHandler(final ConsoleCallback newHandler) {
        if (newHandler == null)
            throw new NullPointerException("Callback handler cannot be null");

        jythonRunner.setCallbackHandler(newHandler);
    }

    /**
     * Returns a subset of Jython's local namespace containing only variables
     * that are {@literal "pure"} Java objects.
     * 
     * @return Jython variable names mapped to their Java instantiation.
     */
    public Map<String, Object> getJavaInstances() {
        Map<String, Object> javaMap = new HashMap<String, Object>();

        PyStringMap locals = jythonRunner.copyLocals();
        if (locals == null)
            return javaMap;

        PyList items = locals.items();
        for (int i = 0; i < items.__len__(); i++) {
            PyTuple tuple = (PyTuple)items.__finditem__(i);
            String key = ((PyString)tuple.__finditem__(0)).toString();
            PyObject val = tuple.__finditem__(1);
            if (val instanceof PyJavaInstance)
                javaMap.put(key, val.__tojava__(Object.class));
        }

        return javaMap;
    }
    
    // TODO: basically makes it so that when a user hits "home" the caret is
    // moved to just after PS1 or PS2, rather than the beginning of the line.
    public void handleHome() {
        String line = getLineText(getCaretLine());
        int[] offsets = getLineOffsets(getCaretLine());

        int linePosition = getPromptLength(line);

        textPane.setCaretPosition(offsets[0] + linePosition);
    }

    /**
     * Moves the caret to the end of the line it is currently on, rather than
     * the end of the document.
     */
    public void handleEnd() {
        int[] offsets = getLineOffsets(getCaretLine());
        textPane.setCaretPosition(offsets[1] - 1);
    }
    
    public void handleUp() {
        System.err.println("handleUp");
    }
    
    public void handleDown() {
        System.err.println("handleDown");
    }

    // TODO(jon): what about selected regions?
    // TODO(jon): what about multi lines?
    public void handleDelete() {
        if (!onLastLine())
            return;

        String line = getLineText(getCaretLine());
        if (line == null)
            return;

        int position = textPane.getCaretPosition();
        int start = getPromptLength(line);

        // don't let the user delete parts of PS1 or PS2
        int lineStart = getLineOffsetStart(getCaretLine());
        if (((position-1)-lineStart) < start)
            return;

        try {
            document.remove(position - 1, 1);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the user pressing enter by basically grabbing the line of jython
     * under the caret. If the caret is on the last line, the line is queued
     * for execution. Otherwise the line is reinserted at the end of the 
     * document--this lets the user preview a previous command before they 
     * rerun it.
     */
    // TODO(jon): if you hit enter at the start of a block, maybe it should
    // replicate the enter block at the end of the document?
    public void handleEnter() {
        String line = getLineJython(getCaretLine());
        if (line == null)
            line = "";

        if (onLastLine())
            jythonRunner.queueLine(this, line);
        else
            insert(TXT_NORMAL, line);
    }

    /**
     * Puts together the GUI once EventQueue has processed all other pending 
     * events.
     */
    public void run() {
        JFrame frame = new JFrame(windowTitle);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(getPanel());
        frame.getContentPane().setPreferredSize(new Dimension(600, 200));
        frame.pack();
        frame.setVisible(true);
    }

    public void keyPressed(final KeyEvent e) { }
    public void keyReleased(final KeyEvent e) { }

    // this is weird: hasAction is always false
    // seems to work so long as the ConsoleActions fire first...
    // might want to look at default actions again
    public void keyTyped(final KeyEvent e) {
//        System.err.println("keyTyped: hasAction=" + hasAction(textPane, e) + " key=" + e.getKeyChar());
        if (!hasAction(textPane, e) && !onLastLine()) {
//            System.err.println("keyTyped: hasAction=" + hasAction(textPane, e) + " lastLine=" + onLastLine());
            e.consume();
        }
    }

    private static boolean hasAction(final JTextPane jtp, final KeyEvent e) {
        assert jtp != null;
        assert e != null;
        KeyStroke stroke = 
            KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiers());
        return (jtp.getKeymap().getAction(stroke) != null);
    }

    public enum Actions {
        DELETE("jython.delete", KeyEvent.VK_BACK_SPACE, 0),
        END("jython.end", KeyEvent.VK_END, 0),
        ENTER("jython.enter", KeyEvent.VK_ENTER, 0),
        HOME("jython.home", KeyEvent.VK_HOME, 0),
//        UP("jython.up", KeyEvent.VK_UP, 0),
//        DOWN("jython.down", KeyEvent.VK_DOWN, 0);
        ;

        private final String id;
        private final int keyCode;
        private final int modifier;

        Actions(final String id, final int keyCode, final int modifier) {
            this.id = id;
            this.keyCode = keyCode;
            this.modifier = modifier;
        }

        public String getId() {
            return id;
        }

        public KeyStroke getKeyStroke() {
            return KeyStroke.getKeyStroke(keyCode, modifier);
        }
    }

    public static void main(String[] args) {
        Properties systemProperties = System.getProperties();
        Properties jythonProperties = new Properties();
        jythonProperties.setProperty("python.home", PYTHON_HOME);
        Interpreter.initialize(systemProperties, jythonProperties, new String[]{""});
        EventQueue.invokeLater(new Console());
//        EventQueue.invokeLater(new Console(Fun Time Console #2));
    }
}
