/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modified by anton@malinskiy.com
 */

package de.asideas.overlay.controls.internal.qcontrols;

import android.content.Context;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import de.asideas.overlay.controls.R;

/**
 * Controller for Quick Controls pie menu
 */
public class PieControl
{
    protected Context mActivity;

    protected PieMenu mPie;

    protected int mItemSize;

    public PieControl(Context activity)
    {
        mActivity = activity;
        mItemSize = (int) activity.getResources().getDimension(R.dimen.qc_item_size);
    }

    public void attachToContainer(WindowManager windowManager, WindowManager.LayoutParams layoutParams)
    {
        if (mPie == null)
        {
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT);
            mPie = new PieMenu(mActivity);
            mPie.setLayoutParams(lp);
            populateMenu();
        }
        windowManager.addView(mPie, layoutParams);
    }

    public void attachToContainer(ViewGroup container)
    {
        if (mPie == null)
        {
            mPie = new PieMenu(mActivity);
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT);
            mPie.setLayoutParams(lp);
            populateMenu();
        }
        container.addView(mPie);
    }

    protected void removeFromContainer(ViewGroup container)
    {
        container.removeView(mPie);
    }

    public void forceToTop(ViewGroup container)
    {
        if (mPie.getParent() != null)
        {
            container.removeView(mPie);
            container.addView(mPie);
        }
    }

    public void setClickListener(PieItem item, OnClickListener listener)
    {
        item.getView().setOnClickListener(listener);
    }

    protected void populateMenu()
    {
    }

    public PieItem makeItem(int image, int l)
    {
        ImageView view = new ImageView(mActivity);
        view.setImageResource(image);
        view.setMinimumWidth(mItemSize);
        view.setMinimumHeight(mItemSize);
        view.setScaleType(ScaleType.CENTER);
        LayoutParams lp = new LayoutParams(mItemSize, mItemSize);
        view.setLayoutParams(lp);
        return new PieItem(view, l);
    }

    protected PieItem makeFiller()
    {
        return new PieItem(null, 1);
    }

    public void show()
    {
        mPie.show(true);
    }
}