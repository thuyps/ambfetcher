package xframe.net;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;

import okhttp3.ConnectionPool;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.Util;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;
import xframe.framework.xDataOutput;
import xframe.framework.xFileManager;
import xframe.utils.xUtils;

public class xHttpOkHttp3 extends Thread {

    public interface IGetFileListener {

        void onDone(xHttpOkHttp3 sender, int total, xDataOutput data);

        void onProgress(xHttpOkHttp3 sender, int transfered, int total);

        void onError(xHttpOkHttp3 sender, String error);
    }

    public static final String kContentType = "Content-Type";
    public static final String kApplicationJSon = "application/json";
    public static final String kApplicationStream = "application/octet-stream";

    static ConnectionPool connectionPool;

    interface ProgressListener {

        void update(long bytesRead, long contentLength, boolean done);
    }

    private static class ProgressResponseBody extends ResponseBody {

        private final ResponseBody responseBody;
        private final ProgressListener progressListener;
        private BufferedSource bufferedSource;

        ProgressResponseBody(ResponseBody responseBody, ProgressListener progressListener) {
            this.responseBody = responseBody;
            this.progressListener = progressListener;
        }

        @Override
        public MediaType contentType() {
            return responseBody.contentType();
        }

        @Override
        public long contentLength() {
            return responseBody.contentLength();
        }

        @Override
        public BufferedSource source() {
            if (bufferedSource == null) {
                bufferedSource = Okio.buffer(source(responseBody.source()));
            }
            return bufferedSource;
        }

        private Source source(Source source) {
            return new ForwardingSource(source) {
                long totalBytesRead = 0L;

                @Override
                public long read(Buffer sink, long byteCount) throws IOException {
                    long bytesRead = super.read(sink, byteCount);
                    // read() returns the number of bytes read, or -1 if this source is exhausted.
                    totalBytesRead += bytesRead != -1 ? bytesRead : 0;
                    progressListener.update(totalBytesRead, responseBody.contentLength(), bytesRead == -1);
                    return bytesRead;
                }
            };
        }
    }
    //=============================================================

    public static final int HTTP_POST = 1;
    public static final int HTTP_GET = 0;

    public static int MAX_DATAOUTBUT_BUFFER_SIZE = (512 * 1024);
    String mUrl;
    String mLocalFile;
    String mApi;
    String mStringBody;
    xDataOutput mBinaryData;
    String mContentType;

    Handler handler;

    boolean mCancel = false;

    IGetFileListener mListener;
    public Object mTag;
    public Object requestInfo;

    BufferedInputStream bis;
    int current = 0;

    int count = 0;
    boolean isFinished = false;

    String fileName;

    public int mLevel;
    public int filesize;
    public int total_count = 0;
    public boolean mNeedValidData = false;
    int mResumeOffset = 0;
    boolean mIsStreamingGet = false;
    int mRetryCount = 0;

    static String internetOfflineMsg = null;
    //=============================================
    //=============================================
    Context mContext;

    public static void cleanup() {
        connectionPool = null;
    }

    int httpmethod = HTTP_GET;

    public xHttpOkHttp3(Context context, String url, IGetFileListener listener) {
        mContext = context;
        mListener = listener;
        mUrl = url;
        if (internetOfflineMsg == null) {
            internetOfflineMsg = "Network error.";
        }

        FolderCache = getRootWorkingDir(context);
        xFileManager.createAllDirs(FolderCache);

        handler = new Handler();
    }

    public void setInvokeCallbackInBackground() {
        handler = null;
    }

    public static String getRootWorkingDir(Context context) {
        return Environment.getExternalStorageDirectory() + "/Android/data/" + context.getPackageName() + "/files/net";
    }

    public void setListener(IGetFileListener listener) {
        mListener = listener;
    }

    public Context getContext() {
        return mContext;
    }

    public void setMethod(int method) {
        httpmethod = method;
    }

    public void setResume(int offset) {
        mResumeOffset = offset;
    }

    public void setIsStreamingGet(boolean isStreaming) {
        mIsStreamingGet = isStreaming;
    }

    public static RequestBody createCustomRequestBody(final MediaType contentType, final File file, final ProgressListener progressListener) {
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return contentType;
            }

            @Override
            public long contentLength() {
                return file.length();
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                Source source = null;
                try {
                    source = Okio.source(file);
                    //sink.writeAll(source);
                    Buffer buf = new Buffer();
                    Long remaining = contentLength();
                    int total = (int) contentLength();
                    int reads = 0;
                    for (long readCount; (readCount = source.read(buf, 2048)) != -1;) {
                        sink.write(buf, readCount);
                        xUtils.trace("xHttpDownloader", "source size: " + contentLength() + " remaining bytes: " + (remaining -= readCount));

                        reads += readCount;

                        progressListener.update(reads, total, reads == total);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    public static RequestBody createCustomRequestBodyIS(final MediaType mediaType, final InputStream inputStream) {
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return mediaType;
            }

            @Override
            public long contentLength() {
                try {
                    return inputStream.available();
                } catch (IOException e) {
                    return 0;
                }
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                Source source = null;
                try {
                    source = Okio.source(inputStream);
                    sink.writeAll(source);
                } finally {
                    Util.closeQuietly(source);
                }
            }
        };
    }

    long lastTimeProgress = 0;
    ProgressListener progressListener = new ProgressListener() {
        @Override
        public void update(final long bytesRead, final long contentLength, boolean done) {
            double progress = bytesRead;
            progress /= contentLength;

            long d = System.currentTimeMillis() - lastTimeProgress;
            if (d < 50) {
                return;
            }
            lastTimeProgress = System.currentTimeMillis();

            if (handler != null) {
                handler.post(() -> {
                    mListener.onProgress(xHttpOkHttp3.this, (int) bytesRead, (int) contentLength);
                });
            } else {
                mListener.onProgress(xHttpOkHttp3.this, (int) bytesRead, (int) contentLength);
            }
        }
    };
    //==============================================================

    public void startUpload(final String filepath) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                _startUpload(filepath);
            }
        }).start();
    }

    public void _startUpload(String filepath) {
        try {
            final ProgressListener uploadProgress = new ProgressListener() {
                @Override
                public void update(long bytesRead, long contentLength, boolean done) {
                    //mListener.onProgress(xHttpOkHttp3.this, (int)bytesRead, (int)contentLength);
                    xUtils.trace("uploaded: " + bytesRead);
                }
            };

            RequestBody requestBody = createCustomRequestBody(MediaType.parse("application/octet-stream"),
                    new File(filepath),
                    uploadProgress);

            //	request
            Headers headerbuild = Headers.of(mHeader);

            Request request = new Request.Builder().url(mUrl)
                    .post(requestBody)
                    .headers(headerbuild)
                    .build();

            //----------
            if (connectionPool == null) {
                connectionPool = new ConnectionPool();
            }
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectionPool(connectionPool)
                    .addNetworkInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Response originalResponse = chain.proceed(chain.request());
                            return originalResponse.newBuilder()
                                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                                    .build();
                        }
                    })
                    .addNetworkInterceptor(headerInterceptor)
                    .connectTimeout(200, TimeUnit.SECONDS)
                    .readTimeout(200, TimeUnit.SECONDS)
                    .writeTimeout(200, TimeUnit.SECONDS)
                    .build();
            try {
                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    mListener.onError(this, "Network error.");
                }

                ResponseBody responseBody = response.body();

                //	read output
                /*
				InputStream inputStream = responseBody.byteStream();
				xDataOutput o = new xDataOutput(64*1024);
				byte p[] = new byte[1024];
				while (true)
				{
					int readBytes = inputStream.read(p, 0, p.length);
					if (readBytes > 0){
						o.write(p, 0, readBytes);
					}
					if (readBytes == -1){
						//Thread.sleep(10);
						//continue;
						break;
					}
					if (readBytes == 0){
						break;
					}
				}

                 */

 /*
				byte[] p = responseBody.bytes();
				final xDataOutput o = new xDataOutput(p.length);
				o.write(p);

                 */
                InputStream inputStream = responseBody.byteStream();
                xDataOutput o = new xDataOutput(64 * 1024);
                xUtils.trace("network 4");
                byte[] p = new byte[1024];
                int negativeCnt = 200;
                while (true) {
                    int readBytes = inputStream.read(p, 0, p.length);
                    if (readBytes > 0) {
                        negativeCnt = 200;
                        //xUtils.trace("okhttp: append: " + readBytes + " to " + o.size());
                        o.write(p, 0, readBytes);
                    }

                    if (readBytes == -1) {
                        negativeCnt--;
                        //xUtils.trace("okhttp: -1");
                        if (negativeCnt > 0) {
                            Thread.sleep(10);
                            continue;
                        } else {
                            break;
                        }
                    }
                    if (readBytes < p.length) {
                        if (responseBody.contentLength() > 0 && o.size() == responseBody.contentLength()) {
                            break;
                        }
                        //xUtils.trace("okhttp: reading 000");
                    }
                }

                xUtils.trace("update net: " + o.size());

                if (handler != null) {
                    handler.post(() -> {
                        mListener.onDone(xHttpOkHttp3.this, o.size(), o);
                    });
                } else {
                    mListener.onDone(xHttpOkHttp3.this, o.size(), o);
                }

            } catch (Throwable throwable) {
                throwable.printStackTrace();

                if (handler != null) {
                    handler.post(() -> {
                        mListener.onError(xHttpOkHttp3.this, "Network error.");
                    });
                } else {
                    mListener.onError(xHttpOkHttp3.this, "Network error.");
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void doGet() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                _startRequest();
            }
        }).start();
    }

    static int fileID = 0;
    public String FolderCache;	//	C.FOLDER_APP

    public void doPostWithData(xDataOutput data) {
        mBinaryData = data;
        mContentType = "application/octet-stream";

        fileID++;

        String filename = "" + fileID + ".dat";
        final String tmp = FolderCache + "/net/" + filename;

        xFileManager.removeFile(tmp);
        xFileManager.saveFile(data, FolderCache + "/net/", filename);

        new Thread(new Runnable() {
            @Override
            public void run() {
                _startUpload(tmp);
            }
        }).start();
    }

    public void doPostWithDataString(String data, String contentType) {
        mStringBody = data;
        mContentType = contentType;

        new Thread(new Runnable() {
            @Override
            public void run() {
                _startRequest();
            }
        }).start();
    }

    private static final Interceptor headerInterceptor = new Interceptor() {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request.Builder builder = chain.request().newBuilder();
            builder.addHeader("X-OS-Name", "android");
            builder.addHeader("X-Platform", "mobile_app");
            //builder.addHeader("X-Device-Model", DeviceUtils.getDeviceModel());
            //builder.addHeader("X-OS-Version", DeviceUtils.getDeviceVersion());
            Request request = builder.build();
            return chain.proceed(request);
        }
    };

    public void _startRequest() {
        Request request;
        Headers headerbuild = Headers.of(mHeader);
        if (mStringBody != null && mStringBody.length() > 0) {
            RequestBody requestBody = RequestBody.create(MediaType.parse(mContentType), mStringBody);

            request = new Request.Builder()
                    .url(mUrl)
                    .headers(headerbuild)
                    .post(requestBody)
                    .build();
        } else if (mBinaryData != null) {
            RequestBody requestBody = createCustomRequestBodyIS(MediaType.parse(mContentType),
                    new ByteArrayInputStream(mBinaryData.getBytes()));

            request = new Request.Builder()
                    .url(mUrl)
                    .headers(headerbuild)
                    .post(requestBody)
                    .build();
        } else {
            request = new Request.Builder()
                    .headers(headerbuild)
                    .url(mUrl)
                    .build();
        }

        if (connectionPool == null) {
            connectionPool = new ConnectionPool();
        }

        Response response = null;

        OkHttpClient client = new OkHttpClient.Builder()
                .connectionPool(connectionPool)
                .addNetworkInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Response originalResponse = chain.proceed(chain.request());
                        return originalResponse.newBuilder()
                                .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                                .build();
                    }
                })
                .addNetworkInterceptor(headerInterceptor)
                .connectTimeout(200, TimeUnit.SECONDS)
                .readTimeout(200, TimeUnit.SECONDS)
                .writeTimeout(200, TimeUnit.SECONDS)
                //.proxy(Proxy.NO_PROXY)
                .build();

        try {
            xUtils.trace("network 1");
            response = client.newCall(request).execute();
            if (!response.isSuccessful()) {
                //  ignore this, it still runnign in background
                //mListener.onError(this, "Network error.");
            }
            xUtils.trace("network 2");

            ResponseBody responseBody = response.body();

            xUtils.trace("network 3");
            InputStream inputStream = responseBody.byteStream();
            xDataOutput o = new xDataOutput(64 * 1024);
            xUtils.trace("network 4");
            byte[] p = new byte[1024];
            int negativeCnt = 20;//0;
            while (true) {
                //  the total number of bytes read into the buffer, or
                //  {@code -1} if there is no more data because the end or the stream has been reached.
                int readBytes = inputStream.read(p, 0, p.length);
                if (readBytes > 0) {
                    negativeCnt = 200;
                    //xUtils.trace("okhttp: append: " + readBytes + " to " + o.size());
                    o.write(p, 0, readBytes);
                    
                    negativeCnt = 20;
                }

                //  -1: 
                if (readBytes == -1) {
                    negativeCnt--;
                    //xUtils.trace("okhttp: -1");
                    if (negativeCnt > 0) {
                        Thread.sleep(10);
                        continue;
                    } else {
                        break;
                    }
                }
                if (readBytes < p.length) {
                    if (responseBody.contentLength() > 0 && o.size() == responseBody.contentLength()) {
                        break;
                    }
                    //xUtils.trace("okhttp: reading 000");
                }
            }
            xUtils.trace("network 5");
            //	read output
            /*
			byte[] p = responseBody.bytes();
			final xDataOutput o = new xDataOutput(p.length);
			o.write(p);

             */

            if (handler != null) {
                handler.post(() -> {
                    mListener.onDone(xHttpOkHttp3.this, o.size(), o);
                });
            } else {
                mListener.onDone(xHttpOkHttp3.this, o.size(), o);
            }

        } catch (Throwable throwable) {
            xUtils.trace("network xxxxxx");
            throwable.printStackTrace();

            if (handler != null) {
                handler.post(() -> {
                    mListener.onError(xHttpOkHttp3.this, "Network error.");
                });
            } else {
                mListener.onError(xHttpOkHttp3.this, "Network error.");
            }

        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (Throwable e3) {
            }
        }
    }

    public void cancel() {
        mCancel = true;

        try {

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    boolean mCallStart = false;

    HashMap<String, String> mHeader = new HashMap<>();

    public void addHeader(String field, String value) {
        mHeader.put(field, value);
    }

    int mInfoInt = 0;

    public int getUserDataInt() {
        return mInfoInt;
    }

    public void setUserDataInt(int infoInt) {
        mInfoInt = infoInt;
    }

    public boolean isHttpFinished() {
        return isFinished;
    }
}
