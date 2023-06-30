package com.ergom.launcher;


import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.os.PowerManager;

public class MainActivity extends Activity {

    private DevicePolicyManager devicePolicyManager;
    private ComponentName deviceAdmin;
    private Button startAppButton;
    private Button startAppButton3;
    private Button startAppButton4;
    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;
    private static final String KIOSK_PACKAGE = "com.ergom.launcher";
    private static final String CHROME_PACKAGE = "com.android.chrome";
    private static final String[] APP_PACKAGES = {KIOSK_PACKAGE, CHROME_PACKAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Pobierz menedżer zasilania i ekranu
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        WindowManager.LayoutParams params = getWindow().getAttributes();

        // Włącz flagi utrzymania ekranu i wyłączenia blokady ekranu
        params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        params.flags |= WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;

        // Ustaw atrybuty dla okna
        getWindow().setAttributes(params);

        // Zadbaj o to, aby CPU pozostał włączony
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "myapp:com.ergom.launcher");
        wakeLock.acquire();
        setContentView(R.layout.activity_main);

        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        deviceAdmin = new ComponentName(this, MyDeviceAdminReceiver.class);

        startAppButton = findViewById(R.id.startAppButton);
        startAppButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWebsite("http://192.168.42.237/");
            }
        });

        startAppButton3 = findViewById(R.id.startAppButton3);
        startAppButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWebsite("https://www.ergom.com/");
            }
        });

        startAppButton4 = findViewById(R.id.startAppButton4);
        startAppButton4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWebsite("http://192.168.42.115/ergom/");
            }
        });

        if (devicePolicyManager.isDeviceOwnerApp(getPackageName())) {
            setKioskModeEnabled(true);
        } else {
            //Sluzy do nadania praw administratora aplikacji z poziomu tableta, ale nie dziala, bo wywala aplikacje i potem i tak trzeba nadac przez ADB
            //TODO - nadanie praw administratora
            //Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            //intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdmin);
            //intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable admin to start kiosk mode");
            //startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            if (resultCode == Activity.RESULT_OK) {
                setKioskModeEnabled(true);
            }
        }
    }

    private void setKioskModeEnabled(boolean enabled) {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = new ComponentName(this, MyDeviceAdminReceiver.class);

        if (enabled) {
            devicePolicyManager.setLockTaskPackages(adminComponent, APP_PACKAGES);
            startLockTask();
        } else {
            stopLockTask();
        }
    }

    private void openWebsite(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

}