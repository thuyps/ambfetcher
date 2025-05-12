/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package xframe.utils;

import android.content.Context;
import xframe.utils.xUtils;

/**
 *
 * @author MAC
 */
public class xDevice {
    static public int point2Pixels(float pt){
        return (int)(pt*xUtils.density());
    }
    
    static public float pixelsToPoints(int px){
        return (int)(px/xUtils.density());
    }
    
    static public float point2PixelsF(float pt){
        return (pt*xUtils.density());
    }
    
    static public void initDevice(Context context){
        
    }
    
}
