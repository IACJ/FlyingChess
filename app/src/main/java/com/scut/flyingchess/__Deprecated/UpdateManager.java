package com.scut.flyingchess.__Deprecated;

import android.support.v7.app.AppCompatActivity;

/**
 * Created by karthur on 2016/5/12.
 *
 * Deprecated by IACJ on 2018/4/9
 */
@Deprecated
public class UpdateManager {
    public UpdateWorker uw;
    private boolean checked;

    public UpdateManager(AppCompatActivity activity) {
        uw = new UpdateWorker(activity);
        checked = false;
    }

    public void checkUpdate() {
        if (checked == false) {
            new Thread(uw).start();
        }
        checked = true;
    }
}

