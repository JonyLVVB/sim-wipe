package local.homelab.simwipe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Required of a default SMS app. A picture message arriving during a wipe is not downloaded. */
public class MmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
    }
}
