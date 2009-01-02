/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2009
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
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

package edu.wisc.ssec.mcidasv.startupmanager.options;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

public class BooleanOption extends AbstractOption {
    private String value = "0";

    public BooleanOption(final String id, final String label, 
        final String defaultValue, 
        final OptionMaster.OptionPlatform optionPlatform,
        final OptionMaster.Visibility optionVisibility) 
    {
        super(id, label, OptionMaster.Type.BOOLEAN, optionPlatform, optionVisibility);
        setValue(defaultValue);
    }

    public JComponent getComponent() {
        final JCheckBox cb = new JCheckBox();
        cb.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                setValue(cb.isSelected() ? "1" : "0");
            }
        });
        boolean booleanValue = false;
        if (value.equals("1"))
            booleanValue = true;
        cb.setSelected(booleanValue);
        if (!onValidPlatform()) {
            cb.setEnabled(false);
        }
        return cb;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String newValue) {
        if (newValue.equals("0") || newValue.equals("1"))
            value = newValue;
    }

    public String toString() {
        return String.format("[BooleanOption@%x: optionId=%s, value=%s]", 
            hashCode(), getOptionId(), getValue());
    }
}
