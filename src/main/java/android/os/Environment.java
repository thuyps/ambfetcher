/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package android.os;

import android.content.Context;

/**
 *
 * @author MAC
 */
public class Environment {
    static public String getExternalStorageDirectory(){
        return Context.getInstance().getExternalFilesDir(null);
    }
}
