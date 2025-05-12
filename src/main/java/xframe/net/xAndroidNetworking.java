package xframe.net;

import android.content.Context;

import java.nio.charset.StandardCharsets;
import org.codehaus.jettison.json.JSONObject;

import xframe.framework.xDataOutput;

public class xAndroidNetworking {
    xHttpOkHttp3 http;
    int method;
    String url;
    Context context;

    public static xAndroidNetworking get(String url, Context context)
    {
        xAndroidNetworking androidNetworking = new xAndroidNetworking(url, xHttpOkHttp3.HTTP_GET, context);

        return androidNetworking;
    }

    public xAndroidNetworking(String url, int method, Context context){
        this.method = method;
        this.url = url;
        this.context = context;
    }

    public xAndroidNetworking build()
    {
        http = new xHttpOkHttp3(context, url, null);

        return this;
    }

    public void getAsJSONObject(JSONObjectRequestListener delegate)
    {
        http.setListener(new xHttpOkHttp3.IGetFileListener() {
            @Override
            public void onDone(xHttpOkHttp3 sender, int total, xDataOutput data) {
                try{
                    String s = new String(data.getBytes(), 0, data.size(), StandardCharsets.UTF_8);
                    JSONObject jsonObject = new JSONObject(s);
                    delegate.onResponse(jsonObject);
                }
                catch (Throwable e){
                    e.printStackTrace();

                    delegate.onError(e.getMessage());
                }
            }

            @Override
            public void onProgress(xHttpOkHttp3 sender, int transfered, int total) {

            }

            @Override
            public void onError(xHttpOkHttp3 sender, String error) {
                delegate.onError(error);
            }
        });

        http.setMethod(method);

        http.doGet();
    }

    public void getAsString(StringRequestListener delegate)
    {
        http.setListener(new xHttpOkHttp3.IGetFileListener() {
            @Override
            public void onDone(xHttpOkHttp3 sender, int total, xDataOutput data) {
                try{
                    String s = new String(data.getBytes(), 0, data.size(), StandardCharsets.UTF_8);
                    delegate.onResponse(s);
                }
                catch (Throwable e){
                    e.printStackTrace();

                    delegate.onError(e.getMessage());
                }
            }

            @Override
            public void onProgress(xHttpOkHttp3 sender, int transfered, int total) {

            }

            @Override
            public void onError(xHttpOkHttp3 sender, String error) {
                delegate.onError(error);
            }
        });

        http.setMethod(method);

        http.doGet();
    }

}
