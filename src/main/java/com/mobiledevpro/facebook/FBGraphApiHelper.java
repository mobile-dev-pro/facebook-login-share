package com.mobiledevpro.facebook;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Helper class for Graph API
 * <p>
 * Created by Dmitriy V. Chernysh on 08.12.17.
 * <p>
 * https://fb.com/mobiledevpro/
 * https://github.com/dmitriy-chernysh
 * #MobileDevPro
 */

class FBGraphApiHelper {

    /**
     * Show list of Facebook Accounts/Pages where current user is admin
     *
     * @param userAccessToken Current user access token
     */
    static void showAccountChooser(final Context context,
                                   AccessToken userAccessToken,
                                   @NonNull final IFBLoginShareHelper.IFBLoginResultCallbacks loginResultCallbacks) {
        GraphRequest request = GraphRequest.newGraphPathRequest(
                userAccessToken,
                "/me/accounts",
                new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse response) {
                        //parse json
                        ArrayList<FBAccount> pagesList = parseAccountsList(response.getJSONObject());
                        if (pagesList == null || pagesList.isEmpty()) {
                            // "You have no Facebook Pages where you are admin. Please, create at least one Page."
                            loginResultCallbacks.onFail(context.getResources().getString(R.string.message_pages_list_empty));
                            return;
                        }

                        //show pages list chooser dialog
                        showAccountChooserDialog(context, pagesList, loginResultCallbacks);
                    }
                });

        request.executeAsync();
    }


    /**
     * Upload video to Facebook
     *
     * @param accessToken      AccessToken
     * @param userOrPageId     User's or Page's ID
     * @param videoFile        Local Video File
     * @param videoTitle       Video Title
     * @param videoDescription Video Description
     * @param callbacks        IFBVideoUploadResultCallbacks
     */
    static void uploadVideoAsync(AccessToken accessToken,
                                 String userOrPageId,
                                 @NonNull File videoFile,
                                 String videoTitle,
                                 String videoDescription,
                                 @NonNull final IFBLoginShareHelper.IFBVideoUploadResultCallbacks callbacks) {
        final GraphRequest request = GraphRequest.newPostRequest(
                accessToken,
                "/" + userOrPageId + "/videos",
                null,
                new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse response) {
                        //Success: {Response:  responseCode: 200, graphObject: {"id":"199207940626593"}, error: null}
                        FBResponse fbResponse = parseUploadResponse(response);
                        if (fbResponse.isSuccess()) {
                            callbacks.onSuccess();
                        } else {
                            callbacks.onFail(fbResponse.getMessage());
                        }
                    }
                }
        );

        Bundle params = request.getParameters();
        try {
            final byte[] fileData = readBytes(videoFile);
            params.putByteArray(videoFile.getName(), fileData);
            params.putString("title", videoTitle);
            params.putString("description", videoDescription);
            request.setParameters(params);
            request.executeAsync();
        } catch (Exception e) {
            Log.e(FBGraphApiHelper.class.getSimpleName(), "FBGraphApiHelper.uploadVideoAsync: Exception - " + e.getLocalizedMessage(), e);
            callbacks.onFail("Facebook upload video error: " + e.getLocalizedMessage());
        }
    }

    /**
     * Parse accounts/pages list
     *
     * @param jsonObject JSON object from Graph Api response
     * @return List of FBAccount
     */
    private static ArrayList<FBAccount> parseAccountsList(JSONObject jsonObject) {
        ArrayList<FBAccount> pagesList = new ArrayList<>();

        try {
            JSONArray jsonArray = jsonObject.getJSONArray("data");
            JSONObject pageObject;
            FBAccount fbAccount;
            for (int i = 0, j = jsonArray.length(); i < j; i++) {
                pageObject = jsonArray.getJSONObject(i);

                fbAccount = new FBAccount(
                        pageObject.getString("id"),
                        pageObject.getString("name"),
                        pageObject.getString("access_token")
                );
                pagesList.add(fbAccount);
            }

        } catch (JSONException e) {
            Log.e(FBLoginShareHelper.class.getSimpleName(), "FBLoginShareHelper.parseAccountsList: JSONException - " + e.getLocalizedMessage(), e);
        }

        return pagesList;
    }

    /**
     * Parse upload response
     *
     * @param graphResponse GraphResponse
     * @return FBResponse
     */
    private static FBResponse parseUploadResponse(GraphResponse graphResponse) {
        FBResponse fbResponse;

        String response = graphResponse.toString();
        response = response.replaceAll("Response:", "");
        try {
            JSONObject jsonObject = new JSONObject(response);
            int respCode = jsonObject.getInt("responseCode");
            String respMessage = jsonObject.getString("error");
            fbResponse = new FBResponse(respCode, respMessage);

        } catch (JSONException e) {
            Log.e(FBLoginShareHelper.class.getSimpleName(), "FBLoginShareHelper.parseUploadResponse: JSONException - " + e.getLocalizedMessage(), e);
            fbResponse = new FBResponse(400, "Facebook upload exception: " + e.getLocalizedMessage());
        }

        return fbResponse;
    }

    /**
     * Show chooser dialog
     *
     * @param context      Activity context
     * @param accountsList Accounts/Pages list
     */
    private static void showAccountChooserDialog(Context context,
                                                 final ArrayList<FBAccount> accountsList,
                                                 @NonNull final IFBLoginShareHelper.IFBLoginResultCallbacks loginResultCallbacks) {
        ArrayList<String> namesList = new ArrayList<>();
        for (FBAccount fbAccount : accountsList) {
            namesList.add(fbAccount.getName());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.DefaultAppTheme_AlertDialog);
        builder.setTitle(context.getResources().getString(R.string.dialog_title_select_page))
                .setItems(namesList.toArray(new CharSequence[namesList.size()]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FBAccount fbAccount = accountsList.get(which);
                        loginResultCallbacks.onSuccess(
                                fbAccount.getAccessToken(),
                                fbAccount.getName(),
                                fbAccount.getId()
                        );
                    }
                })
                .setNegativeButton(context.getResources().getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.create().show();
    }

    private static byte[] readBytes(@NonNull File file) throws IOException {

        InputStream inputStream = new FileInputStream(file.getAbsoluteFile());
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        return byteBuffer.toByteArray();
    }
}
