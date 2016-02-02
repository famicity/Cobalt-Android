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

package org.cobaltians.cobalt.activities;

import org.cobaltians.cobalt.Cobalt;
import org.cobaltians.cobalt.R;
import org.cobaltians.cobalt.customviews.ActionViewMenuItem;
import org.cobaltians.cobalt.customviews.ActionViewMenuItemListener;
import org.cobaltians.cobalt.customviews.BottomBar;
import org.cobaltians.cobalt.font.CobaltFontManager;
import org.cobaltians.cobalt.fragments.CobaltFragment;
import org.cobaltians.cobalt.fragments.CobaltWebLayerFragment;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IllegalFormatException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link Activity} containing a {@link CobaltFragment}.
 * @author Diane
 */
public abstract class CobaltActivity extends AppCompatActivity implements ActionViewMenuItemListener {

    protected static final String TAG = CobaltActivity.class.getSimpleName();

    // NAVIGATION
    private boolean mAnimatedTransition;
    private JSONObject mDataNavigation;

    // Pop
    private static ArrayList<Activity> sActivitiesArrayList = new ArrayList<>();

    // Modal
    private boolean mWasPushedAsModal;
    private static boolean sWasPushedFromModal = false;

    // BARS
    protected HashMap<String, ActionViewMenuItem> mMenuItemsHashMap = new HashMap<>();
    protected HashMap<Integer, String> mMenuItemsIdMap = new HashMap<>();

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
        if (bundle == null) {
            bundle = new Bundle();
        }
        Bundle extras = bundle.getBundle(Cobalt.kExtras);
        if (extras == null) {
            extras = Cobalt.getInstance(this.getApplicationContext()).getConfigurationForController(getInitController());
            extras.putString(Cobalt.kPage, getInitPage());
            bundle.putBundle(Cobalt.kExtras, extras);
        }
        //Bundle extras = (bundle != null) ? bundle.getBundle(Cobalt.kExtras) : null;

        if (bundle.containsKey(Cobalt.kJSData)) {
            try {
                mDataNavigation = new JSONObject(bundle.getString(Cobalt.kJSData));
            } catch (JSONException e) {
                if (Cobalt.DEBUG) Log.e(Cobalt.TAG, TAG + " - onCreate: data navigation parsing failed. " + extras.getString(Cobalt.kJSData));
                e.printStackTrace();
            }
        }

        if (extras.containsKey(Cobalt.kBars)) {
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
                fragment.setArguments(extras);
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

    public String getInitController() {
        return null;
    }

    public String getInitPage() {
        return null;
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
        if (bundle == null) {
            bundle = new Bundle();
        }
        Bundle extras = bundle.getBundle(Cobalt.kExtras);
        if (extras == null) {
            extras = Cobalt.getInstance(this.getApplicationContext()).getConfigurationForController(getInitController());
        }
        if (extras.containsKey(Cobalt.kBars)) {
            try {
                JSONObject actionBar = new JSONObject(extras.getString(Cobalt.kBars));
                String color = actionBar.optString(Cobalt.kBarsColor);
                if (color.equals("")) {
                    color = getThemeDefaultColorBackground();
                }
                JSONArray actions = actionBar.optJSONArray(Cobalt.kBarsActions);
                if (actions != null) setupOptionsMenu(menu, color, actions);
            }
            catch (JSONException exception) {
                if (Cobalt.DEBUG) {
                    Log.e(Cobalt.TAG, TAG + " - onCreate: action bar configuration parsing failed. " + extras.getString(Cobalt.kBars));
                }
                exception.printStackTrace();
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        if (mMenuItemsIdMap.containsKey(itemId)) {
            onPressed(mMenuItemsIdMap.get(itemId));
            return true;
        }
        return super.onOptionsItemSelected(item);
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

    public int getTopBarId() {
        return R.id.top_bar;
    }

    public int getBottomBarId() {
        return R.id.bottom_bar;
    }

    public String getThemeDefaultColorBackground() {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.colorPrimary, typedValue, true);
        try {
            String color = String.format("%06x", (0xFFFFFF & typedValue.data));
            return color;
        }
        catch (NullPointerException exc) {
            if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - getThemeDefaultColorBackground : no attribute colorPrimary found");
        }
        catch (IllegalFormatException exc) {
            if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - getThemeDefaultColorBackground : illegal format for colorPrimary found");
        }
        return "";
    }

    public String getThemeDefaultTextColor() {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        try {
            String color = String.format("%06x", (0xFFFFFF & typedValue.data));
            return color;
        }
        catch (NullPointerException exc) {
            if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - getThemeDefaultTextColor : no attribute textColorPrimary found");
        }
        catch (IllegalFormatException exc) {
            if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - getThemeDefaultTextColor : illegal format for textColorPrimary found");
        }
        return "";
    }

    public void setupBars(JSONObject configuration) {
        Toolbar topBar = (Toolbar) findViewById(getTopBarId());
        // TODO: use LinearLayout for bottomBar instead to handle groups
        //LinearLayout bottomBar = (LinearLayout) findViewById(getBottomBarId());
        BottomBar bottomBar = (BottomBar) findViewById(getBottomBarId());

        // TODO: make bars more flexible
        if (topBar == null
            || bottomBar == null) {
            if (Cobalt.DEBUG) Log.w(Cobalt.TAG, TAG + " - setupBars: activity does not have an action bar and/or does not contain a bottom bar.");
            return;
        }

        setSupportActionBar(topBar);
        ActionBar actionBar = getSupportActionBar();

        // Default
        actionBar.setTitle(null);
        actionBar.setDisplayHomeAsUpEnabled(true);

        if (configuration != null) {
            // Background color
            // TODO: apply on overflow popup
            //TODO : see how get and set the default color value when barsBackgroundcolor and barsColor were null
            String backgroundColor = configuration.optString(Cobalt.kBarsBackgroundColor, null);
            if (backgroundColor == null) backgroundColor = getThemeDefaultColorBackground();
            try {
                int backgroundColorInt = Cobalt.parseColor(backgroundColor);
                actionBar.setBackgroundDrawable(new ColorDrawable(backgroundColorInt));
                bottomBar.setBackgroundColor(backgroundColorInt);
            }
            catch (IllegalArgumentException exception) {
                if (Cobalt.DEBUG) {
                    Log.w(Cobalt.TAG, TAG + " - setupBars: backgroundColor " + backgroundColor + " format not supported, use (#)RGB or (#)RRGGBB(AA).");
                }
                exception.printStackTrace();
            }

            // Color (default: system)
            int colorInt = 0;
            boolean applyColor = false;
            String color = configuration.optString(Cobalt.kBarsColor, null);
            if (color == null) color = getThemeDefaultTextColor();
            try {
                colorInt = Cobalt.parseColor(color);
                applyColor = true;
                topBar.setTitleTextColor(colorInt);

                Drawable overflowIconDrawable = topBar.getOverflowIcon();
                overflowIconDrawable.setColorFilter(colorInt, PorterDuff.Mode.SRC_ATOP);

                Drawable navigationIconDrawable = topBar.getNavigationIcon();
                navigationIconDrawable.setColorFilter(colorInt, PorterDuff.Mode.SRC_ATOP);
            }
            catch (IllegalArgumentException exception) {
                if (Cobalt.DEBUG) {
                    Log.w(Cobalt.TAG, TAG + " - setupBars: color " + color + " format not supported, use (#)RGB or (#)RRGGBB(AA).");
                }
                exception.printStackTrace();
            }


            // Logo
            String logo = configuration.optString(Cobalt.kBarsIcon);
            if (!logo.equals("")) {
                Drawable logoDrawable;

                int logoResId = getResourceIdentifier(logo);
                if (logoResId != 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        logoDrawable = getResources().getDrawable(logoResId, null);
                    }
                    else {
                        logoDrawable = getResources().getDrawable(logoResId);
                    }

                    if (applyColor) {
                        logoDrawable.setColorFilter(colorInt, PorterDuff.Mode.SRC_ATOP);
                    }
                }
                else {
                    logoDrawable = CobaltFontManager.getCobaltFontDrawable(this, logo, colorInt);
                }
                topBar.setLogo(logoDrawable);
                actionBar.setDisplayShowHomeEnabled(true);
            }
            else {
                actionBar.setDisplayShowHomeEnabled(false);
            }

            // Title
            String title = configuration.optString(Cobalt.kBarsTitle);
            if (title.length() != 0) {
                actionBar.setTitle(title);
            }
            else {
                actionBar.setDisplayShowTitleEnabled(false);
            }

            // Visible
            JSONObject visible = configuration.optJSONObject(Cobalt.kBarsVisible);
            setActionBarVisible(visible);
            /*if (visible != null) {
                boolean top = visible.optBoolean(Cobalt.kVisibleTop, true);
                if (!top && actionBar.isShowing()) {
                    actionBar.hide();
                }
                else if (top && !actionBar.isShowing()){
                    actionBar.show();
                }

                boolean bottom = visible.optBoolean(Cobalt.kVisibleBottom);
                if (bottom) {
                    bottomBar.setVisibility(View.VISIBLE);
                }
                else bottomBar.setVisibility(View.GONE);
            }*/
            // Up
            JSONObject navigationIcon = configuration.optJSONObject(Cobalt.kBarsNavigationIcon);
            if (navigationIcon == null) navigationIcon = new JSONObject();
            boolean enabled = navigationIcon.optBoolean(Cobalt.kNavigationIconEnabled, true);
            actionBar.setDisplayHomeAsUpEnabled(enabled);
            Drawable navigationIconDrawable;

            String icon = navigationIcon.optString(Cobalt.kNavigationIconIcon);
            if (!icon.equals("")) {
                int iconResId = getResourceIdentifier(icon);
                if (iconResId != 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        navigationIconDrawable = getResources().getDrawable(iconResId, null);
                    }
                    else {
                        navigationIconDrawable = getResources().getDrawable(iconResId);
                    }
                    if (applyColor) {
                        navigationIconDrawable.setColorFilter(colorInt, PorterDuff.Mode.SRC_ATOP);
                    }
                }
                else {
                    navigationIconDrawable = CobaltFontManager.getCobaltFontDrawable(this, icon, colorInt);
                }
                topBar.setNavigationIcon(navigationIconDrawable);
            }
        }
    }

    private void setupOptionsMenu(Menu menu, String color, JSONArray actions) {
        ActionBar actionBar = getSupportActionBar();
        // TODO: use LinearLayout for bottomBar instead to handle groups
        //LinearLayout bottomBar = (LinearLayout) findViewById(getBottomBarId());
        BottomBar bottomBar = (BottomBar) findViewById(getBottomBarId());

        if (actionBar == null
            || bottomBar == null) {
            if (Cobalt.DEBUG) {
                Log.w(Cobalt.TAG, TAG + " - setupOptionsMenu: activity does not have an action bar and/or does not contain a bottom bar.");
            }
            return;
        }

        Menu bottomMenu = bottomBar.getMenu();

        menu.clear();
        bottomMenu.clear();

        int actionId = 0;
        int menuItemsAddedToTop = 0;
        int menuItemsAddedToOverflow = 0;
        int menuItemsAddedToBottom = 0;

        int length = actions.length();
        for (int i = 0; i < length; i++) {
            try {
                JSONObject action = actions.getJSONObject(i);
                String position = action.getString(Cobalt.kActionPosition);             // must be "top", "bottom", "overflow"
                JSONArray groupActions = action.optJSONArray(Cobalt.kActionActions);

                Menu addToMenu = menu;
                int order;

                switch (position) {
                    case Cobalt.kPositionTop:
                        order = menuItemsAddedToTop++;
                        break;
                    case Cobalt.kPositionOverflow:
                        order = menuItemsAddedToOverflow++;
                        break;
                    case Cobalt.kPositionBottom:
                        order = menuItemsAddedToBottom++;
                        addToMenu = bottomMenu;
                        // TODO find a way to add same space between each actionViewItem
                        /*MenuItem spaceMenuItem = addToMenu.add(Menu.NONE, Menu.NONE, order++, "");
                        MenuItemCompat.setShowAsAction(spaceMenuItem, MenuItem.SHOW_AS_ACTION_ALWAYS);
                        spaceMenuItem.setVisible(true);
                        spaceMenuItem.setEnabled(false);*/
                        break;
                    default:
                        throw new JSONException("androidPosition attribute must be top, overflow or bottom.");
                }

                if (groupActions != null) {
                    addGroup(addToMenu, order, groupActions, actionId, position, color);
                }
                else {
                    addMenuItem(addToMenu, order, action, actionId++, position, color);
                }
            }
            catch (JSONException exception) {
                if (Cobalt.DEBUG) {
                    Log.w(Cobalt.TAG,   TAG + " - setupOptionsMenu: action " + i + " of actions array below is not an object, does not contain a position field or its value is not top, overflow or bottom.\n"
                                        + actions.toString());
                }

                exception.printStackTrace();
            }
        }

        /*if (menuItemsAddedToBottom > 0) {
            MenuItem spaceMenuItem = bottomMenu.add(Menu.NONE, Menu.NONE, menuItemsAddedToBottom, "");
            MenuItemCompat.setShowAsAction(spaceMenuItem, MenuItem.SHOW_AS_ACTION_ALWAYS);
            spaceMenuItem.setVisible(true);
            spaceMenuItem.setEnabled(false);
        }*/
    }

    protected void addGroup(Menu menu, int order, JSONArray actions, int actionId, String position, String color) {
        int length = actions.length();
        for (int i = 0; i < length; i++) {
            try {
                JSONObject action = actions.getJSONObject(i);
                addMenuItem(menu, order++, action, actionId++, position, color);
            }
            catch (JSONException exception) {
                if (Cobalt.DEBUG) {
                    Log.w(Cobalt.TAG,   TAG + " - addGroup: action " + i + " of actions array below is not an object.\n"
                                        + actions.toString());
                }
                exception.printStackTrace();
            }
        }
    }

    public void setBadgeMenuItem(String name, final String badgeText){
        if (mMenuItemsHashMap.containsKey(name)) {
            final ActionViewMenuItem item = mMenuItemsHashMap.get(name);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    item.setActionBadge(badgeText);
                }
            });
        }
    }

    public void setContentMenuItem(String name, final JSONObject content){
        if (mMenuItemsHashMap.containsKey(name)) {
            final ActionViewMenuItem item = mMenuItemsHashMap.get(name);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    item.setActionContent(content);
                }
            });
        }
    }

    public void setActionBarVisible(final JSONObject visible) {
        if (visible != null) {
            final ActionBar actionBar = getSupportActionBar();
            final BottomBar bottomBar = (BottomBar) findViewById(getBottomBarId());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (visible.has(Cobalt.kVisibleTop)) {
                        boolean top = visible.optBoolean(Cobalt.kVisibleTop);

                        if (!top && actionBar.isShowing()) {
                            actionBar.hide();
                        }
                        else if (top && !actionBar.isShowing()){
                            actionBar.show();
                        }
                    }

                    if (visible.has(Cobalt.kVisibleBottom)) {
                        boolean bottom = visible.optBoolean(Cobalt.kVisibleBottom);
                        if (bottom) {
                            bottomBar.setVisibility(View.VISIBLE);
                        }
                        else bottomBar.setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    protected void addMenuItem(Menu menu, int order, JSONObject action, final int id, String position, String barsColor) {
        try {
            final String name = action.getString(Cobalt.kActionName);
            String title = action.getString(Cobalt.kActionTitle);
            String color = action.optString(Cobalt.kActionColor, barsColor);             // default: same as bar color
            boolean visible = action.optBoolean(Cobalt.kActionVisible, true);
            boolean enabled = action.optBoolean(Cobalt.kActionEnabled, true);

            final MenuItem menuItem = menu.add(Menu.NONE, id, order, title);

            int showAsAction = MenuItem.SHOW_AS_ACTION_IF_ROOM;
            switch(position) {
                case Cobalt.kPositionBottom:
                    showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS;
                    break;
                case Cobalt.kPositionOverflow:
                    showAsAction = MenuItem.SHOW_AS_ACTION_NEVER;
                    break;
            }
            MenuItemCompat.setShowAsAction(menuItem, showAsAction);

            int colorInt = 0;
            boolean applyColor = false;
            try {
                colorInt = Cobalt.parseColor(color);
                applyColor = true;
            }
            catch (IllegalArgumentException colorException) {
                if (Cobalt.DEBUG) {
                    Log.w(Cobalt.TAG, TAG + " - addMenuItem: color " + color + " format not supported, use (#)RGB or (#)RRGGBB(AA).");
                }
                colorException.printStackTrace();

                try {
                    colorInt = Cobalt.parseColor(barsColor);
                    applyColor = true;
                }
                catch (IllegalArgumentException barsColorException) {
                    if (Cobalt.DEBUG) {
                        Log.w(Cobalt.TAG, TAG + " - addMenuItem: barscolor " + barsColor + " format not supported, use (#)RGB or (#)RRGGBB(AA).");
                    }
                    barsColorException.printStackTrace();
                }
            }

            ActionViewMenuItem actionView = new ActionViewMenuItem(this, action, barsColor);

            MenuItemCompat.setActionView(menuItem, actionView);
            menuItem.setVisible(visible);
            menuItem.setEnabled(enabled);
            mMenuItemsHashMap.put(name, actionView);
            //need this next hashmap to send onPressed when item is on overflow
            mMenuItemsIdMap.put(id, name);
        }
        catch (JSONException exception) {
            if (Cobalt.DEBUG) {
                Log.w(Cobalt.TAG, TAG + "addMenuItem: action " + action.toString() + " format not supported, use at least {\n"
                        + "\tname: \"name\",\n"
                        + "\ttitle: \"title\",\n"
                        + "}");
            }

            exception.printStackTrace();
        }
    }



    public int getResourceIdentifier(String resource) {
        int resId = 0;

        try {
            if (resource == null || resource.length() == 0) {
                throw new IllegalArgumentException();
            }

            String[] resourceSplit = resource.split(":");
            String packageName, resourceName;
            switch(resourceSplit.length) {
                case 1:
                    packageName = getPackageName();
                    resourceName = resourceSplit[0];
                    break;
                case 2:
                    packageName = resourceSplit[0].length() != 0 ? resourceSplit[0] : getPackageName();
                    resourceName = resourceSplit[1];
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            resId = getResources().getIdentifier(resourceName, "drawable", packageName);
            if (resId == 0) {
                Log.w(Cobalt.TAG, TAG + "getResourceIdentifier: resource " + resource + " not found.");
            }
        }
        catch (IllegalArgumentException exception) {
            if (Cobalt.DEBUG) {
                Log.w(Cobalt.TAG, TAG + "getResourceIdentifier: resource " + resource + " format not supported, use resource, :resource or package:resource.");
            }
            exception.printStackTrace();
        }

        return resId;
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

    @Override
    public void onPressed(String name) {
        CobaltFragment fragment = (CobaltFragment) getSupportFragmentManager().findFragmentById(getFragmentContainerId());
        JSONObject message = new JSONObject();
        JSONObject data = new JSONObject();
        try {
            message.put(Cobalt.kJSType, Cobalt.JSTypeUI);
            message.put(Cobalt.kJSUIControl, Cobalt.JSControlBars);
            data.put(Cobalt.kJSAction, Cobalt.JSActionActionPressed);
            data.put(Cobalt.kJSActionName, name);
            message.put(Cobalt.kJSData, data);
            fragment.sendMessage(message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
