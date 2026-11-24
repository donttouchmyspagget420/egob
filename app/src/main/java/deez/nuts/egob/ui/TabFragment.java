package deez.nuts.egob.ui;

import androidx.annotation.IdRes;

/** Implemented by every fragment that owns a bottom navigation tab. */
public interface TabFragment {
    @IdRes
    int getTabId();
}
