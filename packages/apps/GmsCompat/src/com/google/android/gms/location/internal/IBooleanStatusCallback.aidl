package com.google.android.gms.location.internal;

import com.google.android.gms.common.api.Status;

interface IBooleanStatusCallback {
    oneway void onResult(in Status status, boolean result) = 0;
}
