package com.ergom.launcher;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.text.InputType;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;
    private static final String KIOSK_PACKAGE = "com.ergom.launcher";
    private static final String CHROME_PACKAGE = "com.android.chrome";
    private static final String[] APP_PACKAGES = {KIOSK_PACKAGE, CHROME_PACKAGE};
    private TextView timeTextView;
    private Handler handler;
    private SimpleDateFormat timeFormat;
    private TextView ipAddressTextView;
    private Timer timer;
    private static final int MAX_CLICK_COUNT = 5;
    private int clickCount = 0;
    private static final String PASSWORD = "qwerty";


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
        wakeLock.acquire(10*60*1000L /*10 minutes*/);
        setContentView(R.layout.activity_main);

        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName deviceAdmin = new ComponentName(this, MyDeviceAdminReceiver.class);

        Button startAppButton = findViewById(R.id.startAppButton);
        startAppButton.setOnClickListener(v -> openWebsite("http://192.168.42.237/"));

        Button startAppButton3 = findViewById(R.id.startAppButton3);
        startAppButton3.setOnClickListener(v -> openWebsite("https://www.ergom.com/"));

        Button startAppButton4 = findViewById(R.id.startAppButton4);
        startAppButton4.setOnClickListener(v -> openWebsite("http://192.168.42.115/ergom/"));

        if (devicePolicyManager.isDeviceOwnerApp(getPackageName())) {
            setKioskModeEnabled();
        } else {
            //Sluzy do nadania praw administratora aplikacji z poziomu tableta, ale nie dziala, bo wywala aplikacje i potem i tak trzeba nadac przez ADB
            //TODO - nadanie praw administratora
            //Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            //intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdmin);
            //intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable admin to start kiosk mode");
            //startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
        }

        timeTextView = findViewById(R.id.timeTextView);
        handler = new Handler();

        // Format godziny (np. 12:34 PM)
        timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        // Cykliczne odświeżanie co sekundę
        handler.postDelayed(timeRunnable, 1000);

        ipAddressTextView = findViewById(R.id.ipAddressTextView);

        // Pobranie adresu IP urządzenia
        String ipAddress = getDeviceIpAddress();

        // Wyświetlenie adresu IP w TextView
        ipAddressTextView.setText(ipAddress);

        // Uruchomienie timera do odświeżania adresu IP co 5 sekund
        startTimer();

        ipAddressTextView.setOnClickListener(v -> {
            clickCount++;
            if (clickCount >= MAX_CLICK_COUNT) {
                showPasswordDialog();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            if (resultCode == Activity.RESULT_OK) {
                setKioskModeEnabled();
            }
        }
    }

    private void setKioskModeEnabled() {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = new ComponentName(this, MyDeviceAdminReceiver.class);

        devicePolicyManager.setLockTaskPackages(adminComponent, APP_PACKAGES);
        startLockTask();
    }

    private void openWebsite(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private final Runnable timeRunnable = new Runnable() {
        @Override
        public void run() {
            updateTime();
            handler.postDelayed(this, 1000);
        }
    };

    private void updateTime() {
        Calendar calendar = Calendar.getInstance();
        String currentTime = timeFormat.format(calendar.getTime());
        timeTextView.setText(currentTime);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Zatrzymanie cyklicznego odświeżania po zniszczeniu aktywności
        handler.removeCallbacks(timeRunnable);
    }

    private String getDeviceIpAddress() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        int ipAddress = wifiInfo.getIpAddress();
        @SuppressLint("DefaultLocale") String ipAddressString = String.format(
                "%d.%d.%d.%d",
                (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff),
                (ipAddress >> 24 & 0xff)
        );

        return ipAddressString;
    }
    private void startTimer() {
        timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                // Aktualizacja adresu IP na głównym wątku
                runOnUiThread(() -> {
                    String ipAddress = getDeviceIpAddress();
                    ipAddressTextView.setText(ipAddress);
                });
            }
        };

        // Uruchomienie timera co 5 sekund
        timer.schedule(timerTask, 5000, 5000);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Zatrzymanie timera po zniszczeniu activity
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void showPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Proszę wprowadzić hasło");

        final EditText passwordInput = new EditText(this);
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(passwordInput);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String enteredPassword = passwordInput.getText().toString();
            if (enteredPassword.equals(PASSWORD)) {
                exitLockTask();
            } else {
                Toast.makeText(MainActivity.this, "Nieprawidłowe hasło", Toast.LENGTH_SHORT).show();
            }
            clickCount = 0;
        });

        builder.setNegativeButton("Anuluj", (dialog, which) -> clickCount = 0);

        builder.create().show();
    }

    private void exitLockTask() {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (devicePolicyManager != null) {
            if (devicePolicyManager.isLockTaskPermitted(getPackageName())) {
                stopLockTask();
            }
        }
    }
}

