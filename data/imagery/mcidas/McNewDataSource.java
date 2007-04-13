package ucar.unidata.data.imagery.mcidas;

import edu.wisc.ssec.mcidas.*;

import java.awt.*;
import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Date;
import java.util.StringTokenizer;

import visad.*;

import java.rmi.RemoteException;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Trace;
import ucar.unidata.data.*;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.ui.colortable.ColorTableManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.DisplayControl;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.control.ImageSequenceControl;
import ucar.unidata.idv.control.mcidas.FrameComponentInfo;
import ucar.unidata.idv.control.mcidas.McIDASComponents;
import ucar.unidata.idv.control.mcidas.McNewImageSequenceControl;

import visad.Set;
import visad.georef.MapProjection;
import visad.java3d.*;
import visad.util.*;
import visad.data.mcidas.*;
import visad.meteorology.*;

/**
 * Used to cache  a data choice and its data
 *
 * @author IDV development team
 * @version $Revision$
 */


public class McNewDataSource extends DataSourceImpl  {

    public static String request;

    /** list of frames to load */
    private List frameNumbers = new ArrayList();

    /** list of frames */
    protected List frameList;

    /** list of twoD categories */          
    private List twoDCategories;  
                    
    /** list of 2D time series categories */
    private List twoDTimeSeriesCategories;

    /** image data arrays */
    private double values[][] = new double[1][1];
    static byte pixels[];
    static int lastFrameNo = 0;
    static McIDASFrame lastFrm = null;

    DisplayControlImpl dci;

    /**
     * Default bean constructor; does nothing
     */
    public McNewDataSource() {}


    /**
     * Create a McNewDataSource
     *
     *
     * @param descriptor the datasource descriptor
     * @param name my name
     * @param properties my properties
     */
    public McNewDataSource(DataSourceDescriptor descriptor, String name,
                            Hashtable properties) {
        super(descriptor, "McIDAS-X", "McIDAS-X", properties);
/*
        System.out.println("McNewDataSource:");
        System.out.println("    descriptor=" + descriptor);
        System.out.println("    name=" + name);
        System.out.println("    properties=" + properties);
*/
        if ((properties == null) || (properties.get("frame numbers") == null)) {
          List frames = new ArrayList();
          frames.add(new Integer(-1));
          properties.put("frame numbers", frames);
        }

        frameNumbers.clear();
        frameNumbers.add(properties.get("frame numbers"));

        List frames = new ArrayList();
        try {
            request = (String)properties.get("request");
            frames = (List)frameNumbers.get(0);
            setFrameList(makeFrameDescriptors(frames));

            Integer frmInt = (Integer)frames.get(0);
            int frmNo = frmInt.intValue();

        } catch (Exception e) {
            System.out.println("McNewDataSource e=" + e);
        }
    }


    /**
     * Make a list of frame descriptors
     *
     * @param frames  List of frame numbers
     *
     * @return ImageDataset
     */
    public List makeFrameDescriptors(List frames) {
        List descriptors = new ArrayList();
        Integer frmInt;
        int frmNo;
        for (int i = 0; i < frames.size(); i++) {
          frmInt = (Integer)frames.get(i);
          frmNo = frmInt.intValue();
          descriptors.add(new McIDASFrameDescriptor(frmNo, request));
        }
        return descriptors;
    }


    /** Get a list of DateTimes for a frame sequence 
     */
/*
    public List getDateTimes() {
      List selectedDateTimes = new ArrayList();
      for (int i=0; i<this.frameList.size(); i++) {
        selectedDateTimes.add(((McIDASFrameDescriptor)(this.frameList.get(i))).getDateTime());
      }
      return selectedDateTimes;
    }
*/

   
    /**
     * This is called after  this datasource has been fully created
     * and initialized after being unpersisted by the XmlEncoder.
     */
    public void initAfterUnpersistence() {
        super.initAfterUnpersistence();

        List frames = getFrame();
        setFrameList(makeFrameDescriptors(frames));
    }


    /**
     * Gets called after creation. Initialize the connection
     */
    public void initAfterCreation() {
        initConnection();
    }



    /**
     * Initialize the connection to McIDAS.
     * This gets called when the data source is newly created
     * or decoded form a bundle.
     */
    private void initConnection() {
      int istat = 0;

      if (istat < 0)
        setInError(true,"Unable to attach McIDAS-X shared memory");
    }

    protected boolean shouldCache(Data data) {
        return false;
    }


    /**
     *
     * @param dataChoice        The data choice that identifies the requested
     *                          data.
     * @param category          The data category of the request.
     * @param dataSelection     Identifies any subsetting of the data.
     * @param requestProperties Hashtable that holds any detailed request
     *                          properties.
     *
     * @return The data
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */

    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection, Hashtable requestProperties)
                                throws VisADException, RemoteException {
/*
        System.out.println("McNewDataSource getDataInner:");
        System.out.println("   dataChoice=" + dataChoice);
        System.out.println("   category=" + category);
        System.out.println("   dataSelection=" + dataSelection);
        System.out.println("   requestProperties=" + requestProperties);
*/
        FrameComponentInfo frameComponentInfo = new FrameComponentInfo();
        Boolean mc;
        mc = (Boolean)(requestProperties.get(McIDASComponents.IMAGE));
        if (mc == null)  mc=Boolean.TRUE; 
        if (mc.booleanValue()) {
          frameComponentInfo.isImage = true;
        } else {
          frameComponentInfo.isImage = false;
        }
        mc = (Boolean)(requestProperties.get(McIDASComponents.GRAPHICS));
        if (mc == null)  mc=Boolean.TRUE; 
        if (mc.booleanValue()) {
          frameComponentInfo.isGraphics = true;
        } else {
          frameComponentInfo.isGraphics = false;
        }
        mc = (Boolean)(requestProperties.get(McIDASComponents.COLORTABLE));
        if (mc == null)  mc=Boolean.TRUE; 
        if (mc.booleanValue()) {
          frameComponentInfo.isColorTable = true;
        } else {
          frameComponentInfo.isColorTable = false;
        }

        int frmNo;
        List frames = new ArrayList();
        List defList = null;
        frameNumbers.clear();
        frameNumbers.add((List)getProperty(ucar.unidata.ui.imagery.mcidas.FrameChooser.FRAME_NUMBERS_KEY, defList));
        frames = (List)frameNumbers.get(0);

        Data data=null;
        if (frames.size() < 2) {
          Integer frmInt = (Integer)frames.get(0);
          frmNo = frmInt.intValue();
          data = (Data) getMcIdasSequence(frmNo, frameComponentInfo);
        } else {
          String dc="";
          String fd="";
          for (int i=0; i<frames.size(); i++) {
            dc = dataChoice.toString();
            fd = (this.frameList.get(i)).toString();
            if (dc.compareTo(fd) == 0) {
              Integer frmInt = (Integer)frames.get(i);
              frmNo = frmInt.intValue();
              if (i > 0) {
                 frameComponentInfo.setIsColorTable(false);
              }
              data = (Data) getMcIdasSequence(frmNo, frameComponentInfo);
            }
          }
        }
        return data;
    }

    /**
     * make a time series from selected McIDAS-X frames
     */
    private SingleBandedImage getMcIdasSequence(int frmNo, FrameComponentInfo frameComponentInfo)
            throws VisADException, RemoteException {
/*
      System.out.println("McNewDataSource getMcIdasSequence:");
      System.out.println("   frmNo=" + frmNo);
      System.out.println("   frameComponentInfo=" + frameComponentInfo);
*/
      SingleBandedImage image = getMcIdasFrame(frmNo, frameComponentInfo);
      if (image != null) {
         if (shouldCache((Data)image)) {
            Integer fo = new Integer(frmNo);
            putCache(fo,image);
         }
      }
      return image;
    }


    private DisplayControlImpl getDisplayControlImpl() {
      dci = null;
      List dcl = getDataChangeListeners();
      if (dcl != null) {
        for (int i=0; i< dcl.size(); i++) {
          if (dcl.get(i) instanceof McNewImageSequenceControl) {
            dci= (DisplayControlImpl)(dcl.get(i));
            break;
          }
        }
      }
      return dci;
    }


    /**
     * Set the list of {@link AddeImageDescriptor}s that define this data
     * source.
     *
     * @param l The list of image descriptors.
     */
    public void setFrameList(List l) {
        this.frameList = l;
    }

    /**
     * Get frame numbers
     *
     * @return frame numbers 
     */
    public List getFrame() {

        List defList = null;
        List frameNumbers =
            (List)getProperty(ucar.unidata.ui.imagery.mcidas.FrameChooser.FRAME_NUMBERS_KEY, defList);
        return frameNumbers;
    }

    public List getFrameNumbers() {
        return frameNumbers;
    }

    /**
     * Get the name for the main data object
     *
     * @return name of main data object
     */
    public String getDataName() {

        String dataName =
            (String) getProperty(ucar.unidata.ui.imagery.mcidas.FrameChooser.DATA_NAME_KEY,
                                 "Frame Sequence");
        if (dataName.equals("")) {
            dataName = "Frame Sequence";
        }
        return dataName;
    }

    /**
     * Initialize the {@link ucar.unidata.data.DataCategory} objects that
     * this data source uses.
     */
    private void makeCategories() {
        twoDTimeSeriesCategories =
            DataCategory.parseCategories("MCNEW-IMAGE-2D;", false);
        twoDCategories = DataCategory.parseCategories("MCNEW-IMAGE-2D;", false);
    }

    /**
     * Return the list of {@link ucar.unidata.data.DataCategory} used for
     * single time step data.
     *
     * @return A list of categories.
     */
    public List getTwoDCategories() {
        if (twoDCategories == null) {
            makeCategories();
        }
        return twoDCategories;
    }

    /**
     * Return the list of {@link ucar.unidata.data.DataCategory} used for
     * multiple time step data.
     *
     * @return A list of categories.
     */

    public List getTwoDTimeSeriesCategories() {
        if (twoDCategories == null) {
            makeCategories();
        }
        return twoDTimeSeriesCategories;
    }


    /**
     * Create the set of {@link ucar.unidata.data.DataChoice} that represent
     * the data held by this data source.  We create one top-level
     * {@link ucar.unidata.data.CompositeDataChoice} that represents
     * all of the image time steps. We create a set of children
     * {@link ucar.unidata.data.DirectDataChoice}, one for each time step.
     */
    public void doMakeDataChoices() {
        if (this.frameList == null) return;
        CompositeDataChoice composite = new CompositeDataChoice(this,
                                            getFrame(), getName(),
                                            getDataName(),
                                            (this.frameList.size() > 1)
                                            ? getTwoDTimeSeriesCategories()
                                            : getTwoDCategories()) {
            public List getSelectedDateTimes() {
                return dataSource.getSelectedDateTimes();
            }
        };
        addDataChoice(composite);
        doMakeDataChoices(composite);
    }

    /**
     * Make the data choices and add them to the given composite
     *
     * @param composite The parent data choice to add to
     */
    private void doMakeDataChoices(CompositeDataChoice composite) {
        int cnt = 0;
        List frameNos = new ArrayList();
        List frameChoices = new ArrayList();

        for (Iterator iter = frameList.iterator(); iter.hasNext(); ) {
            Object              object     = iter.next();
            McIDASFrameDescriptor fd        = getDescriptor(object);
            String              name       = fd.toString();
            DataSelection       frameSelect = null;
            Integer frameNo = fd.getFrameNumber();
            if (frameNo != null) {
              frameNos.add(frameNo);
              //We will create the  data choice with an index, not with the actual frame number.
               frameSelect =
                   new DataSelection(Misc.newList(new Integer(cnt)));
            }
            frameSelect = null;
            DataChoice choice =
                new DirectDataChoice(this, new FrameDataInfo(cnt, fd),
                                     composite.getName(), name,
                                     getTwoDCategories(), frameSelect);
            cnt++;
            frameChoices.add(choice);
        }

        //Sort the data choices.
        composite.replaceDataChoices(sortChoices(frameChoices));
    }

    /**
     * Sort the list of data choices on their frame numbers 
     *
     * @param choices The data choices
     *
     * @return The data choices sorted
     */
    private List sortChoices(List choices) {
        Object[]   choicesArray = choices.toArray();
/*
        Comparator comp         = new Comparator() {
            public int compare(Object o1, Object o2) {
                McIDASFrameDescriptor fd1 = getDescriptor(o1);
                McIDASFrameDescriptor fd2 = getDescriptor(o2);
                return fd1.getFrameNumber().compareTo(fd2.getFrameNumber());
            }
        };
        Arrays.sort(choicesArray, comp);
*/
        return new ArrayList(Arrays.asList(choicesArray));

    }

    /**
     * A utility method that helps us deal with legacy bundles that used to
     * have String file names as the id of a data choice.
     *
     * @param object     May be an AddeImageDescriptor (for new bundles) or a
     *                   String that is converted to an image descriptor.
     * @return The image descriptor.
     */
    private McIDASFrameDescriptor getDescriptor(Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof DataChoice) {
            object = ((DataChoice) object).getId();
        }
        if (object instanceof FrameDataInfo) {
            int index = ((FrameDataInfo) object).getIndex();
            List                choices = getDataChoices();
            CompositeDataChoice cdc = (CompositeDataChoice) choices.get(0);
            if (index < cdc.getDataChoices().size()) {
                DataChoice dc = (DataChoice) cdc.getDataChoices().get(index);
                Object     tmpObject = dc.getId();
                if (tmpObject instanceof FrameDataInfo) {
                    return ((FrameDataInfo) tmpObject).getFd();
                }
            }
            return ((FrameDataInfo) object).getFd();
        }

        if (object instanceof McIDASFrameDescriptor) {
            return (McIDASFrameDescriptor) object;
        }
        return new McIDASFrameDescriptor();
    }

    /**
     * Class FrameDataInfo Holds an index and an McIDASFrameDescriptor
     */
    public class FrameDataInfo {

        /** The index */
        private int index;

        /** The FD */
        private McIDASFrameDescriptor fd;



        /**
         * Ctor for xml encoding
         */
        public FrameDataInfo() {}

        /**
         * CTOR
         *
         * @param index The index
         * @param fd The fd
         */
        public FrameDataInfo(int index, McIDASFrameDescriptor fd) {
            this.index = index;
            this.fd   = fd;
        }

        /**
         * Get the index
         *
         * @return The index
         */
        public int getIndex() {
            return index;
        }

        /**
         * Set the index
         *
         * @param v The index
         */
        public void setIndex(int v) {
            index = v;
        }

        /**
         * Get the descriptor
         *
         * @return The descriptor
         */
        public McIDASFrameDescriptor getFd() {
            return fd;
        }

        /**
         * Set the descriptor
         *
         * @param v The descriptor
         */
        public void setFd(McIDASFrameDescriptor v) {
            fd = v;
        }

        /**
         * toString
         *
         * @return toString
         */
        public String toString() {
            return "index:" + index + " " + fd;
        }

    }


    public SingleBandedImage getMcIdasFrame(int frameNumber, FrameComponentInfo frameComponentInfo)
           throws VisADException, RemoteException {
/*
        System.out.println("McNewDataSource getMcIdasFrame:");
        System.out.println("   frameNumber=" + frameNumber);
        System.out.println("   frameComponentInfo=" + frameComponentInfo);
*/
        FlatField image_data = null;
        SingleBandedImage field = null;

        if (frameNumber < 1) return field;
        McIDASFrame frm = new McIDASFrame(frameNumber, request);
        int frameNo = frm.getFrameNumber();

        FrameDirectory fd = frm.getFrameDirectory();
        if (fd.nav[0] == 0) return field;
        Date nominal_time = fd.getNominalTime();

        int height = frm.getLineSize();
        if (height < 0) return field;
        int width = frm.getElementSize();
        if (width < 0) return field;

        DataContext dataContext = getDataContext();
        ColorTableManager colorTableManager = ((IntegratedDataViewer)dataContext).getColorTableManager();
        List dcl = ((IntegratedDataViewer)dataContext).getDisplayControls();
        DisplayControlImpl dc = null;
        for (int i=dcl.size()-1; i>=0; i--) {
            DisplayControlImpl dci = (DisplayControlImpl)dcl.get(i);
            if (dci instanceof ImageSequenceControl) {
                dc = dci;
                break;
            }
        }

/* check for frameComponentInfo.isColorTable == true */

//        if (frameComponentInfo.isColorTable) {
             frameComponentInfo.isColorTable = false;
             ColorTable mcidasXColorTable = 
                 new ColorTable("MCIDAS-X",ColorTable.CATEGORY_BASIC,
                 frm.getEnhancementTable());
             colorTableManager.addUsers(mcidasXColorTable);
             dc.setColorTable("default", mcidasXColorTable);
//        }

/* check for frameComponentInfo.isImage == true */

        values = new double[1][height*width];
//        if (frameComponentInfo.isImage) {
            frameComponentInfo.isImage = false;
            byte[] img = new byte[height*width];
            img = frm.getFrameData();
            byte[] pixels = new byte[height*width];

            for (int i=0; i<height; i++) {
                if (i > (height-12)) {
                    for (int j=0; j<width; j++) {
                        pixels[i*width + j] = 0;
                    }
                } else {
                    for (int j=0; j<width; j++) {
                        pixels[i*width + j] = img[(height-i-1)*width + j];
                    }
                }
            }

            for (int i=0; i<height*width; i++) {
                values[0][i] = (double)pixels[i];
                if (values[0][i] < 0.0 ) values[0][i] += 256.0;
            }
//        }

  // fake an area directory
        int[] adir = new int[64];
        adir[5] = fd.uLLine;
        adir[6] = fd.uLEle;
        adir[8] = height;
        adir[9] = width;
        adir[11] = fd.lineRes;
        adir[12] = fd.eleRes;

        AREACoordinateSystem cs;
        try {
            cs = new AREACoordinateSystem( adir, fd.nav, fd.aux);
        } catch (Exception e) {
            System.out.println("AREACoordinateSystem e=" + e);
            return field;
        }
        AREAnav ng = new GVARnav(fd.nav);
        double uLine = (double)adir[5];
        double uEle = (double)adir[6];

/* check for frameComponentInfo.isGraphics == true */

        double[] segment = new double[5];
//        if (frameComponentInfo.isGraphics) {
            frameComponentInfo.isGraphics = false;
            List graphics = frm.getGraphicsData();
            for (int i=0; i<graphics.size(); i++) {
                String line = (String)(graphics.get(i));
                StringTokenizer tok = new StringTokenizer(line);
                for (int j=0; j<5; j++) {
                    segment[j] = new Double(tok.nextToken()).doubleValue();
                }
                int color = (int)segment[4] + 1;
                double[][] dcoord = new double[2][2];
                dcoord[0][0] = segment[0];
                dcoord[1][0] -=segment[1];
                dcoord[0][1] = segment[2];
                dcoord[1][1] -=segment[3];
                double[][] dref = new double[2][2];
                dref = ng.toLinEle(dcoord);
                int y1 = (int)Math.round(dref[1][0]-uLine);
                int x1 = (int)Math.round(dref[0][0]-uEle);
                int y2 = (int)Math.round(dref[1][1]-uLine);
                int x2 = (int)Math.round(dref[0][1]-uEle);
                int xDiff = Math.abs(x2 - x1);
                int yDiff = Math.abs(y2 - y1);
                double m = (double)0.0;
                double b = (double)0.0;
                if (xDiff != 0) {
                    m = (double)(y1 - y2) / (double)(x1 - x2);
                    b = (double)y1 - m*(double)x1;
                }
                int num = yDiff;
                int inc = 1;
                boolean xFlag = true;
                if (xDiff > yDiff) {
                    num = xDiff;
                    if (x2 < x1) inc = -1;
                } else {
                    xFlag = false;
                    if (y2 < y1) inc = -1;
                }
                if (num == 0) num = 1;
                int y = y1;
                int x = x1;
                for (int j=0; j<num; j++) {
                    if (((y<height)&&(y>0)) && ((x<width)&&(x>0))) {
                        values[0][y*width + x] = color;
                    }
                    if (xFlag) {
                        x += inc;
                        if (yDiff != 0) {
                            y = (int)Math.round(m*(double)x + b);
                        }
                    } else {
                        y += inc;
                        if (xDiff != 0) {
                            x = (int)Math.round(((double)y - b) / m);
                        }
                    }
                }
                if (((y2<height)&&(y2>0)) && ((x2<width)&&(x2>0))) {
                    values[0][y2*width + x2] = color;
                }
            }

/*
        double[][] linele = new double[2][4];
        double[][] latlon = new double[2][4];
       // LR
        linele[0][0] = (double)(width-1);
        linele[1][0] = 0.0;
       // UL
        linele[0][1] = 0.0;
        linele[1][1] = (double)(height-1);
       // LL
        linele[0][2] = 0.0;
        linele[1][2] = 0.0;
       // UR
        linele[0][3] = (double)(width-1);
        linele[1][3] = (double)(height-1);
                                                                                              
        latlon = cs.toReference(linele);
        System.out.println("linele: " + linele[0][0] + " " + linele[1][0] + " " +
                           linele[0][1] + " " + linele[1][1] + " " +
                           linele[0][2] + " " + linele[1][2] + " " +
                           linele[0][3] + " " + linele[1][3]);
        System.out.println("latlon: " + latlon[0][0] + " " + latlon[1][0] + " " +
                           latlon[0][1] + " " + latlon[1][1] + " " +
                           latlon[0][2] + " " + latlon[1][2] + " " +
                           latlon[0][3] + " " + latlon[1][3]);
*/
 
        RealType[] domain_components = {RealType.getRealType("ImageElement", null, null),
              RealType.getRealType("ImageLine", null, null)};
        RealTupleType image_domain =
                   new RealTupleType(domain_components, cs, null);

//  Image numbering is usually the first line is at the "top"
//  whereas in VisAD, it is at the bottom.  So define the
//  domain set of the FlatField to map the Y axis accordingly
 
        Linear2DSet domain_set = new Linear2DSet(image_domain,
                                 0, (width - 1), width,
                                 (height - 1), 0, height );
        RealType range = RealType.getRealType("brightness");
                                                                                              
        FunctionType image_func = new FunctionType(image_domain, range);
                                                                                              
// now, define the Data objects
        image_data = new FlatField(image_func, domain_set);
        DateTime date = new DateTime(nominal_time);
        image_data = new NavigatedImage(image_data, date, "McIDAS Image");

// put the data values into the FlatField image_data
        image_data.setSamples(values,false);
        field = (SingleBandedImage) image_data;

        return field;
    }
}
