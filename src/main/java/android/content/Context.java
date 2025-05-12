/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package android.content;

import java.io.File;

/**
 *
 * @author MAC
 */
public class Context {
    public static Context _instance = null;
    String _workingDirectory;
    
    static public Context getInstance(){
        if (_instance == null){
            _instance = new Context();
        }
        return _instance;
    }
    
    public String getExternalFilesDir(Object ctx){
        if (_workingDirectory == null){
            try{
                File directory = new File(".");
                _workingDirectory = directory.getCanonicalPath() + File.separator + "appdata";
            }
            catch(Throwable e){
                
            }
        }
        
        return _workingDirectory;
    }
    
    public String getPackageName(){
        return "vnchart";
    }
}
