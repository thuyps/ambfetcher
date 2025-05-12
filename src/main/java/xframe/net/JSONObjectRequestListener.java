package xframe.net;

import org.codehaus.jettison.json.JSONObject;


public interface JSONObjectRequestListener {
    void onResponse(JSONObject response);
    void onError(String error);
}
