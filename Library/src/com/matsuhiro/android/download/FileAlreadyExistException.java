
package com.matsuhiro.android.download;

public class FileAlreadyExistException extends DownloadException {

    private static final long serialVersionUID = 1L;

    public FileAlreadyExistException(String message) {

        super(message);
    }

}
