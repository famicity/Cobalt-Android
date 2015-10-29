/**
 *
 * CobaltActivity
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

package fr.cobaltians.cobalt.activities;

import fr.cobaltians.cobalt.Cobalt;
import fr.cobaltians.cobalt.R;
import fr.cobaltians.cobalt.fragments.CobaltFragment;
import fr.cobaltians.cobalt.fragments.CobaltWebLayerFragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link Activity} containing a {@link CobaltFragment}.
 * @author Diane
 */
public abstract class CobaltActivity extends AppCompatActivity {

    protected static final String TAG = CobaltActivity.class.getSimpleName();

    // NAVIGATION
    private boolean mAnimatedTransition;
    private JSONObject mDataNavigation;

    // Pop
    private static ArrayList<Activity> sActivitiesArrayList = new ArrayList<>();

    // Modal
    private boolean mWasPushedAsModal;
    private static boolean sWasPushedFromModal = false;

    // TODO: uncomment for Bars
    // BARS MENU ITEMS
    private HashMap<Integer, String> mMenuItemsHashMap = new HashMap<>();

    /***********************************************************************************************
     *
     * LIFECYCLE
     *
     **********************************************************************************************/

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(getLayoutToInflate());

        sActivitiesArrayList.add(this);

        Bundle bundle = getIntent().getExtras();
        Bundle extras = (bundle != null) ? bundle.getBundle(Cobalt.kExtras) : null;

        if (extras != null && bundle.containsKey(Cobalt.kJSData)) {
            try {
                mDataNavigation = new JSONObject(bundle.getString(Cobalt.kJSData));
            } catch (JSONException e) {
                if (Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - onCreate: data navigation parsing failed. " + extras.getString(Cobalt.kJSData));
                e.printStackTrace();
            }
        }

        if (extras != null && extras.containsKey(Cobalt.kBars)) {
            try {
                JSONObject actionBar = new JSONObject(extras.getString(Cobalt.kBars));
                setupBars(actionBar);
            }
            catch (JSONException exception) {
                setupBars(null);
                if (Cobalt.DEBUG) {
                    Log.e(Cobalt.TAG, TAG + " - onCreate: bars configuration parsing failed. " + extras.getString(Cobalt.kBars));
                }
                exception.printStackTrace();
            }
        }
        else {
            setupBars(null);
        }

		if (savedInstanceState == null) {
            CobaltFragment fragment = getFragment();

            if (fragment != null) {
                if (bundle != null) {
                    if (extras != null) fragment.setArguments(extras);
                    mAnimatedTransition = bundle.getBoolean(Cobalt.kJSAnimated, true);

                    if (mAnimatedTransition) {
                        mWasPushedAsModal = bundle.getBoolean(Cobalt.kPushAsModal, false);
                        if (mWasPushedAsModal) {
                            sWasPushedFromModal = true;
                            overridePendingTransition(R.anim.modal_open_enter, android.R.anim.fade_out);
                        }
                        else if (bundle.getBoolean(Cobalt.kPopAsModal, false)) {
                            sWasPushedFromModal = false;
                            overridePendingTransition(android.R.anim.fade_in, R.anim.modal_close_exit);
                        }
                        else if (sWasPushedFromModal) overridePendingTransition(R.anim.modal_push_enter, R.anim.modal_push_exit);
                    }
                    else overridePendingTransition(0, 0);
                }

                if (findViewById(getFragmentContainerId()) != null) {
                    getSupportFragmentManager().beginTransaction().replace(getFragmentContainerId(), fragment).commit();
                }
                else if (Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - onCreate: fragment container not found");
            }
            else if (Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - onCreate: getFragment() returned null");
		}
	}

    @Override
    protected void onStart() {
        super.onStart();

        Cobalt.getInstance(getApplicationContext()).onActivityStarted(this);
    }

    public void onAppStarted() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(getFragmentContainerId());
        if (fragment != null
            && CobaltFragment.class.isAssignableFrom(fragment.getClass())) {
            ((CobaltFragment) fragment).sendEvent(Cobalt.JSEventOnAppStarted, null, null);
        }
        else if (Cobalt.DEBUG) Log.i(Cobalt.TAG,    TAG + " - onAppStarted: no fragment container found \n"
                                                    + " or fragment found is not an instance of CobaltFragment. \n"
                                                    + "Drop onAppStarted event.");
    }

    public void onAppForeground() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(getFragmentContainerId());
        if (fragment != null
            && CobaltFragment.class.isAssignableFrom(fragment.getClass())) {
            ((CobaltFragment) fragment).sendEvent(Cobalt.JSEventOnAppForeground, null, null);
        }
        else if (Cobalt.DEBUG) Log.i(Cobalt.TAG,    TAG + " - onAppForeground: no fragment container found \n"
                                                    + " or fragment found is not an instance of CobaltFragment. \n"
                                                    + "Drop onAppForeground event.");
    }

    @Override
    public void finish() {
        super.finish();

        if (mAnimatedTransition) {
            if (mWasPushedAsModal) {
                sWasPushedFromModal = false;
                overridePendingTransition(android.R.anim.fade_in, R.anim.modal_close_exit);
            } else if (sWasPushedFromModal) overridePendingTransition(R.anim.modal_pop_enter, R.anim.modal_pop_exit);
        }
        else overridePendingTransition(0, 0);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Cobalt.getInstance(getApplicationContext()).onActivityStopped(this);
    }

    public void onAppBackground() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(getFragmentContainerId());
        if (fragment != null
            && CobaltFragment.class.isAssignableFrom(fragment.getClass())) {
            ((CobaltFragment) fragment).sendEvent(Cobalt.JSEventOnAppBackground, null, null);
        }
        else if (Cobalt.DEBUG) Log.i(Cobalt.TAG,    TAG + " - onAppBackground: no fragment container found \n"
                                                    + " or fragment found is not an instance of CobaltFragment. \n"
                                                    + "Drop onAppBackground event.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        sActivitiesArrayList.remove(this);
    }

    /***********************************************************************************************
     *
     * MENU
     *
     **********************************************************************************************/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        Bundle bundle = getIntent().getExtras();
        Bundle extras = (bundle != null) ? bundle.getBundle(Cobalt.kExtras) : null;

        if (extras != null
            && extras.containsKey(Cobalt.kBars)) {
            try {
                JSONObject actionBar = new JSONObject(extras.getString(Cobalt.kBars));
                JSONArray actions = actionBar.optJSONArray(Cobalt.kBarsActions);
                if (actions != null) return setupOptionsMenu(menu, actions);
            }
            catch (JSONException exception) {
                if (Cobalt.DEBUG) {
                    Log.e(Cobalt.TAG, TAG + " - onCreate: action bar configuration parsing failed. " + extras.getString(Cobalt.kBars));
                }
                exception.printStackTrace();
            }
        }

        return false;
    }

    // TODO: uncomment for Bars
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (mMenuItemsHashMap.containsKey(itemId)) {
            String name = mMenuItemsHashMap.get(item.getItemId());

            Fragment fragment = getSupportFragmentManager().findFragmentById(getFragmentContainerId());
            if (fragment != null
                && CobaltFragment.class.isAssignableFrom(fragment.getClass())) {
                try {
                    JSONObject data = new JSONObject();
                    data.put(Cobalt.kJSAction, Cobalt.JSActionActionPressed);
                    data.put(Cobalt.kJSActionName, name);

                    JSONObject message = new JSONObject();
                    message.put(Cobalt.kJSType, Cobalt.JSTypeUI);
                    message.put(Cobalt.kJSUIControl, Cobalt.JSControlBars);
                    message.put(Cobalt.kJSData, data);

                    ((CobaltFragment) fragment).sendMessage(message);
                }
                catch(JSONException exception) { exception.printStackTrace(); }
            }
            else if (Cobalt.DEBUG) Log.i(Cobalt.TAG,    TAG + " - onOptionsItemSelected: no fragment container found \n"
                    + " or fragment found is not an instance of CobaltFragment. \n"
                    + "Drop " + name + "bars button pressed event.");

            return true;
        }
        else if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        else return super.onOptionsItemSelected(item);
    }

    /***********************************************************************************************
     *
     * COBALT
     *
     **********************************************************************************************/

    /***********************************************************************************************
     *
     * UI
     *
     **********************************************************************************************/

	/**
	 * Returns a new instance of the contained fragment. 
	 * This method should be overridden in subclasses.
	 * @return a new instance of the fragment contained.
	 */
	protected abstract CobaltFragment getFragment();

	protected int getLayoutToInflate() {
		return R.layout.activity_cobalt;
	}

	public int getFragmentContainerId() {
		return R.id.fragment_container;
	}

    public int getBottomBarId() {
        return R.id.bottom_bar;
    }

    private void setupBars(JSONObject configuration) {
        ActionBar actionBar = getSupportActionBar();
        LinearLayout bottomActionBar = (LinearLayout) findViewById(getBottomBarId());

        // TODO: make bars more flexible
        if (actionBar == null
            || bottomActionBar == null) {
            if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + "setupBars: activity does not have an action bar and/or does not contain a bottom bar.");
            return;
        }

        // Default
        actionBar.show();
        actionBar.setTitle(null);
        actionBar.setDisplayHomeAsUpEnabled(true);

        if (configuration != null) {
            // Background color
            String backgroundColor = configuration.optString(Cobalt.kBarsBackgroundColor);
            try {
                int colorInt = Cobalt.parseColor(backgroundColor);
                actionBar.setBackgroundDrawable(new ColorDrawable(colorInt));
                bottomActionBar.setBackgroundColor(colorInt);
            }
            catch (IllegalArgumentException exception) {
                if (Cobalt.DEBUG) {
                    Log.w(Cobalt.TAG, TAG + "setupBars: backgroundColor " + backgroundColor + " format not supported, use #RRGGBB.");
                }
                exception.printStackTrace();
            }

            // Color (default: system)
            // TODO: color

            // Logo
            // TODO: add font support
            String logo = configuration.optString(Cobalt.kBarsIcon);
            try {
                if (logo.length() == 0) throw new IllegalArgumentException();
                String[] logoSplit = logo.split(":");
                String packageName, logoName;
                switch(logoSplit.length) {
                    case 1:
                        packageName = getPackageName();
                        logoName = logoSplit[0];
                        break;
                    case 2:
                        packageName = logoSplit[0].length() != 0 ? logoSplit[0] : getPackageName();
                        logoName = logoSplit[1];
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
                int resId = getResources().getIdentifier(logoName, "drawable", packageName);
                if (resId != 0) {
                    actionBar.setIcon(resId);
                    actionBar.setDisplayShowHomeEnabled(true);
                }
                else {
                    Log.w(Cobalt.TAG, TAG + "setupBars: androidIcon " + logo + " not found.");
                }
            }
            catch (IllegalArgumentException exception) {
                if (Cobalt.DEBUG) {
                    Log.w(Cobalt.TAG, TAG + "setupBars: androidIcon " + logo + " format not supported, use icon, :icon or com.example.app:icon.");
                }
                exception.printStackTrace();
            }

            // Title
            String title = configuration.optString(Cobalt.kBarsTitle);
            if (title.length() != 0) {
                actionBar.setTitle(title);
                //actionBar.setDisplayShowTitleEnabled(true);
            }

            // Visible
            JSONObject visible = configuration.optJSONObject(Cobalt.kBarsVisible);
            if (visible != null) {
                boolean top = visible.optBoolean(Cobalt.kVisibleTop, true);
                if (!top) {
                    actionBar.hide();
                }

                boolean bottom = visible.optBoolean(Cobalt.kVisibleBottom);
                if (bottom) {
                    bottomActionBar.setVisibility(View.VISIBLE);
                }
            }

            // Up
            JSONObject navigationIcon = configuration.optJSONObject(Cobalt.kBarsNavigationIcon);
            if (navigationIcon != null) {
                boolean enabled = navigationIcon.optBoolean(Cobalt.kNavigationIconEnabled, true);
                actionBar.setDisplayHomeAsUpEnabled(enabled);

                String icon = navigationIcon.optString(Cobalt.kNavigationIconIcon);
                // TODO: add font support
                try {
                    if (icon.length() == 0) throw new IllegalArgumentException();
                    String[] iconSplit = icon.split(":");
                    String packageName, iconName;
                    switch(iconSplit.length) {
                        case 1:
                            packageName = getPackageName();
                            iconName = iconSplit[0];
                            break;
                        case 2:
                            packageName = iconSplit[0].length() != 0 ? iconSplit[0] : getPackageName();
                            iconName = iconSplit[1];
                            break;
                        default:
                            throw new IllegalArgumentException();
                    }
                    int resId = getResources().getIdentifier(iconName, "drawable", packageName);
                    if (resId != 0) {
                        actionBar.setHomeAsUpIndicator(resId);
                    }
                    else {
                        Log.w(Cobalt.TAG, TAG + "setupBars: androidNavigationIcon " + icon + " not found.");
                    }
                }
                catch (IllegalArgumentException exception) {
                    if (Cobalt.DEBUG) {
                        Log.w(Cobalt.TAG, TAG + "setupBars: androidNavigationIcon " + icon + " format not supported, use icon, :icon or com.example.app:icon.");
                    }
                    exception.printStackTrace();
                }
            }
        }
    }

    // TODO: uncomment for Bars
    private boolean setupOptionsMenu(Menu menu, JSONArray actions) {
        ActionBar actionBar = getSupportActionBar();
        LinearLayout bottomActionBar = (LinearLayout) findViewById(getBottomBarId());

        if (actionBar == null
            || bottomActionBar == null) {
            if (Cobalt.DEBUG) {
                Log.w(Cobalt.TAG, TAG + "setupOptionsMenu: activity does not have an action bar and/or does not contain a bottom bar.");
            }
            return false;
        }

        int menuItemsAddedToTop = 0;
        int menuItemsAddedToOverflow = 0;

        int length = actions.length();
        for (int i = 0; i < length; i++) {
            try {
                // TODO: handle groups
                /*
                actions:[{
                    androidPosition: string,    //mandatory, may be "top", "bottom", "overflow"
                    actions: []
                },
                ...]
                */
                JSONObject action = actions.getJSONObject(i);
                final String name = action.getString(Cobalt.kActionName);
                String title = action.getString(Cobalt.kActionTitle);
                String position = action.getString(Cobalt.kActionPosition);             // must be "top", "bottom", "overflow"
                // TODO: add font support
                String icon = action.optString(Cobalt.kActionIcon, null);               // must be "fontKey character"
                String androidIcon = action.optString(Cobalt.kActionAndroidIcon, null); // must be "icon", ":icon", "package:icon"
                // TODO: add default color support
                String color = action.optString(Cobalt.kActionColor, null);             // default: same as bar color
                boolean visible = action.optBoolean(Cobalt.kActionVisible, true);
                boolean enabled = action.optBoolean(Cobalt.kActionEnabled, true);
                // TODO: add badge support
                String badge = action.optString(Cobalt.kActionBadge, null);             // if "", hide it

                MenuItem menuItem;
                String[] split;
                int resId;

                switch(position) {
                    case Cobalt.kPositionTop:
                        if (icon.length() == 0) throw new JSONException("Actions positionned at top or bottom must specify an icon attribute. Current: " + icon);
                        split = icon.split(":");
                        if (split.length != 2) throw new JSONException("androidIcon " + icon + " format not supported, use com.example.app:icon.");
                        resId = getResources().getIdentifier(split[1], "drawable", split[0]);
                        if (resId == 0) throw new JSONException("androidIcon " + icon + " not found.");

                        menuItem = menu.add(Menu.NONE, i, menuItemsAddedToTop++, title);
                        menuItem.setIcon(resId);
                        // TODO: limit according to screen width dp (< 360dp : 2, 360dp >= < 500dp : 3, 500dp >= < 600dp : 4, 600dp >= : 5)
                        if (menuItemsAddedToTop > 2) MenuItemCompat.setShowAsAction(menuItem, MenuItem.SHOW_AS_ACTION_IF_ROOM);
                        else MenuItemCompat.setShowAsAction(menuItem, MenuItem.SHOW_AS_ACTION_ALWAYS);
                        menuItem.setVisible(visible);
                        mMenuItemsHashMap.put(i, name);
                        break;
                    case Cobalt.kPositionOverflow:
                        menuItem = menu.add(Menu.NONE, i, menuItemsAddedToOverflow++, title);
                        MenuItemCompat.setShowAsAction(menuItem, MenuItem.SHOW_AS_ACTION_NEVER);
                        menuItem.setVisible(visible);

                        mMenuItemsHashMap.put(i, name);
                        break;
                    case Cobalt.kPositionBottom:
                        if (icon.length() == 0) throw new JSONException("Actions positionned at top or bottom must specify an icon attribute. Current: " + icon);
                        split = icon.split(":");
                        if (split.length != 2) throw new JSONException("androidIcon " + icon + " format not supported, use com.example.app:icon.");
                        resId = getResources().getIdentifier(split[1], "drawable", split[0]);
                        if (resId == 0) throw new JSONException("androidIcon " + icon + " not found.");

                        ImageButton button = new ImageButton(this);
                        button.setImageResource(resId);
                        // TODO: find best background to mimic default behavior
                        button.setBackgroundResource(android.R.drawable.menuitem_background);
                        //button.setBackgroundResource(R.drawable.menu_item_background_dark);
                        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(   ViewGroup.LayoutParams.WRAP_CONTENT,
                                                                                            getResources().getDimensionPixelSize(R.dimen.bottom_actionbar_button_size));
                        button.setLayoutParams(layoutParams);
                        int padding = getResources().getDimensionPixelSize(R.dimen.bottom_actionBar_button_padding);
                        button.setPadding(padding, padding, padding, padding);
                        // TODO: add accessibility tooltip
                        //if (title != null) button.setContentDescription(title);
                        if (visible) {
                            button.setVisibility(View.VISIBLE);
                        }
                        else button.setVisibility(View.GONE);

                        button.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {
                                Fragment fragment = getSupportFragmentManager().findFragmentById(getFragmentContainerId());
                                if (fragment != null
                                    && CobaltFragment.class.isAssignableFrom(fragment.getClass())) {
                                    try {
                                        JSONObject data = new JSONObject();
                                        data.put(Cobalt.kJSAction, Cobalt.JSActionActionPressed);
                                        data.put(Cobalt.kJSActionName, name);

                                        JSONObject message = new JSONObject();
                                        message.put(Cobalt.kJSType, Cobalt.JSTypeUI);
                                        message.put(Cobalt.kJSUIControl, Cobalt.JSControlBars);
                                        message.put(Cobalt.kJSData, data);

                                        ((CobaltFragment) fragment).sendMessage(message);
                                    }
                                    catch(JSONException exception) { exception.printStackTrace(); }
                                }
                                else if (Cobalt.DEBUG) Log.i(Cobalt.TAG,    TAG + " - onBarsButtonClick: no fragment container found \n"
                                        + " or fragment found is not an instance of CobaltFragment. \n"
                                        + "Drop " + name + "bars button pressed event.");
                            }
                        });

                        bottomActionBar.addView(button);
                        break;
                    default:
                        throw new JSONException("androidPosition attribute must be top, overflow or bottom.");
                }
            }
            catch (JSONException exception) {
                if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + "setupOptionsMenu: actions " + actions.toString() + " format not supported, use at least {\n"
                                                    + "\tname: \"name\",\n"
                                                    + "\ttitle: \"title\",\n"
                                                    + "\tandroidPosition: \"top\"|\"overflow\"|\"bottom\"\n"
                                                    + "}");
                exception.printStackTrace();
            }
        }

        // true to display menu
        return true;
    }

    /***********************************************************************************************
     *
     * BACK
     *
     **********************************************************************************************/

	/**
	 * Called when back button is pressed. 
	 * This method should NOT be overridden in subclasses.
	 */
	@Override
	public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(getFragmentContainerId());
        if (fragment != null
            && CobaltFragment.class.isAssignableFrom(fragment.getClass())) {
            ((CobaltFragment) fragment).askWebViewForBackPermission();
        }
        else {
            super.onBackPressed();
            if (Cobalt.DEBUG) Log.i(Cobalt.TAG,     TAG + " - onBackPressed: no fragment container found \n"
                                                    + " or fragment found is not an instance of CobaltFragment. \n"
                                                    + "Call super.onBackPressed()");
        }
	}

	/**
	 * Called from the contained {@link CobaltFragment} when the Web view has authorized the back event.
	 * This method should NOT be overridden in subclasses.
	 */
	public void back() {
		runOnUiThread(new Runnable() {

            @Override
            public void run() {
                backWithSuper();
            }
        });
	}

	private void backWithSuper() {
		super.onBackPressed();
	}

    /***********************************************************************************************
     *
     * WEB LAYER DISMISS
     *
     **********************************************************************************************/

	/**
	 * Called when a {@link CobaltWebLayerFragment} has been dismissed.
	 * This method may be overridden in subclasses.
	 */
	public void onWebLayerDismiss(String page, JSONObject data) {
        CobaltFragment fragment = (CobaltFragment) getSupportFragmentManager().findFragmentById(getFragmentContainerId());
        if (fragment != null) {
            fragment.onWebLayerDismiss(page, data);
		}
        else if (Cobalt.DEBUG) Log.e(Cobalt.TAG,   TAG + " - onWebLayerDismiss: no fragment container found");
	}

    public void popTo(String controller, String page, JSONObject data){
        Intent popToIntent = Cobalt.getInstance(this).getIntentForController(controller, page);

        if (popToIntent != null) {
            Bundle popToExtras = popToIntent.getBundleExtra(Cobalt.kExtras);
            String popToActivityClassName = popToExtras.getString(Cobalt.kActivity);

            try {
                Class<?> popToActivityClass = Class.forName(popToActivityClassName);

                boolean popToControllerFound = false;
                int popToControllerIndex = -1;

                for (int i = sActivitiesArrayList.size() - 1; i >= 0; i--) {
                    Activity oldActivity = sActivitiesArrayList.get(i);
                    Class<?> oldActivityClass = oldActivity.getClass();

                    Bundle oldBundle = oldActivity.getIntent().getExtras();
                    Bundle oldExtras = (oldBundle != null) ? oldBundle.getBundle(Cobalt.kExtras) : null;
                    String oldPage = (oldExtras != null) ? oldExtras.getString(Cobalt.kPage) : null;

                    if (oldPage == null
                        && CobaltActivity.class.isAssignableFrom(oldActivityClass)) {
                        Fragment fragment = ((CobaltActivity) oldActivity).getSupportFragmentManager().findFragmentById(((CobaltActivity) oldActivity).getFragmentContainerId());
                        if (fragment != null) {
                            oldExtras = fragment.getArguments();
                            oldPage = (oldExtras != null) ? oldExtras.getString(Cobalt.kPage) : null;
                        }
                    }

                    if (popToActivityClass.equals(oldActivityClass)
                        &&  (! CobaltActivity.class.isAssignableFrom(oldActivityClass)
                            || (CobaltActivity.class.isAssignableFrom(oldActivityClass) && page.equals(oldPage)))) {
                        popToControllerFound = true;
                        popToControllerIndex = i;
                        ((CobaltActivity)oldActivity).setDataNavigation(data);
                        break;
                    }
                }

                if (popToControllerFound) {
                    while (popToControllerIndex + 1 <= sActivitiesArrayList.size()) {
                        sActivitiesArrayList.get(popToControllerIndex + 1).finish();
                    }
                }
                else if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - popTo: controller " + controller + (page == null ? "" : " with page " + page) + " not found in history. Abort.");
            }
            catch (ClassNotFoundException exception) {
                exception.printStackTrace();
            }
        }
        else if (Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - popTo: unable to pop to null controller");
    }

    public void dataForPop(JSONObject data) {
        if (sActivitiesArrayList.size() >= 2) {
            boolean cobaltActivityFound = false;
            int index = sActivitiesArrayList.size()-2;
            while (!cobaltActivityFound && index >= 0) {
                Activity activity = sActivitiesArrayList.get(index);
                if (CobaltActivity.class.isAssignableFrom(activity.getClass())) {
                    ((CobaltActivity) activity).setDataNavigation(data);
                    cobaltActivityFound = true;
                }
                index--;
            }
            if (!cobaltActivityFound && Cobalt.DEBUG) Log.e(Cobalt.TAG,  TAG + " - dataForPop: CobaltActivity not found");
        }
    }

    public JSONObject getDataNavigation() { return mDataNavigation; }

    public void setDataNavigation(JSONObject data) {
        this.mDataNavigation = data;
    }
}
