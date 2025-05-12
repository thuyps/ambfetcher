/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package android.os;

import java.awt.EventQueue;
import java.util.ArrayList;

/**
 *
 * @author MAC
 */
public class Handler {    
    //===============================
    static Thread _thread;
    static boolean _threadIsRunning = false;
    
    public static void initHandler(){
        _runs = new ArrayList<>();
        if (_thread == null){
            _thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    _threadIsRunning = true;
                    
                    _doRunLoop();
                }
            });
            _thread.start();
        }
    }
    
    static void _doRunLoop(){
        while (_threadIsRunning){
            try{
                Thread.sleep(5);
                
                if (_runs.size() > 0)
                {
                    RunItem item = null;

                    synchronized (_runs) {
                        long now = System.currentTimeMillis();                        
                        for (RunItem i: _runs)
                        {
                            if (now >= i.t){
                                item = i;
                                break;
                            }
                        }
                        if (item != null){
                            _runs.remove(item);
                        }
                    }
                    //  do run
                    if (item != null){
                        javax.swing.SwingUtilities.invokeLater(item.r);
                    }
                }
            }
            catch(Throwable e){
                e.printStackTrace();
            }
        }
    }
    
    public static void pushADelayed(int delayMs, Runnable r){
        long t = System.currentTimeMillis() + delayMs;
        RunItem item = new RunItem();
        item.r = r;
        item.t = t;
        
        synchronized (_runs) {
            _runs.add(item);
        }
    }
    
    public static void uninitHandler(){
        _threadIsRunning = false;
    }
    static class RunItem{
        public Runnable r;
        public long t;
    }
    static ArrayList<RunItem> _runs;
    //===============================    
    
    public void postDelayed(int delayMs, Runnable r){
        if (delayMs <= 0){
            post(r);
        }
        else{
            pushADelayed(delayMs, r);
        }
    }
    
    public void postDelayed(Runnable r, int delayMs){
        if (delayMs <= 0){
            post(r);
        }
        else{
            pushADelayed(delayMs, r);
        }
    }
    
    public void post(Runnable r)
    {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                r.run();
            }
        });
    }
}
