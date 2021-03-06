package com.mobiledevpro.facebook;

import android.app.Application;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import java.io.File;

/**
 * Interface for Facebook helper
 * <p>
 * Created by Dmitriy V. Chernysh on 07.12.17.
 * <p>
 * https://fb.com/mobiledevpro/
 * https://github.com/dmitriy-chernysh
 * #MobileDevPro
 */

public interface IFBLoginShareHelper {

    interface IFBLoginResultCallbacks {
        void onSuccess(String accessToken, String userOrPageName, String userOrPageId);

        void onFail(String message);
    }

    interface IFBVideoUploadResultCallbacks {
        void onSuccess(String postUrl);

        void onFail(String message);
    }

    void init(Application app, String appID);

    void loginAsPageAdmin(Fragment fragment, @NonNull IFBLoginResultCallbacks outCallbacks);

    void onLoginResult(int requestCode, int resultCode, Intent data);

    void logout();

    void uploadVideoToPageAsync(String accessToken,
                                String userOrPageId,
                                @NonNull File localVideoFile,
                                String videoTitle,
                                String videoDescription,
                                @NonNull IFBVideoUploadResultCallbacks callbacks);
}
