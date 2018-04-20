/**
 *
 * CobaltFragment
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
import org.cobaltians.cobalt.R;
import org.cobaltians.cobalt.activities.CobaltActivity;
import org.cobaltians.cobalt.customviews.CobaltSwipeRefreshLayout;
import org.cobaltians.cobalt.customviews.IScrollListener;
import org.cobaltians.cobalt.customviews.OverScrollingWebView;
import org.cobaltians.cobalt.database.LocalStorageJavaScriptInterface;
import org.cobaltians.cobalt.plugin.CobaltPluginManager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.*;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link Fragment} allowing interactions between native and Web
 * 
 * @author Diane Moebs
 */
public abstract class CobaltFragment extends Fragment implements IScrollListener, SwipeRefreshLayout.OnRefreshListener {

    // TAG
    protected final static String TAG = CobaltFragment.class.getSimpleName();
	
	/*********************************************************
	 * MEMBERS
	 ********************************************************/

	protected Context mContext;

    protected ViewGroup mWebViewContainer;

	protected OverScrollingWebView mWebView;
    protected CobaltSwipeRefreshLayout mSwipeRefreshLayout;

	private ArrayList<JSONObject> mToJSWaitingCallsQueue = new ArrayList<>();
    private ArrayList<String> mFromJSWaitingCallsQueue = new ArrayList<>();
    private ArrayList<AlertDialog> mPendingAlertDialogs = new ArrayList<>();
    private boolean mActive = false;

	private boolean mPreloadOnCreate = true;
	private boolean mCobaltIsReady = false;

	private boolean mIsInfiniteScrollRefreshing = false;

	private CobaltPluginManager mPluginManager;

    private boolean mAllowCommit;


    /**************************************************************************************************
	 * LIFECYCLE
	 **************************************************************************************************/

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;

        executeFromJSWaitingCalls();
    }

    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPluginManager = CobaltPluginManager.getInstance(mContext);
        setRetainInstance(true);
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

        try {
            View view = inflater.inflate(getLayoutToInflate(), container, false);

            setUpViews(view);
            setUpListeners();

            return view;
        }
        catch (InflateException e) {
            if (Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - onCreateView: InflateException");
            e.printStackTrace();
        }

		return null;
	}	

	/**
	 * Restores Web view state.
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (mWebView != null) {
			mWebView.restoreState(savedInstanceState);
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();

        mAllowCommit = true;

		addWebView();
		preloadContent();
	}

    @Override
    public void onResume() {
        super.onResume();

        showPendingAlertDialogs();
        executeToJSWaitingCalls();

        JSONObject data = ((CobaltActivity) mContext).getDataNavigation();
        sendEvent(Cobalt.JSEventOnPageShown, data, null);
        ((CobaltActivity) mContext).setDataNavigation(null);
    }

    @Override
    public void onPause() {
        mActive = false;

        super.onPause();
    }

    @Override
	public void onStop() {
		super.onStop();
		
		// Fragment will rotate or be destroyed, so we don't preload content defined in fragment's arguments again
        mPreloadOnCreate = false;
		
		removeWebViewFromPlaceholder();
	}

    /**
     * Saves the Web view state.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        mAllowCommit = false;

        super.onSaveInstanceState(outState);
        if (mWebView != null) {
            mWebView.saveState(outState);
        }
    }

    @Override
	public void onDestroy() {
        super.onDestroy();
		
		mPluginManager.onFragmentDestroyed(mContext, this);
	}

    @Override
    public void onDetach() {
        mContext = null;
        super.onDetach();
    }

	/****************************************************************************************
	 * LIFECYCLE HELPERS
	 ***************************************************************************************/

	/**
	 * This method should be overridden in subclasses.
	 * @return Layout id inflated by this fragment
	 */
	protected int getLayoutToInflate() {
		if (isPullToRefreshActive()) return R.layout.fragment_refresh_cobalt;
        else return R.layout.fragment_cobalt;
	}

	/**
	 * Sets up the fragment's properties according to the inflated layout.
	 * This method should be overridden in subclasses. 
	 * @param rootView: parent view
	 */
	protected void setUpViews(View rootView) {
        mWebViewContainer = ((ViewGroup) rootView.findViewById(getWebViewContainerId()));
        if (isPullToRefreshActive()) {
            mSwipeRefreshLayout = ((CobaltSwipeRefreshLayout) rootView.findViewById(getSwipeRefreshContainerId()));
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setColorSchemeResources(R.color.cobalt_blue_bright,
                        R.color.cobalt_blue_light,
                        R.color.cobalt_blue_dark,
                        R.color.cobalt_blue_light);
            }
            else if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - setUpViews: SwipeRefresh container not found!");
        }
        if (Cobalt.DEBUG && mWebViewContainer == null) Log.w(Cobalt.TAG, TAG + " - setUpViews: WebView container not found!");
	}

    protected int getWebViewContainerId() {
        return R.id.web_view_container;
    }

    protected int getSwipeRefreshContainerId() {
        return R.id.swipe_refresh_container;
    }

	/**c
	 * Sets up listeners for components inflated from the given layout and the parent view.
	 * This method should be overridden in subclasses.
	 */
	protected void setUpListeners() { }
	
	/**
	 * Called to add the Web view in the placeholder (and creates it if necessary).
	 * This method SHOULD NOT be overridden in subclasses.
	 */
	protected void addWebView() {
        if (mWebView == null) {
            mWebView = new OverScrollingWebView(mContext);
            setWebViewSettings(this);

            if (isPullToRefreshActive()
                    && mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setOnRefreshListener(this);
                mSwipeRefreshLayout.setWebView(mWebView);
            }
		}

        if (mWebViewContainer != null) {
            mWebViewContainer.addView(mWebView);
        }
	}

    protected void setWebViewSettings(CobaltFragment javascriptInterface) {
        // Fix bug #123 on android 4.2.2 for render refresh view with layer type software
        // Deactivate completely hardware acceleration. It caused some render issues when hosted in ViewPager (ScrollViews conflict)
        // See https://stackoverflow.com/questions/12266696/webviews-in-viewpager-are-not-loaded-rendered-until-page-is-shown#answer-21890834
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        else {
            mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD) mWebView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        mWebView.setScrollListener(this);
        mWebView.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_OVERLAY);

        mWebView.setBackgroundColor(Color.TRANSPARENT);

        // Enables JS
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        // Enables and setups JS local storage
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        //@deprecated since API 19. But calling this method have simply no effect for API 19+
        webSettings.setDatabasePath(mContext.getFilesDir().getParentFile().getPath() + "/databases/");

        // Enables cross-domain calls for Ajax
        allowAjax();
		
        // Enable zoom on webview if html meta tag viewport allows it too.
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            webSettings.setDisplayZoomControls(false);
        }

        // Fix some focus issues on old devices like HTC Wildfire
        // keyboard was not properly showed on input touch.
        // Removed the line below due to unfocus of native component on webLayer appearance
        //mWebView.requestFocus(View.FOCUS_DOWN);
        mWebView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_UP:
                        if (! view.hasFocus()) {
                            view.requestFocus();
                        }
                        break;
                    default:
                        break;
                }

                return false;
            }
        });
        
        //Enable Webview debugging from chrome desktop
        if (Cobalt.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        // Add JavaScript interface so JavaScript can call native functions.
        mWebView.addJavascriptInterface(javascriptInterface, "Android");
        mWebView.addJavascriptInterface(new LocalStorageJavaScriptInterface(mContext), "LocalStorage");

        WebViewClient webViewClient = new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                executeToJSWaitingCalls();
            }
        };

        mWebView.setWebViewClient(webViewClient);
    }

    @SuppressLint("NewApi")
	private void allowAjax() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            // TODO: see how to restrict only to local files
            mWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        }
    }

	private void preloadContent() {
        String page = (getPage() != null) ? getPage() : "index.html";
		
		if (mPreloadOnCreate) {
			if (page.startsWith("https://") || page.startsWith("http://")) {
                mWebView.loadUrl(page);
            } else {
                loadFileFromAssets(page);
            }
		}
	}

    /**
     * Load the given file in the Web view
     * @param file: file name to load.
     * @warning All application HTML files should be found in the same subfolder in ressource path
     */
    private void loadFileFromAssets(String file) {
        mWebView.loadUrl(Cobalt.getInstance(mContext).getResourcePath() + file);
    }

	/**
	 * Called when fragment is about to rotate or be destroyed
	 * This method SHOULD NOT be overridden in subclasses.
	 */
	private void removeWebViewFromPlaceholder() {
		if (mWebViewContainer != null) {
            mWebViewContainer.removeView(mWebView);
		}
	}
	
	/****************************************************************************************
	 * SCRIPT EXECUTION
	 ***************************************************************************************/
	// TODO: find a way to keep in the queue not sent messages
	/**
	 * Sends script to be executed by JavaScript in Web view
	 * @param jsonObj: JSONObject containing script.
	 */
    private void executeScriptInWebView(final JSONObject jsonObj) {
        if (jsonObj != null) {
            Activity activity = getActivity();
            if (mWebView != null
                    && activity != null
                    && mCobaltIsReady) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Line & paragraph separators are not JSON compliant but supported by JSONObject
                        String script = jsonObj.toString().replaceAll("[\u2028\u2029]", "");

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            // Since KitKat, messages are automatically urldecoded when received from the web. encoding them to fix this.
                            script = script.replaceAll("%", "%25");
                        }

                        String url = "javascript:cobalt.execute(" + script + ");";
                        mWebView.loadUrl(url);
                    }
                });
            }
            else {
                if (Cobalt.DEBUG) Log.i(Cobalt.TAG, TAG + " - executeScriptInWebView: adding message to queue: " + jsonObj);
                mToJSWaitingCallsQueue.add(jsonObj);
            }
        }
        else if (Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - executeScriptInWebView: jsonObj is null!");
    }

    public void executeToJSWaitingCalls() {
        ArrayList<JSONObject> toJSWaitingCallsQueue = new ArrayList<>(mToJSWaitingCallsQueue);
		int toJSWaitingCallsQueueLength = toJSWaitingCallsQueue.size();

        mToJSWaitingCallsQueue.clear();

		for (int i = 0 ; i < toJSWaitingCallsQueueLength ; i++) {
			if (Cobalt.DEBUG) Log.i(Cobalt.TAG, TAG + " - executeToJSWaitingCalls: execute " + toJSWaitingCallsQueue.get(i).toString());
			executeScriptInWebView(toJSWaitingCallsQueue.get(i));
		}
	}

	/****************************************************************************************
	 * MESSAGE SENDING
	 ***************************************************************************************/

    /**
     * Calls the Web callback with an object containing response fields
     * @param callbackId: the Web callback.
     * @param data: the object containing response fields
     */
    public void sendCallback(final String callbackId, final JSONObject data) {
        if (callbackId != null
                && callbackId.length() > 0) {
            try {
                JSONObject jsonObj = new JSONObject();
                jsonObj.put(Cobalt.kJSType, Cobalt.JSTypeCallBack);
                jsonObj.put(Cobalt.kJSCallback, callbackId);
                jsonObj.put(Cobalt.kJSData, data);
                executeScriptInWebView(jsonObj);
            }
            catch (JSONException exception) {
                if (Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - sendCallback: JSONException");
                exception.printStackTrace();
            }
        }
        else if (Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - sendCallback: callbackId is null or empty!");
    }

    /**
     * Calls the Web callback with an object containing response fields
     * @param event: the name of the event.
     * @param data: the object containing response fields
     * @param callbackID: the Web callback.
     */
    public void sendEvent(final String event, final JSONObject data, final String callbackID) {
        if (event != null
                && event.length() > 0) {
            try {
                JSONObject jsonObj = new JSONObject();
                jsonObj.put(Cobalt.kJSType, Cobalt.JSTypeEvent);
                jsonObj.put(Cobalt.kJSEvent, event);
                jsonObj.put(Cobalt.kJSData, data);
                jsonObj.put(Cobalt.kJSCallback, callbackID);
                executeScriptInWebView(jsonObj);
            }
            catch (JSONException exception) {
                if (Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - sendEvent: JSONException");
                exception.printStackTrace();
            }
        }
        else if (Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - sendEvent: event is null or empty!");
    }

    /**
     * Calls the Web callback with an object containing response fields
     * @param plugin: the name of the plugin.
     * @param data: the object containing response fields
     * @param callbackID: the Web callback.
     */
    public void sendPlugin(final String plugin, final JSONObject data, final String callbackID) {
        if (plugin != null
            && plugin.length() > 0) {
            try {
                JSONObject jsonObj = new JSONObject();
                jsonObj.put(Cobalt.kJSType, Cobalt.JSTypePlugin);
                jsonObj.put(Cobalt.kJSPluginName, plugin);
                jsonObj.put(Cobalt.kJSData, data);
                jsonObj.put(Cobalt.kJSCallback, callbackID);
                executeScriptInWebView(jsonObj);
            }
            catch (JSONException exception) {
                if (Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - sendPlugin: JSONException");
                exception.printStackTrace();
            }
        }
        else if (Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - sendPlugin: plugin is null or empty!");
    }

    /**
     * Calls the Web callback with an object containing response fields
     * @param message: the object containing response fields
     */
    public void sendMessage(final JSONObject message) {
        if (message != null) {
            executeScriptInWebView(message);
        }
        else if (Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - sendMessage: message is null !");
    }

	/****************************************************************************************
	 * MESSAGE HANDLING
	 ***************************************************************************************/
	/**
	 * This method is called when the JavaScript sends a message to the native side.
	 * This method should be overridden in subclasses.
	 * @param message : the JSON-message sent by JavaScript.
	 * @return true if the message was handled by the native, false otherwise
	 * @details some basic operations are already implemented : navigation, logs, toasts, native alerts, web alerts
	 * @details this method may be called from a secondary thread.
	 */
	// This method must be public !!!
	@JavascriptInterface
	public void onCobaltMessage(String message) {
        if (mContext == null) {
            mFromJSWaitingCallsQueue.add(message);
            return;
        }

		try {
			final JSONObject jsonObj = new JSONObject(message);

            boolean messageHandled = false;

            // TYPE
            String type = jsonObj.optString(Cobalt.kJSType, null);
			if (type != null) {
                final JSONObject data;
                final String callback;
                String action;

                switch (type) {
                    // CALLBACK
                    case Cobalt.JSTypeCallBack:
                        try {
                            String callbackId = jsonObj.getString(Cobalt.kJSCallback);
                            data = jsonObj.optJSONObject(Cobalt.kJSData);
                            messageHandled = handleCallback(callbackId, data);
                        }
                        catch(JSONException exception) {
                            if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - onCobaltMessage: " +
                                            Cobalt.kJSCallback + " field is missing.\n" + message);
                            exception.printStackTrace();
                        }
                        break;
                    // COBALT IS READY
                    case Cobalt.JSTypeCobaltIsReady:
                        onCobaltIsReady(jsonObj.optString(Cobalt.kJSVersion, null));
                        messageHandled = true;
                        break;
                    // EVENT
                    case Cobalt.JSTypeEvent:
                        try {
                            final String event = jsonObj.getString(Cobalt.kJSEvent);
                            data = jsonObj.optJSONObject(Cobalt.kJSData);
                            callback = jsonObj.optString(Cobalt.kJSCallback, null);

                            ((Activity) mContext).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    onUnhandledEvent(event, data, callback);
                                }
                            });

                            messageHandled = true;
                        }
                        catch(JSONException exception) {
                            if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - onCobaltMessage: " +
                                    Cobalt.kJSEvent + " field is missing.\n" + message);
                            exception.printStackTrace();
                        }
                        break;
                    // INTENT
                    case Cobalt.JSTypeIntent:
                        try {
                            action = jsonObj.getString(Cobalt.kJSAction);
                            // OPEN EXTERNAL URL
                            if (action.equals(Cobalt.JSActionIntentOpenExternalUrl)) {
                                data = jsonObj.getJSONObject(Cobalt.kJSData);
                                String url = data.getString(Cobalt.kJSUrl);
                                openExternalUrl(url);
                                messageHandled = true;
                            }
                        }
                        catch(JSONException exception) {
                            if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - onCobaltMessage: " +
                                    Cobalt.kJSAction + ", " + Cobalt.kJSData + " or " +
                                    Cobalt.kJSData + "." + Cobalt.kJSUrl + " field(s) is/are " +
                                    "missing.\n" + message);
                            exception.printStackTrace();
                        }
                        break;
                    // LOG
                    case Cobalt.JSTypeLog:
                        try {
                            String text = jsonObj.getString(Cobalt.kJSValue);
                            Log.d(Cobalt.TAG, "JS LOG: " + text);
                            messageHandled = true;
                        }
                        catch(JSONException exception) {
                            if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - onCobaltMessage: " +
                                    Cobalt.kJSValue + " field is missing.\n" + message);
                            exception.printStackTrace();
                        }
                        break;
                    // NAVIGATION
                    case Cobalt.JSTypeNavigation:
                        try {
                            action = jsonObj.getString(Cobalt.kJSAction);

                            String page;
                            String controller;

                            switch (action) {
                                // PUSH
                                case Cobalt.JSActionNavigationPush:
                                    try {
                                        data = jsonObj.getJSONObject(Cobalt.kJSData);
                                        push(data);
                                        messageHandled = true;
                                    }
                                    catch(JSONException exception) {
                                        if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - " +
                                                "onCobaltMessage: " + Cobalt.kJSData +
                                                " field is missing.\n" + message);
                                        exception.printStackTrace();
                                    }
                                    break;
                                // POP
                                case Cobalt.JSActionNavigationPop:
                                    data = jsonObj.optJSONObject(Cobalt.kJSData);
                                    if (data != null) {
                                        page = data.optString(Cobalt.kJSPage, null);
                                        controller = data.optString(Cobalt.kJSController, null);
                                        JSONObject dataToPop = data.optJSONObject(Cobalt.kJSData);
                                        if (page != null) {
                                            pop(controller, page, dataToPop);
                                        }
                                        else {
                                            pop(dataToPop);
                                        }
                                    }
                                    else {
                                        pop();
                                    }
                                    messageHandled = true;
                                    break;
                                // MODAL
                                case Cobalt.JSActionNavigationModal:
                                    try {
                                        data = jsonObj.getJSONObject(Cobalt.kJSData);
                                        String callbackId = jsonObj.optString(Cobalt.kJSCallback, null);
                                        presentModal(data, callbackId);
                                        messageHandled = true;
                                    }
                                    catch(JSONException exception) {
                                        if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - " +
                                                "onCobaltMessage: " + Cobalt.kJSData +
                                                " field is missing.\n" + message);
                                        exception.printStackTrace();
                                    }
                                    break;
                                // DISMISS
                                case Cobalt.JSActionNavigationDismiss:
                                    try {
                                        // TODO: not present in iOS
                                        data = jsonObj.getJSONObject(Cobalt.kJSData);
                                        controller = data.getString(Cobalt.kJSController);
                                        page = data.getString(Cobalt.kJSPage);
                                        JSONObject dataForDissmiss = data.optJSONObject(Cobalt.kJSData);
                                        dismissModal(controller, page, dataForDissmiss);
                                        messageHandled = true;
                                    }
                                    catch(JSONException exception) {
                                        if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - " +
                                                "onCobaltMessage: one or more field(s) is/are " +
                                                "missing within " + Cobalt.kJSData + ", " +
                                                Cobalt.kJSData + "." + Cobalt.kJSController +
                                                " or " + Cobalt.kJSData + "." + Cobalt.kJSPage +
                                                ".\n" + message);
                                        exception.printStackTrace();
                                    }
                                    break;
                                // REPLACE
                                case Cobalt.JSActionNavigationReplace:
                                    try {
                                        data = jsonObj.getJSONObject(Cobalt.kJSData);
                                        replace(data);
                                        messageHandled = true;
                                    }
                                    catch(JSONException exception) {
                                        if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - " +
                                                "onCobaltMessage: " + Cobalt.kJSData +
                                                " field is missing.\n" + message);
                                        exception.printStackTrace();
                                    }
                                    break;
                            }
                        }
                        catch(JSONException exception) {
                            if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - onCobaltMessage: " +
                                        Cobalt.kJSAction + " field is missing.\n" + message);
                            exception.printStackTrace();
                        }
                        break;
                    // PLUGIN
                    case Cobalt.JSTypePlugin:
                        messageHandled = mPluginManager.onMessage(mContext, this, jsonObj);
                        break;
                    // UI
                    case Cobalt.JSTypeUI:
                        try {
                            String control = jsonObj.getString(Cobalt.kJSUIControl);
                            data = jsonObj.getJSONObject(Cobalt.kJSData);
                            callback = jsonObj.optString(Cobalt.kJSCallback, null);
                            messageHandled = handleUi(control, data, callback);
                        }
                        catch(JSONException exception) {
                            if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - onCobaltMessage: " +
                                    Cobalt.kJSUIControl + " and/or " + Cobalt.kJSData +
                                    " field(s) is/are missing.\n" + message);
                            exception.printStackTrace();
                        }
                        break;
                    // WEB LAYER
                    case Cobalt.JSTypeWebLayer:
                        try {
                            action = jsonObj.getString(Cobalt.kJSAction);
                            switch (action) {
                                // SHOW
                                case Cobalt.JSActionWebLayerShow:
                                    data = jsonObj.getJSONObject(Cobalt.kJSData);
                                    showWebLayer(data);
                                    messageHandled = true;
                                    break;
                                // DISMISS
                                case Cobalt.JSActionWebLayerDismiss:
                                    if (CobaltActivity.class.isAssignableFrom(mContext.getClass())) {
                                        CobaltActivity activity = (CobaltActivity) mContext;
                                        final Fragment currentFragment = activity.getSupportFragmentManager().findFragmentById(activity.getWebLayerFragmentContainerId());
                                        if (currentFragment != null
                                                && CobaltWebLayerFragment.class.isAssignableFrom(currentFragment.getClass())) {
                                            activity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ((CobaltWebLayerFragment) currentFragment).dismissWebLayer(jsonObj);
                                                }
                                            });
                                        }
                                    }

                                    messageHandled = true;
                                    break;
                                // BRING TO FRONT
                                case Cobalt.JSActionWebLayerBringToFront:
                                    ((Activity) mContext).runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            bringWebLayerToFront();
                                        }
                                    });
                                    break;
                                // SEND TO BACK
                                case Cobalt.JSActionWebLayerSendToBack:
                                    ((Activity) mContext).runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            sendWebLayerToBack();
                                        }
                                    });
                                    break;
                            }
                        }
                        catch(JSONException exception) {
                            if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - onCobaltMessage: " +
                                    Cobalt.kJSAction + " and/or " + Cobalt.kJSData +
                                    " field(s) is/are missing.\n" + message);
                            exception.printStackTrace();
                        }
                        break;
                }
			}

            // UNHANDLED MESSAGE
            if (! messageHandled) {
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onUnhandledMessage(jsonObj);
                    }
                });
            }
		} 
		catch (JSONException exception) {
            if (Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - onCobaltMessage: Unable to parse " +
                    "message as JSON.\n" + message);
			exception.printStackTrace();
		}

        /*
        catch (NullPointerException exception) {
            if (Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - onCobaltMessage: NullPointerException");
            exception.printStackTrace();
        }
        */
	}

    private void executeFromJSWaitingCalls() {
        ArrayList<String> fromJSWaitingCallsQueue = new ArrayList<>(mFromJSWaitingCallsQueue);
        int fromJSWaitingCallsQueueLength = fromJSWaitingCallsQueue.size();

        mFromJSWaitingCallsQueue.clear();

        for (int i = 0 ; i < fromJSWaitingCallsQueueLength ; i++) {
            if (Cobalt.DEBUG) Log.i(Cobalt.TAG, TAG + " - executeFromJSWaitingCalls: execute " + fromJSWaitingCallsQueue.get(i));
            onCobaltMessage(fromJSWaitingCallsQueue.get(i));
        }
    }

	private void onCobaltIsReady(String version) {
        String androidVersion = getResources().getString(R.string.version_name);
        if (! androidVersion.equals(version)) {
            Log.w(TAG, "Cobalt version mismatch: Android Cobalt version is " + androidVersion +
                    " but Web Cobalt version is " + version + ". You should fix this. ");
        }

		if (Cobalt.DEBUG) {
            Log.i(Cobalt.TAG, TAG + " - onCobaltIsReady: version " + androidVersion);
        }

		mCobaltIsReady = true;
        executeToJSWaitingCalls();

        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onReady();
            }
        });
	}

    protected void onReady() { }

	private boolean handleCallback(final String callback, final JSONObject data) {
        switch(callback) {
            case Cobalt.JSCallbackOnBackButtonPressed:
                try {
                    onBackPressed(data.getBoolean(Cobalt.kJSValue));
                    return true;
                }
                catch (JSONException exception) {
                    if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - handleCallback " +
                            Cobalt.JSCallbackOnBackButtonPressed + ": missing value field.");
                    exception.printStackTrace();
                    return false;
                }
            case Cobalt.JSCallbackPullToRefreshDidRefresh:
                ((Activity)mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onPullToRefreshDidRefresh();
                    }
                });
                return true;
            case Cobalt.JSCallbackInfiniteScrollDidRefresh:
                ((Activity)mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onInfiniteScrollDidRefresh();
                    }
                });
                return true;
            default:
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onUnhandledCallback(callback, data);
                    }
                });
                return true;
        }
	}
	
	protected abstract boolean onUnhandledCallback(String callback, JSONObject data);
	
	protected abstract boolean onUnhandledEvent(String event, JSONObject data, String callback);

	private boolean handleUi(String control, JSONObject data, String callback) {
        boolean messageHandled = false;

        switch (control) {
            // PICKER
            case Cobalt.JSControlPicker:
                try {
                    String type = data.getString(Cobalt.kJSType);
                    // DATE
                    if (type.equals(Cobalt.JSPickerDate)) {
                        JSONObject date = data.optJSONObject(Cobalt.kJSDate);

                        Calendar calendar = Calendar.getInstance();
                        int year = calendar.get(Calendar.YEAR);
                        int month = calendar.get(Calendar.MONTH);
                        int day = calendar.get(Calendar.DAY_OF_MONTH);

                        if (date != null
                            && date.has(Cobalt.kJSYear)
                            && date.has(Cobalt.kJSMonth)
                            && date.has(Cobalt.kJSDay)) {
                            year = date.getInt(Cobalt.kJSYear);
                            month = date.getInt(Cobalt.kJSMonth) - 1;
                            day = date.getInt(Cobalt.kJSDay);
                        }

                        JSONObject texts = data.optJSONObject(Cobalt.kJSTexts);
                        String title = texts.optString(Cobalt.kJSTitle, null);
                        String clear = texts.optString(Cobalt.kJSClear, null);
                        String cancel = texts.optString(Cobalt.kJSCancel, null);
                        String validate = texts.optString(Cobalt.kJSValidate, null);

                        showDatePickerDialog(year, month, day, title, clear, cancel, validate, callback);

                        messageHandled = true;
                    }
                }
                catch(JSONException exception) {
                    if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - handleUi: " + Cobalt.kJSType +
                            " field is missing.\n" + data);
                    exception.printStackTrace();
                }
                break;
            // ALERT
            case Cobalt.JSControlAlert:
                showAlertDialog(data, callback);
                messageHandled = true;
                break;
            // TOAST
            case Cobalt.JSControlToast:
                try {
                    final String message = data.getString(Cobalt.kJSMessage);

                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
                        }
                    });

                    messageHandled = true;
                }
                catch (JSONException exception) {
                    if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - handleUi: " + Cobalt.kJSMessage +
                            " field is missing.\n" + data);
                    exception.printStackTrace();
                }
                break;
            // BARS

            case Cobalt.JSControlBars:
                try {
                    String action = data.getString(Cobalt.kJSAction);
                    switch (action) {
                        // SET BARS
                        case Cobalt.JSActionSetBars:
                            JSONObject bars = data.optJSONObject(Cobalt.kJSBars);
                            setBars(bars);
                            messageHandled = true;
                            break;
                        // SET ACTION BADGE
                        case Cobalt.JSActionSetActionBadge:
                            try {
                                final String name = data.getString(Cobalt.kActionName);
                                final String badge = data.getString(Cobalt.kActionBadge);

                                ((Activity) mContext).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((CobaltActivity) mContext).setBadgeMenuItem(name, badge);
                                    }
                                });

                                messageHandled = true;
                            }
                            catch (JSONException exception) {
                                if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - handleUi: " +
                                        Cobalt.kActionName + " and/or " + Cobalt.kActionBadge +
                                        " field(s) is/are missing.\n" + data);
                                exception.printStackTrace();
                            }
                            break;
                        // SET ACTION CONTENT
                        case Cobalt.JSActionSetActionContent:
                            try {
                                final String nameContent = data.getString(Cobalt.kActionName);
                                final JSONObject content = data.getJSONObject(Cobalt.kContent);

                                ((Activity) mContext).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((CobaltActivity) mContext).setContentMenuItem(nameContent, content);
                                    }
                                });

                                messageHandled = true;
                            }
                            catch (JSONException exception) {
                                if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - handleUi: " +
                                        Cobalt.kActionName + " and/or " + Cobalt.kContent +
                                        " field(s) is/are missing.\n" + data);
                                exception.printStackTrace();
                            }
                            break;
                        // SET BARS VISIBLE
                        case Cobalt.JSActionSetBarsVisible:
                            try {
                                final JSONObject visible = data.getJSONObject(Cobalt.kVisible);

                                ((Activity) mContext).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((CobaltActivity) mContext).setActionBarVisible(visible);
                                    }
                                });

                                messageHandled = true;
                            }
                            catch (JSONException exception) {
                                if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - handleUi: " +
                                        Cobalt.kVisible + " field is missing.\n" + data);
                                exception.printStackTrace();
                            }
                            break;
                        // SET BAR CONTENT
                        case Cobalt.JSActionSetBarContent:
                            try {
                                final JSONObject barsContent = data.getJSONObject(Cobalt.kContent);

                                ((Activity) mContext).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((CobaltActivity) mContext).setBarContent(barsContent);
                                    }
                                });

                                messageHandled = true;
                            }
                            catch (JSONException exception) {
                                if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - handleUi: " +
                                        Cobalt.kContent + " field is missing.\n" + data);
                                exception.printStackTrace();
                            }
                            break;
                        // SET ACTION VISIBLE
                        case Cobalt.JSActionSetActionVisible:
                            try {
                                final String actionName = data.getString(Cobalt.kActionName);
                                final boolean actionVisible = data.getBoolean(Cobalt.kVisible);

                                ((Activity) mContext).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((CobaltActivity) mContext).setActionItemVisible(actionName, actionVisible);
                                    }
                                });

                                messageHandled = true;
                            }
                            catch (JSONException exception) {
                                if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - handleUi: " +
                                        Cobalt.kActionName + " and/or " + Cobalt.kVisible +
                                        " field(s) is/are missing.\n" + data);
                                exception.printStackTrace();
                            }
                            break;
                        // SET ACTION ENABLED
                        case Cobalt.JSActionSetActionEnabled:
                            try {
                                final String actionNameEnabled = data.getString(Cobalt.kActionName);
                                final boolean actionEnabled = data.getBoolean(Cobalt.kEnabled);

                                ((Activity) mContext).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((CobaltActivity) mContext).setActionItemEnabled(actionNameEnabled, actionEnabled);
                                    }
                                });

                                messageHandled = true;
                            }
                            catch (JSONException exception) {
                                if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - handleUi: " +
                                        Cobalt.kActionName + " and/or " + Cobalt.kEnabled +
                                        " field(s) is/are missing.\n" + data);
                                exception.printStackTrace();
                            }
                            break;
                    }
                }
                catch(JSONException exception) {
                    if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - handleUi: " + Cobalt.kJSAction +
                            " field is missing.\n" + data);
                    exception.printStackTrace();
                }
                break;
        }

        return messageHandled;
	}

    protected void setBars(final JSONObject actionBar) {
        Intent intent = ((CobaltActivity) mContext).getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            bundle = new Bundle();
        }
        Bundle extras = bundle.getBundle(Cobalt.kExtras);
        if (extras == null) {
            extras = new Bundle();
            bundle.putBundle(Cobalt.kExtras, extras);
        }

        extras.putString(Cobalt.kBars, actionBar.toString());
        intent.putExtras(bundle);

        ((CobaltActivity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((CobaltActivity) mContext).setupBars(actionBar, CobaltFragment.this);
                ((CobaltActivity) mContext).supportInvalidateOptionsMenu();
            }
        });
    }

	protected abstract boolean onUnhandledMessage(JSONObject message);
	
	/*****************************************************************************************************************
	 * NAVIGATION
	 ****************************************************************************************************************/

	private void push(JSONObject data) {
        try {
            String page = data.getString(Cobalt.kJSPage);
            String controller = data.optString(Cobalt.kJSController, null);
            JSONObject bars = data.optJSONObject(Cobalt.kJSBars);
            JSONObject dataToPush = data.optJSONObject(Cobalt.kJSData);

            final Intent intent = Cobalt.getInstance(mContext).getIntentForController(controller, page);
            if (intent != null) {
                if (bars != null) {
                    Bundle configuration = intent.getBundleExtra(Cobalt.kExtras);
                    configuration.putString(Cobalt.kBars, bars.toString());
                }
                if (dataToPush != null) {
                    intent.putExtra(Cobalt.kJSData, dataToPush.toString());
                }

                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mContext.startActivity(intent);
                    }
                });
            }
            else if (Cobalt.DEBUG) {
                Log.e(Cobalt.TAG, TAG + " - push: unable to push " + controller + " controller.");
            }
        }
        catch(JSONException exception) {
            if (Cobalt.DEBUG) {
                Log.e(Cobalt.TAG, TAG + " - push: missing mandatory page field.");
            }

            exception.printStackTrace();
        }
	}
	
	private void pop() {
        onBackPressed(true);
    }

    private void pop(JSONObject data) {
        ((CobaltActivity) mContext).dataForPop(data);
        pop();
    }

    private void pop(String controller, String page, JSONObject data) {
        ((CobaltActivity) mContext).popTo(controller, page, data);
    }
	
	private void presentModal(JSONObject data, String callBackID) {
        try {
            String page = data.getString(Cobalt.kJSPage);
            String controller = data.optString(Cobalt.kJSController, null);
            JSONObject bars = data.optJSONObject(Cobalt.kJSBars);
            JSONObject dataForModal = data.optJSONObject(Cobalt.kJSData);

            final Intent intent = Cobalt.getInstance(mContext).getIntentForController(controller, page);
            if (intent != null) {
                intent.putExtra(Cobalt.kPushAsModal, true);
                if (bars != null) {
                    Bundle configuration = intent.getBundleExtra(Cobalt.kExtras);
                    configuration.putString(Cobalt.kBars, bars.toString());
                }
                if (dataForModal != null) {
                    intent.putExtra(Cobalt.kJSData, dataForModal.toString());
                }

                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mContext.startActivity(intent);
                    }
                });

                // Sends callback to store current activity & HTML page for dismiss
                try {
                    JSONObject callbackData = new JSONObject();
                    callbackData.put(Cobalt.kJSPage, getPage());
                    callbackData.put(Cobalt.kJSController, mContext.getClass().getName());
                    sendCallback(callBackID, callbackData);
                }
                catch (JSONException exception) {
                    exception.printStackTrace();
                }
            }
            else if (Cobalt.DEBUG) {
                Log.e(Cobalt.TAG,  TAG + " - presentModal: unable to present modal " + controller + " controller.");
            }
        }
        catch(JSONException exception) {
            if (Cobalt.DEBUG) {
                Log.e(Cobalt.TAG, TAG + " - presentModal: missing mandatory page field.");
            }

            exception.printStackTrace();
        }
	}

	private void dismissModal(String controller, String page, JSONObject dataForDissmiss) {
		try {
			Class<?> pClass = Class.forName(controller);

			// Instantiates intent only if class inherits from Activity
			if (Activity.class.isAssignableFrom(pClass)) {
				Bundle bundle = new Bundle();
				bundle.putString(Cobalt.kPage, page);

				final Intent intent = new Intent(mContext, pClass);
				intent.putExtra(Cobalt.kExtras, bundle);
                intent.putExtra(Cobalt.kPopAsModal, true);
                if (dataForDissmiss != null) {
                    intent.putExtra(Cobalt.kJSData, dataForDissmiss.toString());
                }

                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        NavUtils.navigateUpTo((Activity) mContext, intent);
                    }
                });
			}
			else if(Cobalt.DEBUG) Log.e(Cobalt.TAG,  TAG + " - dismissModal: unable to dismiss modal since " + controller + " does not inherit from Activity");
		} 
		catch (ClassNotFoundException exception) {
			if (Cobalt.DEBUG) Log.e(Cobalt.TAG,  TAG + " - dismissModal: " + controller + "not found");
			exception.printStackTrace();
		}
	}

    private void replace(JSONObject data) {
        try {
            String page = data.getString(Cobalt.kJSPage);
            String controller = data.optString(Cobalt.kJSController, null);
            JSONObject bars = data.optJSONObject(Cobalt.kJSBars);
            JSONObject dataForReplace = data.optJSONObject(Cobalt.kJSData);
            boolean animated = data.optBoolean(Cobalt.kJSAnimated);

			boolean clearHistory = data.optBoolean(Cobalt.kJSClearHistory, false);
			
            final Intent intent = Cobalt.getInstance(mContext).getIntentForController(controller, page);
            if (intent != null) {
                intent.putExtra(Cobalt.kJSAnimated, animated);
                if (bars != null) {
                    Bundle configuration = intent.getBundleExtra(Cobalt.kExtras);
                    configuration.putString(Cobalt.kBars, bars.toString());
                }
                if (dataForReplace != null) {
                    intent.putExtra(Cobalt.kJSData, dataForReplace.toString());
                }

				if (clearHistory) {
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
				}

                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mContext.startActivity(intent);
                        ((Activity) mContext).finish();
                    }
                });
            }
            else if (Cobalt.DEBUG) {
                Log.e(Cobalt.TAG,  TAG + " - replace: unable to replace " + controller + " controller.");
            }
        }
        catch(JSONException exception) {
            if (Cobalt.DEBUG) {
                Log.e(Cobalt.TAG, TAG + " - replace: missing mandatory page field.");
            }

            exception.printStackTrace();
        }
    }
	
	/**
	 * Called when onBackPressed event is fired. Asks the Web view for back permission.
	 * This method should NOT be overridden in subclasses.
	 */
	public void askWebViewForBackPermission() {
		sendEvent(Cobalt.JSEventOnBackButtonPressed, null, Cobalt.JSCallbackOnBackButtonPressed);
	}
	
	/**
	 * Called when the Web view allowed or not the onBackPressed event.
	 * @param allowedToBack:    true if the WebView allowed the onBackPressed event
     *                          false otherwise.
	 * @details if allowedToBack is true, the onBackPressed method of the activity will be called.
     * This method should not be overridden in subclasses.
	 */
	protected void onBackPressed(boolean allowedToBack) {
        if (allowedToBack) {
            ((CobaltActivity) mContext).back();
        }
        else if (Cobalt.DEBUG) Log.i(Cobalt.TAG, TAG + " - onBackPressed: denied by WebView");
	}
	
	/***********************************************************************************************
     *
	 * WEB LAYER
     *
	 **********************************************************************************************/

    /**
     * Returns a new instance of {@link CobaltWebLayerFragment}
     * @return a new instance of {@link CobaltWebLayerFragment}
     * This method may be overridden in subclasses if the {@link CobaltWebLayerFragment} must implement customized stuff.
     */
    protected CobaltWebLayerFragment getWebLayerFragment() {
        return new CobaltWebLayerFragment();
    }

	protected void showWebLayer(JSONObject data) {
        try {
            String page = data.getString(Cobalt.kJSPage);
            final double fadeDuration = data.optDouble(Cobalt.kJSWebLayerFadeDuration, 0.3);

            Bundle bundle = new Bundle();
            bundle.putString(Cobalt.kPage, page);

            final CobaltWebLayerFragment webLayerFragment = getWebLayerFragment();
            if (webLayerFragment != null) {
                webLayerFragment.setRootFragment(this);
                webLayerFragment.setArguments(bundle);

                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        FragmentTransaction fragmentTransition = ((FragmentActivity) mContext).getSupportFragmentManager().beginTransaction();

                        if (fadeDuration > 0) {
                            fragmentTransition.setCustomAnimations(android.R.anim.fade_in,
                                    android.R.anim.fade_out,
                                    android.R.anim.fade_in,
                                    android.R.anim.fade_out);
                        }
                        else {
                            fragmentTransition.setTransition(FragmentTransaction.TRANSIT_NONE);
                        }

                        if (CobaltActivity.class.isAssignableFrom(mContext.getClass())) {
                            // Dismiss current Web layer if one is already shown
                            CobaltActivity activity = (CobaltActivity) mContext;
                            Fragment currentFragment = activity.getSupportFragmentManager().findFragmentById(activity.getWebLayerFragmentContainerId());
                            if (currentFragment != null
                                && CobaltWebLayerFragment.class.isAssignableFrom(currentFragment.getClass())) {
                                ((CobaltWebLayerFragment) currentFragment).dismissWebLayer(null);
                            }

                            // Shows Web layer
                            View webLayerFragmentContainer = activity.findViewById(activity.getWebLayerFragmentContainerId());
                            if (webLayerFragmentContainer != null) {
                                webLayerFragmentContainer.setVisibility(View.VISIBLE);
                                fragmentTransition.add(activity.getWebLayerFragmentContainerId(), webLayerFragment);
                                if (allowFragmentCommit()) fragmentTransition.commit();
                            }
                            else if (Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - showWebLayer: fragment container not found");
                        }
                    }
                });
            }
            else if (Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - showWebLayer: getWebLayerFragment returned null!");
        }
        catch (JSONException exception) {
            if(Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - showWebLayer: JSONException");
            exception.printStackTrace();
        }
	}

    /**
     * Brings the WebLayer to front.
     */
	protected void bringWebLayerToFront() {
        if (CobaltActivity.class.isAssignableFrom(mContext.getClass())) {
            CobaltActivity activity = (CobaltActivity) mContext;
            View webLayerContainer = activity.findViewById(activity.getWebLayerFragmentContainerId());
            if (webLayerContainer != null) {
                webLayerContainer.bringToFront();

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    ViewParent parentView = webLayerContainer.getParent();
                    parentView.requestLayout();
                    if (parentView instanceof View) {
                        ((View) parentView).invalidate();
                    }
                }
            }
            else if (Cobalt.DEBUG) {
                Log.w(Cobalt.TAG, TAG + " - bringWebLayerToFront: WebLayer fragment container not found");
            }
        }
        else if (Cobalt.DEBUG) {
            Log.w(Cobalt.TAG, TAG + " - bringWebLayerToFront: fragment is attached to " + mContext.getClass().getSimpleName()
                    + ", which is not a CobaltActivity");
        }
    }

    /**
     * Brings the WebView to front (which amounts to sending the WebLayer to back).
     */
    protected void sendWebLayerToBack() {
        if (CobaltActivity.class.isAssignableFrom(mContext.getClass())) {
            CobaltActivity activity = (CobaltActivity) mContext;
            View webViewContainer = activity.findViewById(activity.getFragmentContainerId());

            if (webViewContainer != null) {
                webViewContainer.bringToFront();

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    ViewParent parentView = webViewContainer.getParent();
                    parentView.requestLayout();
                    if (parentView instanceof View) {
                        ((View) parentView).invalidate();
                    }
                }
            }
            else if (Cobalt.DEBUG) {
                Log.w(Cobalt.TAG, TAG + " - sendWebLayerToBack: WebView fragment container not found");
            }
        }
        else if (Cobalt.DEBUG) {
            Log.w(Cobalt.TAG, TAG + " - sendWebLayerToBack: fragment is attached to " + mContext.getClass().getSimpleName()
                    + ", which is not a CobaltActivity");
        }
    }

    public boolean allowFragmentCommit() {
        return mAllowCommit;
    }
	
	/******************************************************************************************************************
	 * ALERT DIALOG
	 *****************************************************************************************************************/

	private void showAlertDialog(JSONObject data, final String callback) {		
		try {
			final String title = data.optString(Cobalt.kJSAlertTitle);
			final String message = data.optString(Cobalt.kJSMessage);
			final boolean cancelable = data.optBoolean(Cobalt.kJSAlertCancelable, false);
			final JSONArray buttons = data.has(Cobalt.kJSAlertButtons) ? data.getJSONArray(Cobalt.kJSAlertButtons) : new JSONArray();

            ((Activity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final AlertDialog alertDialog = new AlertDialog.Builder(mContext)
                                                                        .setTitle(title)
                                                                        .setMessage(message)
                                                                        .create();
                        alertDialog.setCancelable(cancelable);

                        if (buttons.length() == 0) {
                            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (callback != null) {
                                        try {
                                            JSONObject data = new JSONObject();
                                            data.put(Cobalt.kJSAlertButtonIndex, 0);
                                            sendCallback(callback, data);
                                        }
                                        catch (JSONException exception) {
                                            if (Cobalt.DEBUG) Log.e(Cobalt.TAG, "Alert - onClick: JSONException");
                                            exception.printStackTrace();
                                        }
                                    }
                                }
                            });
                        }
                        else {
                            int buttonsLength = Math.min(buttons.length(), 3);
                            for (int i = 0; i < buttonsLength; i++) {
                                int buttonId;

                                switch (i) {
                                    case 0:
                                    default:
                                        buttonId = DialogInterface.BUTTON_NEGATIVE;
                                        break;
                                    case 1:
                                        buttonId = DialogInterface.BUTTON_NEUTRAL;
                                        break;
                                    case 2:
                                        buttonId = DialogInterface.BUTTON_POSITIVE;
                                        break;
                                }

                                alertDialog.setButton(buttonId, buttons.getString(i), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (callback != null) {
                                            int buttonIndex;
                                            switch (which) {
                                                case DialogInterface.BUTTON_NEGATIVE:
                                                default:
                                                    buttonIndex = 0;
                                                    break;
                                                case DialogInterface.BUTTON_NEUTRAL:
                                                    buttonIndex = 1;
                                                    break;
                                                case DialogInterface.BUTTON_POSITIVE:
                                                    buttonIndex = 2;
                                                    break;
                                            }

                                            try {
                                                JSONObject data = new JSONObject();
                                                data.put(Cobalt.kJSAlertButtonIndex, buttonIndex);
                                                sendCallback(callback, data);
                                            }
                                            catch (JSONException exception) {
                                                if (Cobalt.DEBUG) Log.e(Cobalt.TAG, "Alert - onClick: JSONException");
                                                exception.printStackTrace();
                                            }
                                        }
                                    }
                                });
                            }
                        }

                        if (mActive) {
                            alertDialog.show();
                        }
                        else {
                            mPendingAlertDialogs.add(alertDialog);
                        }
                    }
                    catch (JSONException exception) {
                        if (Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - showAlertDialog: JSONException");
                        exception.printStackTrace();
                    }
                }
            });
		} 
		catch (JSONException exception) {
            if (Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - showAlertDialog: JSONException");
			exception.printStackTrace();
		}
	}

    private void showPendingAlertDialogs() {
        mActive = true;

        ArrayList<AlertDialog> pendingAlertDialogs = new ArrayList<>(mPendingAlertDialogs);
        int pendingAlertDialogsCount = pendingAlertDialogs.size();

        mPendingAlertDialogs.clear();

        for (int i = 0 ; i < pendingAlertDialogsCount ; i++) {
            AlertDialog alertDialog = pendingAlertDialogs.get(i);
            if (Cobalt.DEBUG) {
                Log.i(Cobalt.TAG, TAG + " - showPendingAlertDialogs: " + i + "/" + pendingAlertDialogsCount);
            }
            alertDialog.show();
        }
    }

	/*************************************************************************************
     * DATE PICKER
     ************************************************************************************/

    private void showDatePickerDialog(int year, int month, int day, String title, String delete,
                                      String cancel, String validate, String callbackID) {
    	Bundle args = new Bundle();
    	args.putInt(CobaltDatePickerFragment.ARG_YEAR, year);
    	args.putInt(CobaltDatePickerFragment.ARG_MONTH, month);
    	args.putInt(CobaltDatePickerFragment.ARG_DAY, day);
    	args.putString(CobaltDatePickerFragment.ARG_TITLE, title);
    	args.putString(CobaltDatePickerFragment.ARG_DELETE, delete);
    	args.putString(CobaltDatePickerFragment.ARG_CANCEL, cancel);
    	args.putString(CobaltDatePickerFragment.ARG_VALIDATE, validate);
    	args.putString(CobaltDatePickerFragment.ARG_CALLBACK_ID, callbackID);

        final CobaltDatePickerFragment fragment = new CobaltDatePickerFragment();
        fragment.setArguments(args);
        fragment.setListener(this);

        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fragment.show(((FragmentActivity) mContext).getSupportFragmentManager(), "datePicker");
            }
        });
    }
    
    protected void sendDate(int year, int month, int day, String callbackID) {
    	try {
            if (year != -1
                && month != -1
                && day != -1) {
                JSONObject date = new JSONObject();
                date.put(Cobalt.kJSYear, year);
                date.put(Cobalt.kJSMonth, ++month);
                date.put(Cobalt.kJSDay, day);

                sendCallback(callbackID, date);
    		}
            else {
                sendCallback(callbackID, null);
            }
		}
        catch (JSONException e) {
            if (Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - sendDate: JSONException");
			e.printStackTrace();
		}
    }

    /********************************************************
     * OPEN EXTERNAL URL
     ********************************************************/

    private void openExternalUrl(final String url) {
        ((Activity) mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
        });
    }

    /******************************************************************************************************************************
	 * PULL TO REFRESH
	 *****************************************************************************************************************************/
	
	/**
	 * Set the four colors used in the progress animation from color resources.
     * The first color will also be the color of the bar that grows in response to a user swipe gesture.
     * Must be called only after super.onStart().
	 * @param colorResource1 the first color resource
	 * @param colorResource2 the second color resource
	 * @param colorResource3 the third color resource
	 * @param colorResource4 the last color resource
	 */
	protected void setRefreshColorScheme(int colorResource1, int colorResource2, int colorResource3, int colorResource4) {
        if (mSwipeRefreshLayout != null) mSwipeRefreshLayout.setColorSchemeResources(colorResource1, colorResource2, colorResource3, colorResource4);
        else if (Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - setColorScheme: Pull-to-refresh must be active and method called after super.onStart()!");
	}

    @Override
    public void onRefresh() {
        refreshWebView();
    }

    private void refreshWebView() {
        sendEvent(Cobalt.JSEventPullToRefresh, null, Cobalt.JSCallbackPullToRefreshDidRefresh);
	}
	
	private void onPullToRefreshDidRefresh() {
        mSwipeRefreshLayout.setRefreshing(false);
		onPullToRefreshRefreshed();
	}

	/**
	 * This method may be overridden in subclasses.
	 */
	protected void onPullToRefreshRefreshed() { }
	
	/************************************************************************************
	 * INFINITE SCROLL
	 ***********************************************************************************/
	
	@Override
	public void onOverScrolled(int scrollX, int scrollY, int oldscrollX, int oldscrollY) {
        int height = mWebView.getHeight();
        long contentHeight = (long) Math.floor(mWebView.getContentHeight() * mContext.getResources().getDisplayMetrics().density);
        
		if (isInfiniteScrollActive()
            && ! mIsInfiniteScrollRefreshing
            && scrollY >= oldscrollY
            && scrollY + height >= contentHeight - height * getInfiniteScrollOffset() / 100) {
			infiniteScrollRefresh();
		}
	}

	private void infiniteScrollRefresh() {
        sendEvent(Cobalt.JSEventInfiniteScroll, null, Cobalt.JSCallbackInfiniteScrollDidRefresh);
        mIsInfiniteScrollRefreshing = true;
	}
	
	private void onInfiniteScrollDidRefresh() {
		mIsInfiniteScrollRefreshing = false;
		onInfiniteScrollRefreshed();
	}

	/**
	 * This method may be overridden in subclasses.
	 */
	protected void onInfiniteScrollRefreshed() { }
	
    /******************************************************
	 * CONFIGURATION
	 ******************************************************/

    protected boolean isPullToRefreshActive() {
		Bundle args = getArguments();
        return args != null && args.getBoolean(Cobalt.kPullToRefresh);
	}

    protected boolean isInfiniteScrollActive() {
		Bundle args = getArguments();
		return args != null && args.getBoolean(Cobalt.kInfiniteScroll);
	}

    protected int getInfiniteScrollOffset() {
        Bundle args = getArguments();
        if (args != null) {
            return args.getInt(Cobalt.kInfiniteScrollOffset);
        }
        else {
            return Cobalt.INFINITE_SCROLL_OFFSET_DEFAULT_VALUE;
        }
    }
    
    protected String getPage() {
        Bundle args = getArguments();
        if (args != null) {
            return args.getString(Cobalt.kPage);
        }
        else {
            return null;
        }
    }
}
