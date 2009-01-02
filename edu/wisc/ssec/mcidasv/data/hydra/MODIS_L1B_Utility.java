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

package edu.wisc.ssec.mcidasv.data.hydra;
import visad.Set;


public class MODIS_L1B_Utility {

 //... Effective central wavenumbers (inverse centimeters)
   static double[] cwn_terra = {
       2.641767E+03, 2.505274E+03, 2.518031E+03, 2.465422E+03,
       2.235812E+03, 2.200345E+03, 1.478026E+03, 1.362741E+03,
       1.173198E+03, 1.027703E+03, 9.081998E+02, 8.315149E+02,
       7.483224E+02, 7.309089E+02, 7.188677E+02, 7.045309E+02};
                                                                                                                                                     
 //... Temperature correction slopes (no units)
   static double[]  tcs_terra = {
       9.993487E-01,  9.998699E-01,  9.998604E-01,  9.998701E-01,
       9.998825E-01,  9.998849E-01,  9.994942E-01,  9.994937E-01,
       9.995643E-01,  9.997499E-01,  9.995880E-01,  9.997388E-01,
       9.999192E-01,  9.999171E-01,  9.999174E-01,  9.999264E-01};
                                                                                                                                                     
 //... Temperature correction intercepts (Kelvin)
   static double[]  tci_terra = {
       4.744530E-01,  9.091094E-02,  9.694298E-02,  8.856134E-02,
       7.287017E-02,  7.037161E-02,  2.177889E-01,  2.037728E-01,
       1.559624E-01,  7.989879E-02,  1.176660E-01,  6.856633E-02,
       1.903625E-02,  1.902709E-02,  1.859296E-02,  1.619453E-02};
                                                                                                                                                     
 //... Effective central wavenumbers (inverse centimeters)
   static double[]  cwn_aqua = {
       2.647418E+03, 2.511763E+03, 2.517910E+03, 2.462446E+03,
       2.248296E+03, 2.209550E+03, 1.474292E+03, 1.361638E+03,
       1.169637E+03, 1.028715E+03, 9.076808E+02, 8.308397E+02,
       7.482977E+02, 7.307761E+02, 7.182089E+02, 7.035020E+02};
                                                                                                                                                     
 //... Temperature correction slopes (no units)
   static double[]  tcs_aqua = {
       9.993438E-01, 9.998680E-01, 9.998649E-01, 9.998729E-01,
       9.998738E-01, 9.998774E-01, 9.995732E-01, 9.994894E-01,
       9.995439E-01, 9.997496E-01, 9.995483E-01, 9.997404E-01,
       9.999194E-01, 9.999071E-01, 9.999176E-01, 9.999211E-01};
                                                                                                                                                     
 //... Temperature correction intercepts (Kelvin)
  static double[]  tci_aqua = {
       4.792821E-01, 9.260598E-02, 9.387793E-02, 8.659482E-02,
       7.854801E-02, 7.521532E-02, 1.833035E-01, 2.053504E-01,
       1.628724E-01, 8.003410E-02, 1.290129E-01, 6.810679E-02,
       1.895925E-02, 2.128960E-02, 1.857071E-02, 1.733782E-02};

// Constants are from "The Fundamental Physical Constants",
// Cohen, E. R. and B. N. Taylor, Physics Today, August 1993.
                                                                                                                                                     
// Planck constant (Joule second)
  static double h = 6.6260755e-34;
                                                                                                                                                     
// Speed of light in vacuum (meters per second)
 static double c = 2.9979246e+8;
                                                                                                                                                     
// Boltzmann constant (Joules per Kelvin)
 static double k = 1.380658e-23;
                                                                                                                                                     
// Derived constants
 static double c1 = 2.0 * h * c * c;
 static double c2 = (h * c) / k;
                                                                                                                                                     
  public static float[]  modis_radiance_to_brightnessTemp(String platformName, int band_number, float[] values)
         throws Exception
  {
    return (Set.doubleToFloat(
       new double[][] {modis_radiance_to_brightnessTemp(platformName,band_number,(Set.floatToDouble(new float[][] {values}))[0])}))[0];
  }
  public static double[] modis_radiance_to_brightnessTemp(String platformName, int band_number,  double[] values)
         throws Exception
  {
                                                                                                                                                     
    if ((band_number < 20) || (band_number > 36) || (band_number == 26)) {
      throw new Exception("bad band number: "+band_number+" band 20-36 but not 26");
    }
                                                                                                                                                     
    int index;
                                                                                                                                                     
    if (band_number <= 25) {
      index = band_number - 19;
    }
    else {
      index = band_number - 20;
    }
    index -= 1;
                                                                                                                                                     
    double cwn;
    double tcs;
    double tci;

    if (platformName.equals("Terra")) {
      cwn = cwn_terra[index];
      tcs = tcs_terra[index];
      tci = tci_terra[index];
    }
    else if (platformName.equals("Aqua")) {
      cwn = cwn_aqua[index];
      tcs = tcs_aqua[index];
      tci = tci_aqua[index];
    }
    else {
      throw new Exception("platformName must equal: Terra or Aqua");
    }

    //... Compute Planck radiance - MODIS units
    //... Watts per square meter per steradian per micron
                                                                                                                                                     
    //... Convert wavelength to meters
    double ws = 1.0E-6 * (1.0E+4 / cwn);
                                                                                                                                                     
    //... Compute brightness temperature
    int len = values.length;
    double[] new_values = new double[len];
    for (int kk = 0; kk < len; kk++) {
      double rad = values[kk];
      double BT = ( c2 / (ws * Math.log(c1 /(1.0E+6*rad*(ws*ws*ws*ws*ws)) + 1.0))-tci)/tcs;
      new_values[kk] = BT;
    }

    return new_values;
  }

  public static int emissive_indexToBandNumber(int channelIndex) {
    int[] bandNumbers = new int[] {20,21,22,23,24,25,27,28,29,30,31,32,33,34,35,36};
    return bandNumbers[channelIndex];
  }

}
