/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2018
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

package edu.wisc.ssec.mcidasv.data.hydra;

import static java.lang.Character.isDigit;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.parseInt;

import java.util.Comparator;

/**
 * Sorts NOAA format VIIRS variable names by band number.
 * 
 * <p>For example, ensures:<br/>
 * 
 * {@code All_Data/VIIRS-M2-SDR_All/Radiance}<br/>
 * 
 * appears before:<br/>
 * 
 * {@code All_Data/VIIRS-M16-SDR_All/Radiance}<br/></p>
 * 
 * <p>Using natural ordering, it will not, since the band numbers are
 * not zero-padded.</p>
 * 
 * @author tommyj
 */
public final class VIIRSSort implements Comparator<String> {
    
    private static final String VIIRS = "All_Data/VIIRS";
    
    @Override
    public int compare(String v1, String v2) {
        int result = 0;
        
        if ((v1 != null) && (v2 != null)) {
            
            // Assume caller is testing on NOAA-format VIIRS data,
            // but do some basic checks just in case.  If true,
            // apply a further filter based on Band token
            
            if (v1.startsWith(VIIRS) && v2.startsWith(VIIRS)) {
                
                // pull band out of 1st variable name
                int index1 = v1.indexOf('-');
                int index2 = v1.indexOf('-', index1 + 1);
                String band = v1.substring(index1 + 2, index2);
                int b1 = isDigit(band.charAt(0)) ? parseInt(band) : MAX_VALUE;
                
                // pull band out of 2nd variable name
                index1 = v2.indexOf('-');
                index2 = v2.indexOf('-', index1 + 1);
                band = v2.substring(index1 + 2, index2);
                int b2 = isDigit(band.charAt(0)) ? parseInt(band) : MAX_VALUE;
                
                result = Integer.compare(b1, b2);
            } else {
                // all we know is that one of these isn't VIIRS; so default to
                // natural ordering
                result = v1.compareTo(v2);
            }
        }
        return result;
    }
}
