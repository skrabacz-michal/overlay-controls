package de.asideas.overlay.controls.internal.network;

import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Looper;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.Map;

public class NetworkConnection
{
    private static final String TAG = NetworkConnection.class.getSimpleName();

    private static RequestData mLastRequest;

    private final RequestData mRequest;

    private NetworkConnection(RequestData request)
    {
        this.mRequest = request;
    }

    public static NetworkConnection get(String address)
    {
        return new NetworkConnection(new RequestData(RequestData.GET, address));
    }

    public static NetworkConnection post(String address)
    {
        return new NetworkConnection(new RequestData(RequestData.POST, address));
    }

    public NetworkConnection args(Map<String, Object> args)
    {
        mRequest.setArgs(args);
        return this;
    }

    private void close(Closeable closeable)
    {
        if (closeable == null)
        {
            return;
        }
        try
        {
            closeable.close();
        }
        catch (IOException ioe)
        {
            Log.e(TAG, "Unable to close " + closeable.getClass().getSimpleName(), ioe);
        }
    }

    private HttpURLConnection createConnection(RequestData request) throws IOException
    {
        HttpURLConnection connection = (HttpURLConnection) request.getUrl().openConnection();

        connection.setRequestMethod(request.getMethodName());
        connection.setRequestProperty("Host", request.getUrl().getHost());
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(30 * 1000);
        connection.setUseCaches(false);

        if (request.getMethod() == RequestData.POST)
        {
            connection.setDoOutput(true);
        }

        return connection;
    }

    private NetworkException createNetworkException(RequestData request, HttpURLConnection connection, Exception exception)
    {
        int status = -1;
        String errorResponse = "";

        try
        {
            status = connection.getResponseCode();
            errorResponse = read(connection.getErrorStream());
        }
        catch (IOException ioe)
        {
        }

        Log.e(TAG, "Unable to fetch requested data, error " + status, exception);

        return new NetworkException(request, status, errorResponse, exception);
    }

    public NetworkConnection data(Map<String, Object> data)
    {
        mRequest.setPostData(data);
        return this;
    }

    private InputStream execute(RequestData request) throws NetworkException
    {
        HttpURLConnection connection = null;
        NetworkConnection.mLastRequest = request;

        try
        {
            updateAddressWithArgs(request);

            if (Looper.myLooper() == Looper.getMainLooper())
            {
                throw new NetworkException(request, -1, "", new IOException("Network access inside main thread"));
            }

            Log.d(TAG, "Executing (" + request.getMethodName() + ") url: " + request.getAddress());

            connection = createConnection(request);
            connection.connect();

            if (request.getMethod() == RequestData.POST)
            {
                writePostBody(request, connection.getOutputStream());
            }

            int status = connection.getResponseCode();
            boolean needsRedirect = false;

            if (status != HttpURLConnection.HTTP_OK)
            {
                if (status == HTTP_MOVED_TEMP || status == HTTP_MOVED_PERM || status == HTTP_SEE_OTHER)
                {
                    needsRedirect = true;
                }
                else
                {
                    if (request.isHideErrors())
                    {
                        return null;
                    }

                    throw new IOException("HTTP error " + status + " while fetching " + request.getAddress());
                }
            }

            if (needsRedirect)
            {
                String location = connection.getHeaderField("Location");
                if (!location.startsWith("http"))
                {
                    location = request.getUrl().getProtocol() + "://" + request.getUrl().getHost() + location;
                }

                connection.disconnect();

                request.setMethod(RequestData.GET);
                request.getArgs().clear();
                request.getPostData().clear();
                request.setOriginalAddress(request.getAddress());
                request.setAddress(location);

                return execute(request);
            }

            return new FlushedInputStream(connection.getInputStream());
        }
        catch (IOException ioe)
        {
            throw createNetworkException(request, connection, ioe);
        }
    }

    public Bitmap getBitmap() throws NetworkException
    {
        InputStream inputStream = null;

        try
        {
            inputStream = execute(mRequest);

            if (inputStream == null)
            {
                return null;
            }

            return BitmapFactory.decodeStream(inputStream);
        }
        finally
        {
            close(inputStream);
        }
    }

    public Bitmap getBitmapScaled(int maxSize) throws NetworkException
    {
        InputStream inputStream = null;
        BitmapFactory.Options options;
        int resizeScale = 1;

        try
        {
            options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            inputStream = execute(mRequest);
            BitmapFactory.decodeStream(inputStream, null, options);
            close(inputStream);

            if (options.outWidth > maxSize || options.outHeight > maxSize)
            {
                resizeScale = (int) Math.pow(2, (int) Math.round(Math.log(maxSize / (double) Math.max(options.outHeight, options.outWidth)) / Math.log(0.5)));
            }

            options = new BitmapFactory.Options();
            options.inSampleSize = resizeScale;

            inputStream = execute(mRequest);
            return BitmapFactory.decodeStream(inputStream, null, options);
        }
        finally
        {
            close(inputStream);
        }
    }

    public JSONArray getJsonArray() throws NetworkException
    {
        try
        {
            return new JSONArray(getString());
        }
        catch (JSONException jsone)
        {
            jsone.printStackTrace();
        }

        return null;
    }

    public JSONObject getJsonObject() throws NetworkException
    {
        try
        {
            return new JSONObject(getString());
        }
        catch (JSONException jsone)
        {
            jsone.printStackTrace();
        }

        return null;
    }

    public static RequestData getLastRequest()
    {
        return mLastRequest;
    }

    public String getString() throws NetworkException
    {
        return read(execute(mRequest));
    }

    public InputStream getStream() throws NetworkException
    {
        return execute(mRequest);
    }

    public NetworkConnection hideErrors()
    {
        mRequest.setHideErrors(true);
        return this;
    }

    private String read(InputStream inputStream)
    {
        StringBuffer responseBuffer = new StringBuffer();
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;

        if (inputStream == null)
        {
            return null;
        }

        try
        {
            inputStreamReader = new InputStreamReader(inputStream);
            bufferedReader = new BufferedReader(inputStreamReader);

            String lineBuffer;

            while ((lineBuffer = bufferedReader.readLine()) != null)
            {
                responseBuffer.append(lineBuffer);
            }
        }
        catch (IOException ioe)
        {
            Log.e(TAG, "Unable to read from obtained stream", ioe);
        }
        finally
        {
            close(bufferedReader);
            close(inputStreamReader);
            close(inputStream);
        }

        return responseBuffer.toString();
    }

    private void updateAddressWithArgs(RequestData request) throws IOException
    {
        StringBuilder addressBuffer = new StringBuilder(request.getAddress());

        boolean first = true;
        for (Map.Entry<String, Object> entry : request.getArgs().entrySet())
        {
            addressBuffer.append(first ? "?" : "&");
            addressBuffer.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            addressBuffer.append('=');
            addressBuffer.append(URLEncoder.encode(String.valueOf(entry.getValue()), "UTF-8"));

            first = false;
        }

        request.setAddress(addressBuffer.toString());
    }

    private void writePostBody(RequestData request, OutputStream outputStream) throws IOException
    {
        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");

        boolean first = true;
        for (Map.Entry<String, Object> entry : request.getPostData().entrySet())
        {
            if (!first)
            {
                writer.append('&');
            }

            writer.write(URLEncoder.encode(entry.getKey(), "UTF-8"));
            writer.write('=');
            writer.write(URLEncoder.encode(String.valueOf(entry.getValue()), "UTF-8"));

            first = false;
        }

        close(writer);
    }

    static class FlushedInputStream extends FilterInputStream
    {
        public FlushedInputStream(InputStream inputStream)
        {
            super(inputStream);
        }

        @Override
        public long skip(long n) throws IOException
        {
            long totalBytesSkipped = 0L;
            while (totalBytesSkipped < n)
            {
                long bytesSkipped = in.skip(n - totalBytesSkipped);
                if (bytesSkipped == 0L)
                {
                    int b = read();
                    if (b < 0)
                    {
                        break;
                    }
                    else
                    {
                        bytesSkipped = 1;
                    }
                }
                totalBytesSkipped += bytesSkipped;
            }
            return totalBytesSkipped;
        }
    }
}
