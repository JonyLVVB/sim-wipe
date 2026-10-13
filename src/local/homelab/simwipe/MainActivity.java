package local.homelab.simwipe;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.Settings;
import android.provider.Telephony;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * One button: borrow the default-SMS-app role (only that app may delete texts), delete all
 * SMS/MMS and the call log, show what is left, then hand texting back to Google Messages.
 */
public class MainActivity extends Activity {
    private static final int REQ_ROLE = 1;
    private static final int REQ_PERMS = 2;
    private static final String MESSAGES_PKG = "com.google.android.apps.messaging";
    private static final Uri SMS = Uri.parse("content://sms");
    private static final Uri MMS = Uri.parse("content://mms");
    private static final Uri THREADS = Uri.parse("content://mms-sms/conversations");
    private static final String[] PERMS = {
            Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.READ_SMS,
    };

    private TextView status;
    private TextView result;
    private Button handBack;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        int pad = dp(16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        // fitsSystemWindows replaces a view's own padding, so it goes on a wrapper
        LinearLayout frame = new LinearLayout(this);
        frame.setFitsSystemWindows(true);
        frame.addView(root, new LinearLayout.LayoutParams(-1, -1));

        status = new TextView(this);
        status.setTextSize(17);
        root.addView(status);

        Button wipe = new Button(this);
        wipe.setText("WIPE MESSAGES + CALLS");
        wipe.setTextSize(24);
        wipe.setTextColor(Color.WHITE);
        wipe.setBackgroundColor(0xFFB00020);
        wipe.setMinHeight(dp(120));
        wipe.setOnClickListener(v -> startWipe());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(16), 0, dp(16));
        root.addView(wipe, lp);

        result = new TextView(this);
        result.setGravity(Gravity.CENTER);
        result.setTextColor(Color.WHITE);
        result.setTextSize(26);
        result.setPadding(pad, dp(28), pad, dp(28));
        result.setVisibility(View.GONE);
        root.addView(result, new LinearLayout.LayoutParams(-1, -2));

        handBack = new Button(this);
        handBack.setText("Give texting back to Messages");
        handBack.setTextSize(18);
        handBack.setMinHeight(dp(72));
        handBack.setOnClickListener(v -> handBack());
        root.addView(handBack, lp);

        setContentView(frame);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    protected void onActivityResult(int request, int code, Intent data) {
        super.onActivityResult(request, code, data);
        if (request == REQ_ROLE && isDefaultSmsApp()) wipe();
    }

    @Override
    public void onRequestPermissionsResult(int request, String[] perms, int[] results) {
        if (request == REQ_PERMS && missingPerms().isEmpty()) startWipe();
        refresh();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private boolean isDefaultSmsApp() {
        return getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(this));
    }

    private ArrayList<String> missingPerms() {
        ArrayList<String> missing = new ArrayList<>();
        for (String p : PERMS) {
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) missing.add(p);
        }
        return missing;
    }

    /** Row count, or -1 when the query itself failed; a failure must never read as "0 left". */
    private int count(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, new String[]{"_id"}, null, null, null)) {
            return c == null ? -1 : c.getCount();
        } catch (Exception e) {
            return -1;
        }
    }

    private static String show(int n) {
        return n < 0 ? "?" : String.valueOf(n);
    }

    private void refresh() {
        boolean isDefault = isDefaultSmsApp();
        handBack.setVisibility(isDefault ? View.VISIBLE : View.GONE);
        status.setText("On this phone now:  " + show(count(SMS)) + " SMS,  " + show(count(MMS)) + " MMS,  "
                + show(count(CallLog.Calls.CONTENT_URI)) + " calls"
                + (isDefault ? "\n\nSIM Wipe is holding the texting role. Give it back to Messages below." : ""));
    }

    /** Permissions first (one-time), then the SMS role (every time), then the wipe itself. */
    private void startWipe() {
        result.setVisibility(View.GONE);
        ArrayList<String> missing = missingPerms();
        if (!missing.isEmpty()) {
            requestPermissions(missing.toArray(new String[0]), REQ_PERMS);
            return;
        }
        if (!isDefaultSmsApp()) {
            // Android's own "set as default SMS app?" prompt doubles as the confirmation
            RoleManager roles = getSystemService(RoleManager.class);
            startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_SMS), REQ_ROLE);
            return;
        }
        wipe();
    }

    private void delete(Uri uri) {
        try {
            getContentResolver().delete(uri, null, null);
        } catch (Exception ignored) {
            // the recount below decides PASS/FAIL
        }
    }

    private void wipe() {
        delete(SMS);
        delete(MMS);
        delete(THREADS);
        delete(CallLog.Calls.CONTENT_URI);

        int sms = count(SMS), mms = count(MMS), calls = count(CallLog.Calls.CONTENT_URI);
        boolean pass = sms == 0 && mms == 0 && calls == 0;
        result.setBackgroundColor(pass ? 0xFF1B7F3B : 0xFFB00020);
        result.setText((pass ? "PASS" : "FAIL") + "\n\nSMS left: " + show(sms) + "\nMMS left: " + show(mms)
                + "\nCalls left: " + show(calls) + (pass ? "" : "\n\n? = could not be counted"));
        result.setVisibility(View.VISIBLE);
        refresh();
    }

    /** Only the user can change the SMS role; Messages asks for it itself when opened. */
    private void handBack() {
        Intent messages = getPackageManager().getLaunchIntentForPackage(MESSAGES_PKG);
        try {
            startActivity(messages != null ? messages : new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }
}
