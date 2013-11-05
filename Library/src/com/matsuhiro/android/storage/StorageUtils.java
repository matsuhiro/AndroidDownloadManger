
package com.matsuhiro.android.storage;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.matsuhiro.android.download.DownloadTask;

import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.Log;

public class StorageUtils {
	
	private static final String TAG = DownloadTask.class.getSimpleName();

	@SuppressWarnings("deprecation")
	public static long getAvailableStorage(String storageDirectory) {

        try {
            StatFs stat = new StatFs(storageDirectory);
            long avaliableSize = ((long) stat.getAvailableBlocks() * (long) stat.getBlockSize());
            return avaliableSize;
        } catch (RuntimeException ex) {
            return 0;
        }
    }
	
	/* method getStorageDirectories() from Stack Overflow:
	 * 
	 * http://stackoverflow.com/a/18871043/1865860
	 * 
	 * Q: http://stackoverflow.com/users/290163/romulus-urakagi-tsai
	 * A: http://stackoverflow.com/users/2791190/dmitriy-lozenko
	 */
	private static final Pattern DIR_SEP = Pattern.compile("/");

    /**
     * Raturns all available SD-Cards in the system (include emulated)
     *
     * Warning: Hack! Based on Android source code of version 4.3 (API 18)
     * Because there is no standart way to get it.
     * TODO: Test on future Android versions 4.4+
     *
     * @return paths to all available SD-Cards in the system (include emulated)
     */
    public static String[] getStorageDirectories() {
        // Final set of paths
        final Set<String> rv = new HashSet<String>();
        // Primary physical SD-CARD (not emulated)
        final String rawExternalStorage = System.getenv("EXTERNAL_STORAGE");
        // All Secondary SD-CARDs (all exclude primary) separated by ":"
        final String rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE");
        // Primary emulated SD-CARD
        final String rawEmulatedStorageTarget = System.getenv("EMULATED_STORAGE_TARGET");
        if(TextUtils.isEmpty(rawEmulatedStorageTarget)) {
            // Device has physical external storage; use plain paths.
            if(TextUtils.isEmpty(rawExternalStorage)) {
                // EXTERNAL_STORAGE undefined; falling back to default.
                rv.add("/storage/sdcard0");
            } else {
                rv.add(rawExternalStorage);
            }
        } else {
            // Device has emulated storage; external storage paths should have
            // userId burned into them.
            final String rawUserId;
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                rawUserId = "";
            } else {
                final String path = Environment.getExternalStorageDirectory().getAbsolutePath();
                final String[] folders = DIR_SEP.split(path);
                final String lastFolder = folders[folders.length - 1];
                boolean isDigit = false;
                try {
                    Integer.valueOf(lastFolder);
                    isDigit = true;
                } catch(NumberFormatException ignored) {
                	// 
                }
                rawUserId = isDigit ? lastFolder : "";
            }
            // /storage/emulated/0[1,2,...]
            if(TextUtils.isEmpty(rawUserId)) {
                rv.add(rawEmulatedStorageTarget);
            } else {
                rv.add(rawEmulatedStorageTarget + File.separator + rawUserId);
            }
        }
        // Add all secondary storages
        if(!TextUtils.isEmpty(rawSecondaryStoragesStr)) {
            // All Secondary SD-CARDs splited into array
            final String[] rawSecondaryStorages = rawSecondaryStoragesStr.split(File.pathSeparator);
            Collections.addAll(rv, rawSecondaryStorages);
        }
        return rv.toArray(new String[rv.size()]);
    }
    
	private static boolean matchFound(String text, String regEx) {
		Pattern pattern = Pattern.compile(regEx);
		Matcher matcher = pattern.matcher(text);
		if (matcher.find()) return true;
		return false;
	}
	
	public static String[] getAlternateStorageDirectories() {
		return new String[] {
				"/mnt/sdcard",
				"/emmc",
				"/mnt/sdcard/external_sd",
				"/mnt/external_sd",
				"/sdcard/sd",
				"/mnt/sdcard/bpemmctest",
				"/mnt/sdcard/_ExternalSD",
				"/mnt/sdcard-ext",
				"/mnt/Removable/MicroSD",
				"/Removable/MicroSD",
				"/mnt/external1",
				"/mnt/extSdCard",
				"/mnt/extsd",
				"/mnt/usb_storage",
				"/extSdCard",
				"/mnt/extSdCard",
				"/mnt/UsbDriveA",
				"/mnt/UsbDriveB", };
	}
	
	public static String findStoragePathForGivenFile(File file) {
		
		String path = file.getAbsolutePath();
		
		String[] storages = getStorageDirectories();		
		for (int i = 0; i < storages.length; i++) {
			if (matchFound(path, storages[i])) {
				Log.d(TAG, "storageInUse: " + storages[i]);
				return storages[i];
			}
		}
		
		String[] alt_storages = getAlternateStorageDirectories();		
		for (int i = 0; i < alt_storages.length; i++) {
			if (matchFound(path, alt_storages[i])) {
				Log.d(TAG, "storageInUse: " + alt_storages[i]);
				return alt_storages[i];
			}
		}
		
		return Environment.getExternalStorageDirectory().getPath();
	}
}
