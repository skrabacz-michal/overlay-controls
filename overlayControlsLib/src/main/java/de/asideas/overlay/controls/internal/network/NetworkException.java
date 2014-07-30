package de.asideas.overlay.controls.internal.network;

public class NetworkException extends Exception
{
    private static final long serialVersionUID = -1377375174325123811L;

    private int status;

    private String errorResponse;

    private Throwable exception;

    private RequestData request;

    public NetworkException(Throwable exception)
    {
        this.exception = exception;
    }

    public NetworkException(RequestData request, int status, String errorResponse, Throwable exception)
    {
        this.status = status;
        this.errorResponse = errorResponse;
        this.exception = exception;
        this.setRequest(request);
    }

    public String getErrorResponse()
    {
        return errorResponse;
    }

    public Throwable getException()
    {
        return exception;
    }

    public RequestData getRequest()
    {
        return request;
    }

    public int getStatus()
    {
        return status;
    }

    public void setErrorResponse(String mErrorResponse)
    {
        this.errorResponse = mErrorResponse;
    }

    public void setException(Exception mException)
    {
        this.exception = mException;
    }

    public void setRequest(RequestData request)
    {
        this.request = request;
    }

    public void setStatus(int mStatus)
    {
        this.status = mStatus;
    }

}