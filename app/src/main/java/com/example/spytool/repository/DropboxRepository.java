package com.example.spytool.repository;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class DropboxRepository {

    private static final String TAG = "DropboxRepository";
    private static final String ACCESS_TOKEN = "sl.u.AGItYv4UJ1k1YIIyW3xgAe0g4ELmQfoGBJSG_ByB-gYYlc7CDfW_mdkpU5wxEjQ2WX-u1yVsnDHjktVYTwHY7xJzjTqNbVz1-C7QvxsCeayp7KuO9ZPpwRUGYiBgZkR7B2Y9V7152nJvjtKcbTfIAnsyoDayQasi_bQMfWAFEl4Ml5iQglUHtLLPUZo6g3GMXtPSF27KrGjfbPdRITiqzN_OWCnK6xU1Hwu81spiw-QhBsLMyETGcrRCg5etnItsgxlYCXzsN4YIKtVStqiw_oT2xi9eF9hvxaGMXtjQZMDOzRPrF0ANV9WW7uqo9OFNtFzm5sgvnSXeesorJUAyJyRENjnECCzj_-3hvgbme-8tI9bESQ1WAIx07jI8eR_cnR5Uc1G88wpViLdt9XVaw6yobAcA24qy6btmoe8DHPMg6IjaQrsDpX7VbhIj_Rv9hEfSrRAK9PrIkswVtC1WKdW0E8rwh3ACfSLVo1vimCCySK6fg2t2OHzh0gWu_lz31J5H78cFdg7IorLyZTJCAX1hH_uIJizUK31FLcqrAnsvfb3KUP0I8fh9twZvif8V6_4-y4FfifLi05mnbBTCou1WeKYxVdmcx0Ocrk75o-QFFkribbxFvgKbemtbAsLnIuMRz90jJcHGTEDGjV7IYbu-895nhw5MTjYZyz-E_4lDt5g8qYzW5idz84NVKPHvg24tdL5ZviNDSjnjjW-up46FvZwH5g6U8-6UtAtqCUa_kWYmAsHCpBYujaACesS0nrxZKFa5ZC-IVZZ8BqKMfdskMEmvlOq67PeA2QiaoWfGKWXM-yGVbaxnBr929oujxZRUs4Xd_gEBB03yHnuYI-mJC7GFUx3E4_kudddmRDXIXjEiFr9BmJ3wSljxqXLGjnWvYarGHkl--axlr07_cgCv-gnoackXikyAiH4ges4BSpmhUJ6zEKJhXsglNXVMXpFh9zQCTgXdoEb_bAvutBGeOU4DpP_qcoG8TFF2sW0owJx39pY-vBJ2WE-cP2K3xPrj_6KCUtvIWGpN5G4XKngr6SW7s3y4Sf4hu3CLPr0hh_QsZjtGbuyIL6krNzweELsQqHyMEG0rwrh_MG9TpEqgRuY4-te0InsDzrwr5cDh51ZBEMHcisu6baY1armdz_C0WzBCNVHI1dBmoo3yybUno0r_p_rVs0I6f5cyPEwn86dWlvNxF_gB6gZ0We7aKi-O85dEr8zK-vMjmJWuwj4Sl-I2HJ3r17N1-DiuprSLWjs0y9P5kb38uKkH4r6qEFgkldWdsIwBWzSAPpve4lG4G9AD32p2gL5WIAU_BJSzBXQpHknPaPTVuPSsmyjiCZdkfehfHr3aFmk8xCzz_rVUnv03NZJaFPm3SpG2ZO57u_6Avd7PRwN-i4ATsyGJl9bLsRSKac5l_LlRmYwe9ZkNXVKNLKmm8QnslSB5HObkBA";

    private final DbxClientV2 client;

    public interface UploadCallback {
        void onSuccess(String dropboxPath);
        void onError(Exception e);
    }

    public interface DownloadCallback {
        void onSuccess(Bitmap bitmap);
        void onError(Exception e);
    }

    public DropboxRepository() {
        DbxRequestConfig config = DbxRequestConfig.newBuilder("SecurityChat/1.0").build();
        client = new DbxClientV2(config, ACCESS_TOKEN);
        Log.d(TAG, "Dropbox client initialized");
    }

    public void uploadImage(Bitmap bitmap, String fileName, UploadCallback callback) {
        Log.d(TAG, "Starting upload: " + fileName);
        new Thread(() -> {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                InputStream inputStream = new ByteArrayInputStream(baos.toByteArray());

                String dropboxPath = "/" + fileName;
                Log.d(TAG, "Uploading to path: " + dropboxPath);

                FileMetadata metadata = client.files()
                        .uploadBuilder(dropboxPath)
                        .withMode(WriteMode.OVERWRITE)
                        .uploadAndFinish(inputStream);

                Log.d(TAG, "Upload successful: " + metadata.getPathLower());
                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(metadata.getPathLower()));
            } catch (Exception e) {
                Log.e(TAG, "Upload failed", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e));
            }
        }).start();
    }

    public void downloadImage(String dropboxPath, DownloadCallback callback) {
        Log.d(TAG, "Starting download: " + dropboxPath);
        new Thread(() -> {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                client.files().download(dropboxPath).download(baos);
                byte[] data = baos.toByteArray();
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                Log.d(TAG, "Download successful: " + dropboxPath);
                new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(bitmap));
            } catch (Exception e) {
                Log.e(TAG, "Download failed", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e));
            }
        }).start();
    }
}
