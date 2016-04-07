/**
 *
 * CobaltPluginWebContainer
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

package org.cobaltians.cobalt.plugin;

import org.cobaltians.cobalt.fragments.CobaltFragment;

import android.app.Activity;

import java.lang.ref.WeakReference;

public final class CobaltPluginWebContainer {

	/**************************************
     * MEMBERS
     **************************************/
	
	private final WeakReference <Activity> mActivity;
	private final WeakReference <CobaltFragment> mFragment;
	
	/****************************************************************************
     * CONSTRUCTORS
     ****************************************************************************/
	
	public CobaltPluginWebContainer(Activity activity, CobaltFragment fragment) {
		mActivity = new WeakReference<>(activity);
		mFragment = new WeakReference<>(fragment);
	}

	/************************************
     * GETTERS
     ************************************/
	
	public Activity getActivity() {
		return mActivity.get();
	}

	public CobaltFragment getFragment() {
		return mFragment.get();
	}
}
