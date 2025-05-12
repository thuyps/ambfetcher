/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package xframe.framework;

/**
 *
 * @author macbookpro
 */
public interface IActionDelegate {
    static final public int ACTION_CANCEL = 0;
    static final public int ACTION_OK = 1;
    static final public int ACTION_FAILED = 2;
    
    void onAction(int action, Object data);
}
