package edu.wisc.ssec.mcidasv.startupmanager;

import edu.wisc.ssec.mcidasv.ui.persistbox.*;
 
import java.awt.*;

import java.awt.event.*;

import javax.swing.*;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;

import java.net.URL;
import java.util.*;

/**
 * The <code>StartupManager</code> class implements a relatively simple Swing
 * GUI utility that allows users to easily manipulate their McIDAS-V or IDV
 * installations. Rather than have to write execution aliases and so on, the
 * user can simply edit the current settings, click save, and start either
 * McIDAS-V or IDV as they normally would.
 * 
 * Forthcoming features: 
 * - save off preferences to profiles
 * - be able to download these profiles from remote servers (perhaps pinging to see if any updates are needed?).
 * - replacing hardcoded strings with something read from a config xml file (easy multi-lingual support) or maybe a properties file?
 * 
 * TODO: offload some stuff into separate threads
 * TODO: for OS X either pick a slightly larger window size or turn off resizing
 *       to fix that resizer box issue.
 * 
 * @author Jonathan Beavers, SSEC
 */
public class StartupManager implements ListSelectionListener {

	private Hashtable actionToComponent = new Hashtable();
	
	// exception messages
	/** Exception message for dealing with bad ListItem types. */
	protected static final String EMSG_BAD_ITEM_TYPE = "";
	
	/** 
	 * Exception message for dealing with attempting to find an index for an
	 * unknown column name. 
	 */
	protected static final String EMSG_BAD_COLUMN_NAME = "";
	
	/** Exception message for dealing with null ListItems. */
	protected static final String EMSG_NULL_LIST_ITEM = "";
	// end exception messages
	
	// platform identification strings	
	/** */
	private final String WINDOWS_ID = "Windows";
	
	/** */
	private final String MACOSX_ID = "Mac OS X";	
	// end platform ids.
	
	// handy gui pointers
	/** Reference to command row of buttons along the bottom of the panel */
	private CommandRow commandRow;
	
	private OptionPanel optionPanel;
	private JSplitPane splitPane;
	private JList list;
	private DefaultListModel listModel;
	private JScrollPane listScrollPane;
	
	/** Main frame */
	private JFrame frame;
	
	/** A friendly reference to the current instance. */
	private StartupManager manager = this;
	// end handy gui pointers
	
	
	private Object[][] listItems = {
		{new JavaOptions(this), "Java VM", "java.png"},
		{new PluginOptions(this), "IDV Plugins", "plugins.png"},
		{new BundleOptions(this), "IDV Bundles", "bundles.png"},
		{new BatchOptions(this), "Batch Processing", "batch.png"},
		{new NetworkingOptions(this), "Networking", "network.png"},
		{new MiscOptions(this), "McIDAS X Options", "mcidas.png"},
		{new McidasXOptions(this), "Miscellaneous", "misc.png"},
	};

	// TODO: be sure to include trailing "/".
	private static final String ICON_PATH = 
		"/edu/wisc/ssec/mcidasv/startupmanager/resources/icons/";
	
	/** Whether or not determinePlatform() found Windows */
	protected boolean isWindows = false;
	
	/** Whether or not determinePlatform() found OS X */
	protected boolean isMac = false;
	
	/** Whether or not determinePlatform() found Unix */
	protected boolean isUnix = false;
	
	/**
	 * Initialize the startup manager, which largely consists of attempting
	 * to figure out which platform we're using (for the time being).
	 */
	public StartupManager() {
		try {
			determinePlatform();
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Polls the various panels and so on to figure out what options need to
	 * be written to the script that starts McV/IDV.
	 * 
	 * @param path The path of the file that will be created/overwritten.
	 */
	public void writeScript(String path) {
		// each panel should have a method like getFlags() that returns a 
		// string with each flag and any parameters. 
		// java options must be placed differently than McV/IDV options.
		String flags = new String("");
		for (int i = 0; i < listItems.length; i++) {
			flags += ((OptionPanel)listItems[i][0]).getFlags();
		}
		System.out.println(flags);
	}
	
	/**
	 * Initializes the tabbed display panel and all of the child widgets, and
	 * then displays everything to the screen. 
	 */
	public void createDisplay() {
		createListGUI();
		
		frame = new JFrame("Startup Manager");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		//frame.getContentPane().add(listScrollPane, BorderLayout.WEST);
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setLeftComponent(listScrollPane);
		splitPane.setRightComponent(getSelectedPanel());
		frame.getContentPane().add(splitPane, BorderLayout.NORTH);
		frame.getContentPane().add(commandRow.getPanel(), BorderLayout.SOUTH);
		frame.pack();
		frame.setVisible(true);
	}
	
	private void createListGUI() {
		commandRow = new CommandRow(this);
		
		listModel = new DefaultListModel();
		
		for (int i = 0; i < listItems.length; i++) {
			// prep the panel for display
			((OptionPanel)listItems[i][0]).createPanel();
			
			// prep the associated text+icon for display in the JList.
			String text = (String)listItems[i][1];
			String icon = ICON_PATH + (String)listItems[i][2];			
			URL tmp = getClass().getResource(icon);
			JLabel label = new JLabel();
			label.setText(text);
			label.setIcon(new ImageIcon(tmp));
			
			// good to go!
			listModel.addElement(label);
		}
		
		list = new JList(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setSelectedIndex(0);
		list.addListSelectionListener(this);
		list.setVisibleRowCount(listItems.length);
		list.setCellRenderer(new IconCellRenderer());
		listScrollPane = new JScrollPane(list);		
	}
		
	public void valueChanged(ListSelectionEvent e) {
		System.err.println(e);
	}
	
	private OptionPanel getSelectedPanel() {
		int index = list.getSelectedIndex();
		return (OptionPanel)listItems[index][0];
	}
	
	/**
	 * Attempt to identify the OS that we're currently running on. Currently
	 * I only identify "Unix", OS X, and Windows. This method examines the Java
	 * os.name property, and if it finds either Windows or OS X, it'll set 
	 * either isWindows or isMac respectively. If neither Windows or OS X match,
	 * I set isUnix to true. 
	 * 
	 * This behavior is stupid.
	 */
	private void determinePlatform() {
		String os = System.getProperty("os.name");
		
		if (os == null)
			throw new RuntimeException();
		
		if (os.startsWith(WINDOWS_ID))
			isWindows = true;
		else if (os.startsWith(MACOSX_ID))
			isMac = false;
		else
			isUnix = false;
	}
	
	/**
	 * TODO: a pretty huge set of functionality.
	 */
	public void saveState() {
		System.out.println("todo!");
	}
	
	/**
	 * Loop through all of the tabs and call 
	 * <code>toggleAdvancedOptions</code> for each of them. 
	 */
	public void toggleAdvancedOptions() {
		for (int i = 0; i < listItems.length; i++)
			((OptionPanel)listItems[i][0]).toggleAdvancedOptions();
	}
	
	/**
	 * 
	 * 
	 * @param args things and stuff
	 */	
	public static void main(String[] args) {
		// default to Metal
		String laf = UIManager.getCrossPlatformLookAndFeelClassName();
		try {
			UIManager.setLookAndFeel(laf);
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		StartupManager mngr = new StartupManager();
		mngr.createDisplay();
	}
	
	public class IconCellRenderer extends DefaultListCellRenderer {
		
		/**
		 * Extends the default list cell renderer to use icons in addition to
		 * the typical text.
		 */
		public Component getListCellRendererComponent(JList list, Object value, 
				int index, boolean isSelected, boolean cellHasFocus) {
			
			super.getListCellRendererComponent(list, value, index, isSelected, 
					cellHasFocus);
			
			if (value instanceof JLabel) {
				setText(((JLabel)value).getText());
				setIcon(((JLabel)value).getIcon());
			}

			return this;
		}
		
		/** 
		 * I wear some pretty fancy pants, so you'd better believe that I'm
		 * going to enable fancy-pants text antialiasing.
		 * 
		 * @param g The graphics object that we'll use as a base.
		 */
		protected void paintComponent(Graphics g) {
			Graphics2D g2d = (Graphics2D)g;
			
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
					RenderingHints.VALUE_ANTIALIAS_ON);
			
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			
			g2d.setRenderingHint(RenderingHints.KEY_RENDERING, 
					RenderingHints.VALUE_RENDER_QUALITY);
			
			super.paintComponent(g2d);
		}
	}
}