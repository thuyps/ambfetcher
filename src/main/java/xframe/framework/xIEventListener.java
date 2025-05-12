/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package xframe.framework;

/**
 *
 * @author abc
 */
public interface xIEventListener {
/*
    public static final int EVT_RESULT_ERROR = 0;
    public static final int EVT_RESULT_OK = 1;
    public static final int EVT_RESULT_WRONG_DATA = 2;
    public static final int EVT_RESULT_EVT_UNSUPPORTED = 3;
    public static final int EVT_RESULT_INVALID_PARAM = 4;
    public static final int EVT_RESULT_LISTCTRL_NOT_PROCESS = 5;
*/
    int onEvent(Object sender, int evt, int param1, Object param2);
}
