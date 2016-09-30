/**
 *
 * OverScrollingWebView
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

package org.cobaltians.cobalt.customviews;

import org.cobaltians.cobalt.fragments.CobaltFragment;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebView;

public class OverScrollingWebView extends WebView {

	/**
	 * Fragment handling scroll events
	 */
	protected CobaltFragment mScrollListener;

	public OverScrollingWebView(Context context) {
		super(context);
	}

	public OverScrollingWebView(Context context, AttributeSet attributes) {
		super(context, attributes);
	}
	
	public OverScrollingWebView(Context context, AttributeSet attributes, int defaultStyle) {
		super(context, attributes, defaultStyle);
	}

	public void setScrollListener(CobaltFragment scrollListener) {
		mScrollListener = scrollListener;
	}

	/**
	 * Notifies listener of scrolling
	 */
	@Override
	protected void onScrollChanged(int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
		super.onScrollChanged(scrollX, scrollY, oldScrollX, oldScrollY);
		
		if (mScrollListener != null
			&& IScrollListener.class.isAssignableFrom(mScrollListener.getClass())) {
			mScrollListener.onOverScrolled(scrollX, scrollY, oldScrollX, oldScrollY);
		}
	}

	/*
	 * TODO: do not delete this time!
	 * Overwrite this method and calling onScrollChanged fixes an issue about "a race condition where
	 * the webkit layout engine gets out of sync with the Android layout and starts mapping touch
	 * events to an incorrect coordinate system" leading to "all touches fall outside of the window,
	 * and get rejected."
	 * source: https://stackoverflow.com/questions/12090899/android-webview-jellybean-should-not-happen-no-rect-based-test-nodes-found#comment-22698848
	 * "I met with John Reck, an engineer on the Android WebView team, a couple of days ago. He
	 * confirmed that this is a bug--basically a race condition where the webkit layout engine gets
	 * out of sync with the Android layout and starts mapping touch events to an incorrect
	 * coordinate system. Until the bug is fixed, calling onScrollChanged() to resync the layouts
	 * seems to be the best workaround"
     */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
			&& Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			onScrollChanged(getScrollX(), getScrollY(), getScrollX(), getScrollY());
		}

		return super.onTouchEvent(event);
	}
}
