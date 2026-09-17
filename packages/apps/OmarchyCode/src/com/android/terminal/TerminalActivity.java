/*
 * Copyright (C) 2013 The Android Open Source Project
 * Copyright 2026 OmarchyOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */
package com.android.terminal;

import static com.android.terminal.Terminal.TAG;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toolbar;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.io.File;

/** Displays real PTY sessions backed by the on-device Codex, Claude, or shell runtime. */
public final class TerminalActivity extends Activity {
    public static final String EXTRA_SESSION = "com.android.terminal.extra.SESSION";

    private TerminalService mService;
    private ViewPager mPager;
    private Toolbar mToolbar;
    private String mRequestedSession = TerminalService.SESSION_SHELL;

    private final ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((TerminalService.ServiceBinder) service).getService();
            Log.d(TAG, "Bound to Code service with " + mService.getTerminals().size()
                    + " active terminals");
            selectRequestedSession();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    private final PagerAdapter mTermAdapter = new PagerAdapter() {
        private final SparseArray<SparseArray<Parcelable>> mSavedState = new SparseArray<>();

        @Override
        public int getCount() {
            return mService == null ? 0 : mService.getTerminals().size();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            TerminalView view = new TerminalView(container.getContext());
            view.setId(View.generateViewId());

            Terminal terminal = mService.getTerminals().valueAt(position);
            view.setTerminal(terminal);

            SparseArray<Parcelable> state = mSavedState.get(terminal.key);
            if (state != null) {
                view.restoreHierarchyState(state);
            }

            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            TerminalView view = (TerminalView) object;
            int key = view.getTerminal().key;
            SparseArray<Parcelable> state = mSavedState.get(key);
            if (state == null) {
                state = new SparseArray<>();
                mSavedState.put(key, state);
            }
            view.saveHierarchyState(state);
            view.setTerminal(null);
            container.removeView(view);
        }

        @Override
        public int getItemPosition(Object object) {
            if (mService == null) {
                return POSITION_NONE;
            }
            TerminalView view = (TerminalView) object;
            int index = mService.getTerminals().indexOfKey(view.getTerminal().key);
            return index == -1 ? POSITION_NONE : index;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        readRequestedSession(getIntent());
        setContentView(R.layout.activity);

        mToolbar = findViewById(R.id.toolbar);
        setActionBar(mToolbar);
        int backIcon = getResources().getIdentifier(
                "ic_ab_back_material", "drawable", "android");
        if (backIcon != 0) {
            mToolbar.setNavigationIcon(backIcon);
        }
        mToolbar.setNavigationContentDescription(
                com.android.internal.R.string.action_bar_up_description);
        mToolbar.setNavigationOnClickListener(view -> finish());

        mPager = findViewById(R.id.pager);
        mPager.setAdapter(mTermAdapter);
        mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                updateTitle(position);
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        readRequestedSession(intent);
        if (mService != null) {
            selectRequestedSession();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, TerminalService.class), mServiceConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mService != null) {
            unbindService(mServiceConn);
            mService = null;
        }
    }

    private void readRequestedSession(Intent intent) {
        String requested = intent == null ? null : intent.getStringExtra(EXTRA_SESSION);
        if (TerminalService.SESSION_CODEX.equals(requested)
                || TerminalService.SESSION_CLAUDE.equals(requested)
                || TerminalService.SESSION_SHELL.equals(requested)) {
            mRequestedSession = requested;
        }
    }

    private void selectRequestedSession() {
        File home = HubActivity.homeDirectory(this);
        File workspace = HubActivity.workspaceDirectory(this);
        int key = mService.getOrCreateTerminal(
                mRequestedSession, home.getAbsolutePath(), workspace.getAbsolutePath());
        mTermAdapter.notifyDataSetChanged();
        invalidateOptionsMenu();
        int index = mService.getTerminals().indexOfKey(key);
        mPager.post(() -> {
            mPager.setCurrentItem(index, false);
            updateTitle(index);
        });
    }

    private void updateTitle(int position) {
        if (mService == null || position < 0 || position >= mService.getTerminals().size()) {
            mToolbar.setTitle(R.string.app_label);
            return;
        }
        mToolbar.setTitle(mService.getTerminals().valueAt(position).getTitle());
    }

    private TerminalView currentTerminalView() {
        if (mService == null || mService.getTerminals().size() == 0) {
            return null;
        }
        int key = mService.getTerminals().keyAt(mPager.getCurrentItem());
        for (int index = 0; index < mPager.getChildCount(); index++) {
            View child = mPager.getChildAt(index);
            if (child instanceof TerminalView) {
                TerminalView terminalView = (TerminalView) child;
                if (terminalView.getTerminal() != null
                        && terminalView.getTerminal().key == key) {
                    return terminalView;
                }
            }
        }
        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_close_tab).setEnabled(mTermAdapter.getCount() > 0);
        menu.findItem(R.id.menu_keyboard).setEnabled(mTermAdapter.getCount() > 0);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mService == null) {
            return false;
        }
        switch (item.getItemId()) {
            case R.id.menu_keyboard:
                TerminalView current = currentTerminalView();
                if (current != null) {
                    current.showKeyboard();
                }
                return true;
            case R.id.menu_new_tab:
                File home = HubActivity.homeDirectory(this);
                File workspace = HubActivity.workspaceDirectory(this);
                int key = mService.createTerminal(
                        mRequestedSession, home.getAbsolutePath(), workspace.getAbsolutePath());
                mTermAdapter.notifyDataSetChanged();
                int index = mService.getTerminals().indexOfKey(key);
                mPager.setCurrentItem(index, true);
                invalidateOptionsMenu();
                return true;
            case R.id.menu_close_tab:
                int currentIndex = mPager.getCurrentItem();
                int currentKey = mService.getTerminals().keyAt(currentIndex);
                mService.destroyTerminal(currentKey);
                mTermAdapter.notifyDataSetChanged();
                invalidateOptionsMenu();
                if (mService.getTerminals().size() == 0) {
                    finish();
                }
                return true;
            case R.id.menu_home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
