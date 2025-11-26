package ir.shecan.activity.mainActivityUtils;

import android.app.Activity;
import android.content.Intent;
import android.net.VpnService;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import ir.shecan.Shecan;
import ir.shecan.service.ShecanVpnService;
import ir.shecan.util.server.DNSServerHelper;

/**
 * Helper class to handle VPN activation and permission request.
 */
public class VpnManager {

    private final Activity activity;
    private ActivityResultLauncher<Intent> vpnPermissionLauncher;

    public VpnManager(@NonNull Activity activity) {
        this.activity = activity;
        registerVpnLauncher();
    }

    private void registerVpnLauncher() {
        if (!(activity instanceof androidx.fragment.app.FragmentActivity)) return;

        vpnPermissionLauncher = ((androidx.fragment.app.FragmentActivity) activity)
                .registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == Activity.RESULT_OK) {
                                onVpnPermissionGranted();
                            }
                        }
                );
    }

    /**
     * Starts VPN activation process
     */
    public void startVpnActivation() {
        Intent intent = VpnService.prepare(activity);
        if (intent != null) {
            if (intent.resolveActivity(activity.getPackageManager()) != null) {
                vpnPermissionLauncher.launch(intent);
            } else {
                Toast.makeText(activity, "دستگاه شما از VPN داخلی پشتیبانی نمی‌کند.", Toast.LENGTH_LONG).show();
            }
        } else {
            onVpnPermissionGranted();
        }

        incrementActivateCounter();
    }

    private void incrementActivateCounter() {
        long counter = Shecan.configurations.getActivateCounter();
        if (counter != -1) {
            Shecan.configurations.setActivateCounter(++counter);
        }
    }

    private void onVpnPermissionGranted() {
        setupDnsServers();
        Shecan.getInstance().startService(
                Shecan.getServiceIntent(activity.getApplicationContext())
                        .setAction(ShecanVpnService.ACTION_ACTIVATE)
        );
        Shecan.updateShortcut(activity.getApplicationContext());
    }

    private void setupDnsServers() {
        if (ShecanVpnService.isProMode()) {
            ShecanVpnService.primaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getProPrimary());
            ShecanVpnService.secondaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getProSecondary());
        } else {
            ShecanVpnService.primaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getPrimary());
            ShecanVpnService.secondaryServer = DNSServerHelper.getDNSById(DNSServerHelper.getSecondary());
        }
    }

    /**
     * Call this from Activity's onActivityResult
     */
    public void handleActivityResult(int resultCode) {
        if (resultCode == Activity.RESULT_OK) {
            setupDnsServers();
            Shecan.getInstance().startService(
                    Shecan.getServiceIntent(activity.getApplicationContext())
                            .setAction(ShecanVpnService.ACTION_ACTIVATE)
            );
            Shecan.updateShortcut(activity.getApplicationContext());
        }
    }
}
