/**
 *
 * CobaltWebLayerFragment
 * Cobalt
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Cobaltians
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package org.cobaltians.cobalt.fragments;

import org.cobaltians.cobalt.Cobalt;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.cobaltians.cobalt.activities.CobaltActivity;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Special {@link CobaltFragment} presented over the current CobaltFragment as a Web layer.
 * @author Diane
 * @details This class should not be instantiated directly. {@link CobaltFragment} manages it directly with Web layer messages.
 */
public class CobaltWebLayerFragment extends CobaltFragment {

    protected static final String TAG = CobaltWebLayerFragment.class.getSimpleName();

	private JSONObject mData;
	private CobaltFragment mRootFragment;

	/***********************************************************************************************
	 *
	 * LIFECYCLE
	 *
	 **********************************************************************************************/

	@Override
	public void onDestroy() {
		onDismiss();
		
		super.onDestroy();
	}

	/***********************************************************************************************
	 *
	 * MEMBERS
	 *
	 **********************************************************************************************/

	void setRootFragment(CobaltFragment fragment) {
		mRootFragment = fragment;
	}

	/***********************************************************************************************
	 *
	 * LIFECYCLE HELPERS
	 *
	 **********************************************************************************************/

	protected void setWebViewSettings(CobaltFragment javascriptInterface) {
		super.setWebViewSettings(javascriptInterface);

		WebViewClient webViewClient = new WebViewClient() {

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				super.onPageStarted(view, url, favicon);

				if (mRootFragment != null) {
					mRootFragment.sendEvent(Cobalt.JSEventonWebLayerLoading, null, null);
				}
				else if (Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - onPageStarted: no root fragment found");
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);

				if (mRootFragment != null) {
					mRootFragment.sendEvent(Cobalt.JSEventonWebLayerLoaded, null, null);
				}
				else if (Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - onPageFinished: no root fragment found");

				executeToJSWaitingCalls();
			}
		};

		mWebView.setWebViewClient(webViewClient);
	}

	/***********************************************************************************************
	 *
	 * COBALT
	 *
	 **********************************************************************************************/

	@Override
	protected boolean onUnhandledCallback(String name, JSONObject data) {
		return false;		
	}

	@Override
	protected boolean onUnhandledEvent(String name, JSONObject data, String callback) {
		return false;
	}
	
	@Override
	protected boolean onUnhandledMessage(final JSONObject message) {
        return false;
    }
	
	@Override
	protected void onBackPressed(boolean allowedToBack) {
		if (allowedToBack) {
            if (Cobalt.DEBUG) Log.i(Cobalt.TAG, TAG + " - onBackPressed: onBackPressed event allowed by Web view");
			dismissWebLayer(null);
		}
        else if (Cobalt.DEBUG) Log.i(Cobalt.TAG, TAG + " - onBackPressed: onBackPressed event denied by Web view");
	}

	/***********************************************************************************************
	 *
	 * DISMISS
	 *
	 **********************************************************************************************/

	protected void dismissWebLayer(JSONObject jsonObject) {
		if (getActivity() != null) {
			FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
			
			if (jsonObject != null) {
				mData = jsonObject.optJSONObject(Cobalt.kJSData);
				double fadeDuration = jsonObject.optDouble(Cobalt.kJSWebLayerFadeDuration, 0);
				
				if (fadeDuration > 0) {
                    fragmentTransaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
													        android.R.anim.fade_in, android.R.anim.fade_out);
				}
				else {
                    fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_NONE);
				}
			}
			else {
                fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_NONE);
			}

            fragmentTransaction.remove(this);
			if (allowFragmentCommit()) {
				fragmentTransaction.commit();
			}

			if (CobaltActivity.class.isAssignableFrom(mContext.getClass())) {
				CobaltActivity activity = (CobaltActivity) mContext;
				View webLayerFragmentContainer = activity.findViewById(activity.getWebLayerFragmentContainerId());
				webLayerFragmentContainer.setVisibility(View.GONE);
			}
		}
		else if (Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - dismissWebLayer: Web layer is not attached to an activity.");
	}

	private void onDismiss() {
		if (mRootFragment != null) {
			try {
				JSONObject jsonObj = new JSONObject();
				jsonObj.put(Cobalt.kJSPage, getPage());
				jsonObj.put(Cobalt.kJSData, mData);
				mRootFragment.sendEvent(Cobalt.JSEventonWebLayerDismissed, jsonObj, null);
			}
			catch (JSONException exception) {
				if(Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - onDismiss: JSONException");
				exception.printStackTrace();
			}
		}
		else if (Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - onDismiss: no root fragment found");
	}
}
