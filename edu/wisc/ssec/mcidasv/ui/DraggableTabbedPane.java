package edu.wisc.ssec.mcidasv.ui;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JTabbedPane;

import ucar.unidata.ui.ComponentHolder;

/**
 * This is a rather simplistic drag and drop enabled JTabbedPane. It allows
 * users to use drag and drop to move tabs between windows and reorder tabs.
 * 
 * Jeff's work in DnDTree was a great inspiration. :)
 */
public class DraggableTabbedPane extends JTabbedPane implements DragGestureListener, 
	DragSourceListener, DropTargetListener {

	/** Local shorthand for the actions we're accepting. */
	private static final int VALID_ACTION = DnDConstants.ACTION_COPY_OR_MOVE;

	/** Path to the icon we'll use as an index indicator. */
	private static final String IDX_ICON = 
		"/edu/wisc/ssec/mcidasv/resources/icons/tabmenu/go-down.png";

	/** 
	 * Used to signal across all DraggableTabbedPanes that the component 
	 * currently being dragged originated in another window. This'll let McV
	 * determine if it has to do a quiet ComponentHolder transfer.
	 */
	protected static boolean outsideDrag = false;

	/** The actual image that we'll use to display the index indications. */
	private final Image INDICATOR = 
		(new ImageIcon(getClass().getResource(IDX_ICON))).getImage();

	/** The tab index where the drag started. */
	private int sourceIndex = -1;

	/** The tab index that the user is currently over. */
	private int overIndex = -1;

	/** Used for starting the dragging process. */
	private DragSource dragSource;

	/** Used for signaling that we'll accept drops (registers listeners). */
	private DropTarget dropTarget;

	/** The component group holding our components. */
	private McIDASVComponentGroup group;

	/**
	 * Mostly just registers that this component should listen for drag and
	 * drop operations.
	 * 
	 * @param group The <tt>ComponentGroup</tt> that holds this component's tabs.
	 */
	public DraggableTabbedPane(McIDASVComponentGroup group) {
		dropTarget = new DropTarget(this, this);
		dragSource = new DragSource();
		dragSource.createDefaultDragGestureRecognizer(this, VALID_ACTION, this);

		this.group = group;
	}

	/**
	 * Triggered when the user does a (platform-dependent) drag initiating 
	 * gesture. Used to populate the things that the user is attempting to 
	 * drag. 
	 */
	public void dragGestureRecognized(DragGestureEvent e) {
		sourceIndex = getSelectedIndex();

		// transferable allows us to store the current DraggableTabbedPane and
		// the source index of the drag inside the various drag and drop event
		// listeners.
		Transferable transferable = new TransferableIndex(this, sourceIndex);

		Cursor cursor = DragSource.DefaultMoveDrop;
		if (e.getDragAction() != DnDConstants.ACTION_MOVE)
			cursor = DragSource.DefaultCopyDrop;

		dragSource.startDrag(e, cursor, transferable, this);
	}

	/** 
	 * Triggered when the user drags into <tt>dropTarget</tt>.
	 */
	public void dragEnter(DropTargetDragEvent e) {
		DataFlavor[] flave = e.getCurrentDataFlavors();
		if ((flave.length == 0) || !(flave[0] instanceof DraggableTabFlavor))
			return;

		//System.out.print("entered window outsideDrag=" + outsideDrag + " sourceIndex=" + sourceIndex);

		// if the DraggableTabbedPane associated with this drag isn't the 
		// "current" DraggableTabbedPane we're dealing with a drag from another
		// window and we need to make this DraggableTabbedPane aware of that.
		if (((DraggableTabFlavor)flave[0]).getDragTab() != this) {
			//System.out.println(" coming from outside!");
			outsideDrag = true;
		} else {
			//System.out.println(" re-entered parent window");
			outsideDrag = false;
		}
	}

	/**
	 * Triggered when the user drags out of <tt>dropTarget</tt>.
	 */
	public void dragExit(DropTargetEvent e) {
		//System.out.println("drag left a window outsideDrag=" + outsideDrag + " sourceIndex=" + sourceIndex);
		overIndex = -1;
		repaint();
	}

	/**
	 * Triggered continually while the user is dragging over 
	 * <tt>dropTarget</tt>. McIDAS-V uses this to draw the index indicator.
	 * 
	 * @param e Information about the current state of the drag.
	 */
	public void dragOver(DropTargetDragEvent e) {
		if ((!outsideDrag) && (sourceIndex == -1))
			return;

		Point dropPoint = e.getLocation();
		overIndex = indexAtLocation(dropPoint.x, dropPoint.y);

		repaint();
	}

	/**
	 * Triggered when a drop has happened over <tt>dropTarget</tt>.
	 * 
	 * @param e State that we'll need in order to handle the drop.
	 */
	public void drop(DropTargetDropEvent e) {
		// if the dragged ComponentHolder was dragged from another window we
		// must do a behind-the-scenes transfer from its old ComponentGroup to 
		// the end of the new ComponentGroup.
		if (outsideDrag) {
			DataFlavor[] flave = e.getCurrentDataFlavors();
			DraggableTabbedPane other = ((DraggableTabFlavor)flave[0]).getDragTab();

			ComponentHolder target = other.removeDragged();
			sourceIndex = group.quietAddComponent(target);
			outsideDrag = false;
		}

		// check to see if we've actually dropped something McV understands.
		if (sourceIndex >= 0) {
			e.acceptDrop(VALID_ACTION);
			Point dropPoint = e.getLocation();
			int dropIndex = indexAtLocation(dropPoint.x, dropPoint.y);

			// make sure the user chose to drop over a valid area/thing first
			// then do the actual drop.
			if ((dropIndex != -1) && (getComponentAt(dropIndex) != null))
				doDrop(sourceIndex, dropIndex);

			// clean up anything associated with the current drag and drop
			e.getDropTargetContext().dropComplete(true);
			sourceIndex = -1;
			overIndex = -1;

			repaint();
		}
	}

	/**
	 * &quot;Quietly&quot; removes the dragged component from its group.
	 * 
	 * @return The removed component.
	 */
	private ComponentHolder removeDragged() {
		return group.quietRemoveComponentAt(sourceIndex);
	}

	/**
	 * Moves a component to its new index within the component group.
	 * 
	 * @param srcIdx The old index of the component.
	 * @param dstIdx The new index of the component.
	 */
	public void doDrop(int srcIdx, int dstIdx) {
		List<ComponentHolder> comps = group.getDisplayComponents();
		ComponentHolder src = comps.get(srcIdx);

		group.removeComponent(src);
		group.addComponent(src, dstIdx);
	}

	/**
	 * Overridden so that McV can draw an indicator of a dragged tab's possible
	 * new position.
	 */
	@Override
	public void paint(Graphics g) {
		super.paint(g);

		if (overIndex == -1)
			return;

		Rectangle bounds = getBoundsAt(overIndex);

		if (bounds != null)
			g.drawImage(INDICATOR, bounds.x-7, bounds.y, null);
	}

	/**
	 * Used to simply provide a reference to the originating 
	 * DraggableTabbedPane while we're dragging and dropping.
	 */
	private static class TransferableIndex implements Transferable {
		private DraggableTabbedPane tabbedPane;

		private int index;

		public TransferableIndex(DraggableTabbedPane dt, int i) {
			tabbedPane = dt;
			index = i;
		}

		// whatever is returned here needs to be serializable. so we can't just
		// return the tabbedPane. :(
		public Object getTransferData(DataFlavor flavor) {
			return index;
		}

		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] { new DraggableTabFlavor(tabbedPane) };
		}

		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return true;
		}
	}

	/**
	 * To be perfectly honest I'm still a bit fuzzy about DataFlavors. As far 
	 * as I can tell they're used like so: if a user dragged an image file on
	 * to a toolbar, the toolbar might be smart enough to add the image. If the
	 * user dragged the same image file into a text document, the text editor
	 * might be smart enough to insert the path to the image or something.
	 * 
	 * I'm thinking that would require two data flavors: some sort of toolbar
	 * flavor and then some sort of text flavor?
	 */
	private static class DraggableTabFlavor extends DataFlavor {
		private DraggableTabbedPane tabbedPane;

		public DraggableTabFlavor(DraggableTabbedPane dt) {
			super(DraggableTabbedPane.class, "DraggableTabbedPane");
			tabbedPane = dt;
		}

		public DraggableTabbedPane getDragTab() {
			return tabbedPane;
		}
	}

	// required methods that we don't need to implement yet.
	public void dragDropEnd(DragSourceDropEvent e) {}
	public void dragEnter(DragSourceDragEvent e) {
		//System.out.println("other dragEnter outsideDrag=" + outsideDrag + " sourceIndex=" + sourceIndex);
	}
	public void dragExit(DragSourceEvent e) {}
	public void dragOver(DragSourceDragEvent e) {}
	public void dropActionChanged(DragSourceDragEvent e) {}
	public void dropActionChanged(DropTargetDragEvent e) {}
}
