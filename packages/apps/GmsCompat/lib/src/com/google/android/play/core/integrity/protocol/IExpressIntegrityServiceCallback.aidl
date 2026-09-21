package com.google.android.play.core.integrity.protocol;

import android.os.Bundle;

interface IExpressIntegrityServiceCallback {
    oneway void onRequestExpressIntegrityTokenResult(in Bundle result) = 2;
}
