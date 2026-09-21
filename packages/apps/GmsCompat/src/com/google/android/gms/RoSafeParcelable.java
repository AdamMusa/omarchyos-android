package com.google.android.gms;

import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.os.Parcel;

public abstract class RoSafeParcelable extends AbstractSafeParcelable {
    @Override
    public final void writeToParcel(Parcel dest, int flags) {
        throw new IllegalStateException("this is a read-only SafeParcelable");
    }
}
