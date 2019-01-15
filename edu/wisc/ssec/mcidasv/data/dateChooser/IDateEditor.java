/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2019
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
 * 
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.  
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package edu.wisc.ssec.mcidasv.data.dateChooser;

import java.beans.PropertyChangeListener;
import java.util.Date;
import java.util.Locale;

import javax.swing.JComponent;

/**
 * All date editors that should be used by a JDateChooser have to implement this
 * interface.
 * 
 * @author Kai Toedter
 * @version $LastChangedRevision: 105 $
 * @version $LastChangedDate: 2007-02-16 12:56:29 +0100 (Fr, 16 Feb 2007) $
 * 
 */
public interface IDateEditor {

	/**
	 * 
	 * Returns the date.
	 * 
	 * @return the date
	 */
	public Date getDate();

	/**
	 * Sets the date. This should be implemented as a bound property, firing the
	 * "date" property.
	 * 
	 * @param date
	 *            the date to set
	 */
	public void setDate(Date date);

	/**
	 * Sets the date format string, e.g. "MM/dd/yy". If the date format string
	 * is null or invalid, the date format string will be set to the MEDIUM
	 * Simple date format of the current locale.
	 * 
	 * @param dateFormatString
	 *            the date format string
	 */
	public void setDateFormatString(String dateFormatString);

	/**
	 * Returns tha date format string.
	 * 
	 * @return the date format string
	 */
	public String getDateFormatString();

	/**
	 * Sets a valid date range for selectable dates. If max is before
	 * min, the default range with no limitation is set.
	 * 
	 * @param min
	 *            the minimum selectable date or null (then the minimum date should be
	 *            set to 01\01\0001)
	 * @param max
	 *            the maximum selectable date or null (then the maximum date should be
	 *            set to 01\01\9999)
	 */
	public void setSelectableDateRange(Date min, Date max) ;

	/**
	 * Gets the minimum selectable date.
	 * 
	 * @return the minimum selectable date
	 */
	public Date getMaxSelectableDate();
	
	/**
	 * Gets the maximum selectable date.
	 * 
	 * @return the maximum selectable date
	 */
	public Date getMinSelectableDate();

	/**
	 * Sets the maximum selectable date.
	 * 
	 * @param max maximum selectable date
	 */
	public void setMaxSelectableDate(Date max);

	/**
	 * Sets the minimum selectable date.
	 * 
	 * @param min minimum selectable date
	 */
	public void setMinSelectableDate(Date min);

	/**
	 * Returns the UI component, e.g. the actual JTextField implementing the
	 * editor.
	 * 
	 * @return the UI component
	 */
	public JComponent getUiComponent();

	/**
	 * Sets the locale. Usually this should have impact on the current date
	 * format string.
	 * 
	 * @param locale
	 *            the locale to set
	 */
	public void setLocale(Locale locale);

	/**
	 * Enables or disables the UI compoment.
	 * 
	 * @param enabled
	 *            true, if the UI component should be enabled.
	 */
	public void setEnabled(boolean enabled);

	/**
	 * Adds a property change listener that should be added to the implementing
	 * UI component. The UI component should fire a "date" property if the date
	 * changes.
	 * 
	 * @param listener
	 *            the property change listener.
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener);

	/**
	 * Adds a property change listener that should be added to the implementing
	 * UI component. The UI component should fire a "date" property if the date
	 * changes.
	 * 
	 * @param propertyName
	 *            the property name, e.g. "date"
	 * @param listener
	 *            the property change listener.
	 */
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener);

	/**
	 * Removes a property change listener.
	 * 
	 * @param listener
	 *            the property change listener.
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener);

	/**
	 * Removes the listener from the date editor's property change listeners for the specific property.
	 * 
	 * @param propertyName
	 *            the property to listen for, e.g. "date"
	 * @param listener
	 *            the listener
	 */
	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener);
}
