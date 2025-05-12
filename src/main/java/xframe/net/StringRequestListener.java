package xframe.net;

import xframe.utils.xUtils;

public interface StringRequestListener {
    void onResponse(String response);

    void onError(String anError);
}
