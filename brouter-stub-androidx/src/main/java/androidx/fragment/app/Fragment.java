package androidx.fragment.app;

import android.content.Intent;
import android.os.Bundle;


public class Fragment {

    public final FragmentActivity getActivity() {
        throw new AssertionError();
    }

    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        throw new AssertionError();
    }

    public void startActivity(Intent intent, Bundle options) {
        throw new AssertionError();
    }
}
