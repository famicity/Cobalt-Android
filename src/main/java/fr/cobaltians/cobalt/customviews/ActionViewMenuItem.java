package fr.cobaltians.cobalt.customviews;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import fr.cobaltians.cobalt.Cobalt;
import fr.cobaltians.cobalt.R;
import fr.cobaltians.cobalt.activities.CobaltActivity;

/**
 * Created by sebastienfamel on 10/12/15.
 */
public class ActionViewMenuItem extends RelativeLayout {

    private String mName;
    private JSONObject mAction;
    private String mColor;
    private ActionViewMenuItemListener mListener;
    private Context mContext;

    private TextView mBadgeTv;

    LayoutInflater mInflater;

    public ActionViewMenuItem(Context context) {
        super(context);
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        init();
    }

    public ActionViewMenuItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        init();
    }

    public ActionViewMenuItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        init();
    }

    public ActionViewMenuItem(Context context, JSONObject action, String barsColor/*, ActionViewMenuItemListener listener*/) {
        super(context);
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        try {
            this.mName = action.getString(Cobalt.kActionName);
            this.mAction = action;
            this.mColor = barsColor;
            this.mListener = (ActionViewMenuItemListener) context;

        } catch (JSONException e) {
            e.printStackTrace();
        }
        init();
    }

    public void init() {
        mInflater.inflate(R.layout.custom_action_view_menu_item, this, true);
        try {
            String title = mAction.getString(Cobalt.kActionTitle);
            String icon = mAction.optString(Cobalt.kActionIcon, null);               // must be "fontKey character"
            String androidIcon = mAction.optString(Cobalt.kActionAndroidIcon, null);
            String color = mAction.optString(Cobalt.kActionColor, null);             // default: same as bar color
            boolean visible = mAction.optBoolean(Cobalt.kActionVisible, true);
            boolean enabled = mAction.optBoolean(Cobalt.kActionEnabled, true);
            String badge = mAction.optString(Cobalt.kActionBadge, null);             // if "", hide it

            ImageButton imageButton = (ImageButton) findViewById(R.id.image_button_item);
            Button button = (Button) findViewById(R.id.button_item);

            if (androidIcon != null || icon != null) {

                int idResource;
                if (androidIcon != null) idResource = getResource(androidIcon);
                else idResource = getResource(icon);

                if (idResource != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    imageButton.setImageDrawable(mContext.getDrawable(idResource));
                }

                imageButton.setEnabled(enabled);

                if (visible) {
                    imageButton.setVisibility(VISIBLE);
                }
                else imageButton.setVisibility(GONE);

                if (badge != null && badge.length()>0) {
                    mBadgeTv = (TextView) findViewById(R.id.badge_item);
                    mBadgeTv.setText(badge);
                    mBadgeTv.setVisibility(VISIBLE);
                    /*
                    //TODO see how to create and update gradientDrawable
                        GradientDrawable drawableBadge = new GradientDrawable();
                        drawableBadge.setShape(GradientDrawable.RECTANGLE);
                        drawableBadge.setCornerRadius(20);
                        drawableBadge.setColor(getHexaColor("#F00"));
                        drawableBadge.setStroke(1, Cobalt.parseColor(color));
                     */
                }
                imageButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((CobaltActivity)mContext).onPressed(mName);
                    }
                });
                // TODO: add toast tooltip OnLongClickListener with title anchored on MenuItem
            }
            else {
                button.setText(title);
                if (color != null) button.setTextColor(Cobalt.parseColor(color));
                button.setEnabled(enabled);
                button.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((CobaltActivity)mContext).onPressed(mName);
                    }
                });
                // TODO: apply color for items in overflow popup
            }

            // TODO: find best background to mimic default behavior
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int[] attrs = {android.R.attr.selectableItemBackgroundBorderless};
                TypedArray styledAttrs = mContext.getTheme().obtainStyledAttributes(attrs);
                Drawable backgroundDrawable = styledAttrs.getDrawable(0);
                setBackground(backgroundDrawable);
            }
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                int[] attrs = {android.R.attr.selectableItemBackground};
                TypedArray styledAttrs = mContext.getTheme().obtainStyledAttributes(attrs);
                int backgroundResId = styledAttrs.getResourceId(0, 0);
                setBackgroundResource(backgroundResId);
            }
            else {
                setBackgroundResource(android.R.drawable.menuitem_background);
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public int getResource(String imageLink) {
        if (!imageLink.contains(":")) {
            return this.getResources().getIdentifier(imageLink,"drawable", mContext.getPackageName());
        }
        else {
            try {
                String [] splitLink = imageLink.split(":");
                String packName = splitLink[0];
                String drawableName = splitLink[1];
                PackageManager manager = mContext.getPackageManager();
                Resources apkResource = manager.getResourcesForApplication(packName);
                return apkResource.getIdentifier(drawableName, "drawable", packName);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return 0;
            }
        }
    }

    public String getName(){ return mName;}

    public void setActionBadge(String text) {
        if (text.length()>0 && !text.equals("")) {
            mBadgeTv.setText(text);
        }
    }

    public void setActionContext(Context context) {
        //TODO something
    }
}
