package local.homelab.simwipe;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/** Android requires this of a default SMS app (quick-reply to a declined call). Not supported here. */
public class RespondService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        stopSelf(startId);
        return START_NOT_STICKY;
    }
}
