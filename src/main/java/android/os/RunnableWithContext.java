/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package android.os;

/**
 *
 * @author MAC
 */
public abstract class RunnableWithContext implements Runnable{
    Object context;
    public RunnableWithContext(Object c){
        context = c;
    }

    @Override
    public void run(){
        try{
            run(context);
        }catch(Throwable e){
            e.printStackTrace();
        }
    }

    public abstract void run(Object context);
}