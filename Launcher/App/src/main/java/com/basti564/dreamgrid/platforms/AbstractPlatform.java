package com.basti564.dreamgrid.platforms;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;

import androidx.core.content.res.ResourcesCompat;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

public abstract class AbstractPlatform {

    protected static final HashMap<String, Drawable> cachedIcons = new HashMap<>();
    protected static final HashSet<String> excludedIconPackages = new HashSet<>();

    private static final String ICONS1_URL = "https://raw.githubusercontent.com/basti564/LauncherIcons/main/oculus_square/";
    private static final String ICONS2_URL = "https://raw.githubusercontent.com/basti564/LauncherIcons/main/pico_square/";

    public static void clearIconCache() {
        excludedIconPackages.clear();
        cachedIcons.clear();
    }

    public static AbstractPlatform getPlatform(ApplicationInfo applicationInfo) {
        if (applicationInfo.packageName.startsWith(PSPPlatform.PACKAGE_PREFIX)) {
            return new PSPPlatform();
        } else if (isVirtualRealityApp(applicationInfo)) {
            return new VRPlatform();
        } else {
            return new AndroidPlatform();
        }
    }

    public static File packageToPath(Context context, String packageName) {
        return new File(context.getApplicationInfo().dataDir, packageName + ".jpg");
    }

    public static boolean updateIcon(ImageView iconView, File file, String packageName) {
        try {
            Drawable newIconDrawable = Drawable.createFromPath(file.getAbsolutePath());
            if (newIconDrawable != null) {
                iconView.setImageDrawable(newIconDrawable);
                cachedIcons.put(packageName, newIconDrawable);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    protected static boolean saveStream(InputStream inputStream, File outputFile) {
        try {
            DataInputStream dataInputStream = new DataInputStream(inputStream);

            int length;
            byte[] buffer = new byte[65536];
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
            while ((length = dataInputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, length);
            }
            fileOutputStream.flush();
            fileOutputStream.close();

            if (outputFile.length() >= 64 * 1024) {
                Bitmap bitmap = BitmapFactory.decodeFile(outputFile.getAbsolutePath());
                if (bitmap != null) {
                    try {
                        fileOutputStream = new FileOutputStream(outputFile);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream);
                        fileOutputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isMagicLeapHeadset() {
        String manufacturer = Build.MANUFACTURER.toUpperCase();
        return manufacturer.startsWith("MAGIC LEAP");
    }

    public static boolean isOculusHeadset() {
        String manufacturer = Build.MANUFACTURER.toUpperCase();
        return manufacturer.startsWith("META") || manufacturer.startsWith("OCULUS");
    }

    public static boolean isPicoHeadset() {
        String manufacturer = Build.MANUFACTURER.toUpperCase();
        return manufacturer.startsWith("PICO") || manufacturer.startsWith("PİCO"); // PİCO on turkish systems
    }

    protected static boolean isVirtualRealityApp(ApplicationInfo applicationInfo) {
        if (applicationInfo.metaData != null) {
            for (String key : applicationInfo.metaData.keySet()) {
                if (key.startsWith("notch.config")) {
                    return true;
                }
                if (key.startsWith("com.oculus")) {
                    return true;
                }
                if (key.startsWith("pvr.")) {
                    return true;
                }
                if (key.contains("vr.application.mode")) {
                    return true;
                }
            }
        }
        return false;
    }

    public abstract ArrayList<ApplicationInfo> getInstalledApps(Context context);

    public abstract boolean isSupported(Context context);

    public void loadIcon(Activity activity, ImageView iconView, ApplicationInfo appInfo) {
        PackageManager packageManager = activity.getPackageManager();
        Resources resources;
        try {
            resources = packageManager.getResourcesForApplication(appInfo.packageName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return;
        }

        int iconId = appInfo.icon;
        if (iconId == 0) {
            iconId = android.R.drawable.sym_def_app_icon;
        }

        Drawable appIcon = ResourcesCompat.getDrawableForDensity(resources, iconId, DisplayMetrics.DENSITY_XXXHIGH, null);
        iconView.setImageDrawable(appIcon);

        final File iconFile = packageToPath(activity, appInfo.packageName);

        if (iconFile.exists()) {
            AbstractPlatform.updateIcon(iconView, iconFile, appInfo.packageName);
            return;
        }

        if (cachedIcons.containsKey(appInfo.packageName)) {
            iconView.setImageDrawable(cachedIcons.get(appInfo.packageName));
            return;
        }

        downloadIcon(activity, appInfo.packageName, () -> {
            if (updateIcon(iconView, iconFile, appInfo.packageName)) {
                cachedIcons.put(appInfo.packageName, iconView.getDrawable());
            }
        });
    }

    private void downloadIcon(Activity activity, String pkgName, Runnable callback) {
        final File iconFile = packageToPath(activity, pkgName);
        new Thread(() -> {
            try {
                String url = ICONS1_URL + pkgName + ".jpg";
                if (downloadIconFromUrl(url, iconFile)) {
                    activity.runOnUiThread(callback);
                } else {
                    url = ICONS2_URL + pkgName.toLowerCase(Locale.US) + ".png";
                    if (downloadIconFromUrl(url, iconFile)) {
                        activity.runOnUiThread(callback);
                    } else {
                        Log.d("Missing icon", iconFile.getName());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public abstract void runApp(Context context, ApplicationInfo applicationInfo, boolean multiwindow);

    boolean downloadIconFromUrl(String url, File iconFile) {
        try {
            try (InputStream inputStream = new URL(url).openStream()) {
                if (saveStream(inputStream, iconFile)) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
