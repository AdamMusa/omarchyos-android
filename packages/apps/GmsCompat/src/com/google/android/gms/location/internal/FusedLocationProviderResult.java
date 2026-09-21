package com.google.android.gms.location.internal;

import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.common.api.Status;

@SafeParcelable.Class(creator = "FusedLocationProviderResultCreator")
public class FusedLocationProviderResult extends AbstractSafeParcelable {
    public static final FusedLocationProviderResult SUCCESS = new FusedLocationProviderResult(Status.SUCCESS);

    @Field(id = 1) public Status status;

    @Constructor
    public FusedLocationProviderResult(@Param(id = 1) Status status) {
        this.status = status;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        FusedLocationProviderResultCreator.writeToParcel(this, dest, flags);
    }

    public static final Parcelable.Creator<FusedLocationProviderResult> CREATOR = null;
}
