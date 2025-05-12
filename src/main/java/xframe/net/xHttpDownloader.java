package xframe.net;



import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import okhttp3.CacheControl;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;
import xframe.framework.xDataOutput;
import xframe.utils.xUtils;
import xframe.utils.xUtils;


public class xHttpDownloader {
	public static final String kContentType			= "Content-Type";
	public static final String kApplicationJSon		= "application/json";
	public static final String kApplicationStream	= "application/octet-stream";
	public interface xHttpListener
	{
		void onProgress(xHttpDownloader http, int downloaded, int totalSize);
		void onDone(xHttpDownloader http, xDataOutput data);
		void onError(xHttpDownloader http, String error);
	}

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

		@Override public MediaType contentType() {
			return responseBody.contentType();
		}

		@Override public long contentLength() {
			return responseBody.contentLength();
		}

		@Override public BufferedSource source() {
			if (bufferedSource == null) {
				bufferedSource = Okio.buffer(source(responseBody.source()));
			}
			return bufferedSource;
		}

		private Source source(Source source) {
			return new ForwardingSource(source) {
				long totalBytesRead = 0L;

				@Override public long read(Buffer sink, long byteCount) throws IOException {
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

	public static int MAX_DATAOUTBUT_BUFFER_SIZE = (512*1024);
	String mUrl;
	String mLocalFile;
	String mApi;
	//List<NameValuePair> mNameValuePairs;
	String mStringBody;
	String mContentType;

	boolean mCancel = false;

	xHttpListener mListener;
	public Object mTag;
	public Object requestInfo;

	BufferedInputStream bis;
	//ByteArrayBuffer baf = new ByteArrayBuffer(50);
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

	int httpmethod = HTTP_GET;

	public xHttpDownloader(String url, xHttpListener listener) {
		mListener = listener;
		mUrl = url;
		if (internetOfflineMsg == null){
			internetOfflineMsg = "Cannot connect to server";
		}
	}

	public void setMethod(int method) {
		httpmethod = method;
	}
	
	public void setHTTPBody(String body, String contentType)
	{
		mStringBody = body;
		mContentType = contentType;
	}

	public void setEncodeBody(String body, String contentType)
	{
		mStringBody = body;// xUtils.encodeHTMLRequest(body);
		mContentType = contentType;

		mHeader.remove(kContentType);
		mHeader.put(kContentType, kApplicationStream);
	}
	
	public void setResume(int offset){
		mResumeOffset = offset;
	}
	
	public void setIsStreamingGet(boolean isStreaming)
	{
		mIsStreamingGet = isStreaming;
	}

	public static RequestBody createCustomRequestBody(final MediaType contentType, final File file, final ProgressListener progressListener) {
		return new RequestBody() {
			@Override public MediaType contentType() {
				return contentType;
			}
			@Override public long contentLength() {
				return file.length();
			}
			@Override public void writeTo(BufferedSink sink) throws IOException {
				Source source = null;
				try {
					source = Okio.source(file);
					//sink.writeAll(source);
					Buffer buf = new Buffer();
					Long remaining = contentLength();
					int total = (int)contentLength();
					int reads = 0;
					for (long readCount; (readCount = source.read(buf, 2048)) != -1; ) {
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

	public void startUpload(final String filepath)
	{
		new Thread(new Runnable() {
			@Override
			public void run() {
				_startUpload(filepath);
			}
		}).start();
	}


	public void _startUpload(String filepath)
	{
		try {
			final ProgressListener progressListener = new ProgressListener() {
				@Override public void update(long bytesRead, long contentLength, boolean done) {
					mListener.onProgress(xHttpDownloader.this, (int)bytesRead, (int)contentLength);
				}
			};

			RequestBody requestBody = createCustomRequestBody(MediaType.parse("application/octet-stream"), new File(filepath), progressListener);

			//	request
			Headers headerbuild = Headers.of(mHeader);

			Request request = new Request.Builder().url(mUrl)
					.post(requestBody)
					.headers(headerbuild)
                                .cacheControl(CacheControl.FORCE_NETWORK)
					.build();

			//----------
			OkHttpClient client = new OkHttpClient.Builder()
					.connectTimeout(60, TimeUnit.SECONDS)
					.readTimeout(60, TimeUnit.SECONDS)
					.writeTimeout(60, TimeUnit.SECONDS)
                                        .cache(null)
					.build();
			try {
				Response response = client.newCall(request).execute();
				if (!response.isSuccessful()) {
					mListener.onError(this, internetOfflineMsg);
				}

				ResponseBody responseBody = response.body();

				//	read output
				byte[] p = responseBody.bytes();
				final xDataOutput o = new xDataOutput(p.length);
				o.write(p);

                                mListener.onDone(xHttpDownloader.this, o);
			}
			catch (Throwable throwable){
				throwable.printStackTrace();

				mListener.onError(xHttpDownloader.this, internetOfflineMsg);

			}
		}
		catch (Throwable e){
			e.printStackTrace();
		}
	}


	public void startRequest()
	{
		new Thread(new Runnable() {
			@Override
			public void run() {
				_startRequest();
			}
		}).start();
	}
        
        public void startPutRequest()
        {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    _startPutRequest();
                }
            }).start();
        }

	public void _startRequest() {
		Request request;
		if (mStringBody != null && mStringBody.length() > 0){
			RequestBody requestBody = RequestBody.create(MediaType.parse(mContentType), mStringBody);

			request = new Request.Builder()
					.url(mUrl)
					.post(requestBody)
                                .cacheControl(CacheControl.FORCE_NETWORK)
					.build();
		}
		else{
			request = new Request.Builder()
					.url(mUrl)
                                .cacheControl(CacheControl.FORCE_NETWORK)
					.build();
		}


		final ProgressListener progressListener = new ProgressListener() {
			@Override public void update(long bytesRead, long contentLength, boolean done) {
				double progress = bytesRead;
				progress /= contentLength;
				mListener.onProgress(xHttpDownloader.this, (int)bytesRead, (int)contentLength);
			}
		};

		OkHttpClient client = new OkHttpClient.Builder()
				.addNetworkInterceptor(new Interceptor() {
					@Override public Response intercept(Chain chain) throws IOException {
						Response originalResponse = chain.proceed(chain.request());
						return originalResponse.newBuilder()
								.body(new ProgressResponseBody(originalResponse.body(), progressListener))
								.build();
					}
				})
				.connectTimeout(60, TimeUnit.SECONDS)
				.readTimeout(60, TimeUnit.SECONDS)
				.writeTimeout(60, TimeUnit.SECONDS)
                                .cache(null)
				.build();

		try {
			Response response = client.newCall(request).execute();
			if (!response.isSuccessful()) {
				mListener.onError(this, internetOfflineMsg);
			}
                        else{
                            ResponseBody responseBody = response.body();

                            //	read output
                            byte[] p = responseBody.bytes();
                            final xDataOutput o = new xDataOutput(p.length);
                            o.write(p);

                            mListener.onDone(xHttpDownloader.this, o);
                        }

		}
		catch (Throwable throwable){
			throwable.printStackTrace();

			mListener.onError(xHttpDownloader.this, internetOfflineMsg);

		}
	}
        
        public void _startPutRequest() {
		Request request;
		if (mStringBody != null && mStringBody.length() > 0){
			RequestBody requestBody = RequestBody.create(MediaType.parse(mContentType), mStringBody);

			request = new Request.Builder()
					.url(mUrl)
					.put(requestBody)
                                .cacheControl(CacheControl.FORCE_NETWORK)
					.build();
		}
		else{
			request = new Request.Builder()
					.url(mUrl)
                                .cacheControl(CacheControl.FORCE_NETWORK)
					.build();
		}


		final ProgressListener progressListener = new ProgressListener() {
			@Override public void update(long bytesRead, long contentLength, boolean done) {
				double progress = bytesRead;
				progress /= contentLength;
				mListener.onProgress(xHttpDownloader.this, (int)bytesRead, (int)contentLength);
			}
		};

		OkHttpClient client = new OkHttpClient.Builder()
				.addNetworkInterceptor(new Interceptor() {
					@Override public Response intercept(Interceptor.Chain chain) throws IOException {
						Response originalResponse = chain.proceed(chain.request());
						return originalResponse.newBuilder()
								.body(new ProgressResponseBody(originalResponse.body(), progressListener))
								.build();
					}
				})
				.connectTimeout(60, TimeUnit.SECONDS)
				.readTimeout(60, TimeUnit.SECONDS)
				.writeTimeout(60, TimeUnit.SECONDS)
                                .cache(null)
				.build();

		try {
			Response response = client.newCall(request).execute();
			if (!response.isSuccessful()) {
				mListener.onError(this, internetOfflineMsg);
                                return;
			}

			ResponseBody responseBody = response.body();

			//	read output
			byte[] p = responseBody.bytes();
			final xDataOutput o = new xDataOutput(p.length);
			o.write(p);

			mListener.onDone(xHttpDownloader.this, o);

		}
		catch (Throwable throwable){
			throwable.printStackTrace();

			mListener.onError(xHttpDownloader.this, internetOfflineMsg);

		}
	}

	public void cancel() {
		mCancel = true;
		
		try
		{

		}
		catch(Throwable e)
		{
			e.printStackTrace();
		}
	}
	
	boolean mCallStart = false;

	HashMap<String, String> mHeader = new HashMap<String, String>();
	public void addHeader(String field, String value)
	{
		mHeader.put(field, value);
	}

	
	int mInfoInt = 0;
	public int getUserDataInt()
	{
		return mInfoInt;
	}
	
	public void setUserDataInt(int infoInt)
	{
		mInfoInt = infoInt;
	}
	
	public boolean isHttpFinished()
	{
		return isFinished;
	}
        
    @Override
    protected void finalize() throws Throwable {
        super.finalize(); //To change body of generated methods, choose Tools | Templates.
        
        xUtils.trace("end of http downloader");
    }

}
