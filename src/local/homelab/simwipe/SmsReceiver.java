package local.homelab.simwipe;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

/**
 * While this app briefly holds the SMS role it is the only one that stores incoming texts, so
 * save them; Messages shows them once it has the role back.
 */
public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(intent.getAction())) return;
        SmsMessage[] parts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (parts == null || parts.length == 0) return;
        StringBuilder text = new StringBuilder();
        for (SmsMessage part : parts) {
            if (part.getMessageBody() != null) text.append(part.getMessageBody());
        }
        ContentValues v = new ContentValues();
        v.put("address", parts[0].getOriginatingAddress());
        v.put("body", text.toString());
        v.put("date", System.currentTimeMillis());
        v.put("read", 0);
        v.put("seen", 0);
        int subId = intent.getIntExtra("subscription", -1);
        if (subId >= 0) v.put("sub_id", subId);
        context.getContentResolver().insert(Telephony.Sms.Inbox.CONTENT_URI, v);
    }
}
