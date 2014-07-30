package de.asideas.overlay.controls.internal.network;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

public class RequestData
{
    public static final int GET = 0;

    public static final int POST = 1;

    private int method;

    private String address;

    private String originalAddress;

    private URL url;

    private Map<String, Object> args;

    private Map<String, Object> cookies;

    private Map<String, Object> postData;

    private boolean hideErrors;

    public RequestData()
    {
        args = new LinkedHashMap<String, Object>();
        postData = new LinkedHashMap<String, Object>();
        cookies = new LinkedHashMap<String, Object>();
    }

    public RequestData(int method, String address)
    {
        this();
        this.method = method;
        this.address = address;
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
        this.url = null;
    }

    public String getOriginalAddress()
    {
        return originalAddress;
    }

    public void setOriginalAddress(String originalAddress)
    {
        if (this.originalAddress != null)
        {
            this.originalAddress = originalAddress;
        }
    }

    public URL getUrl()
    {
        if (url == null && address != null)
        {
            try
            {
                url = new URL(address);
            }
            catch (MalformedURLException e)
            {
            }
        }

        return url;
    }

    public void setUrl(URL url)
    {
        this.url = url;
    }

    public Map<String, Object> getArgs()
    {
        return args;
    }

    public void setArgs(Map<String, Object> args)
    {
        this.args = args;
    }

    public int getMethod()
    {
        return method;
    }

    public void setMethod(int method)
    {
        this.method = method;
    }

    public String getMethodName()
    {
        return method == GET ? "GET" : "POST";
    }

    public Map<String, Object> getCookies()
    {
        return cookies;
    }

    public void setCookies(Map<String, Object> cookies)
    {
        this.cookies = cookies;
    }

    public Map<String, Object> getPostData()
    {
        return postData;
    }

    public void setPostData(Map<String, Object> postData)
    {
        this.postData = postData;
    }

    public boolean isHideErrors()
    {
        return hideErrors;
    }

    public void setHideErrors(boolean hideErrors)
    {
        this.hideErrors = hideErrors;
    }

}