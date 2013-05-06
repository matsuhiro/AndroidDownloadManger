
package com.matsuhiro.android.download;

public class DownloadException extends Exception {
    private static final long serialVersionUID = 1L;

    private String mExtra;

    public DownloadException(String message) {
        super(message);
    }

    public DownloadException(String message, String extra) {
        super(message);
        mExtra = extra;
    }

    public String getExtra() {
        return mExtra;
    }
}
