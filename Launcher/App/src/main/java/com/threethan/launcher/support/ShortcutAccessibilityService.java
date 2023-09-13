package com.threethan.launcher.support;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.threethan.launcher.R;
import com.threethan.launcher.launcher.LauncherActivityShortcutService;

import java.util.Timer;
import java.util.TimerTask;

public class ShortcutAccessibilityService extends AccessibilityService {
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String eventText = event.getText().toString();
            String exploreAccessibilityEventName = getResources().getString(R.string.accessibility_event_name);
            if (exploreAccessibilityEventName.compareTo(eventText) == 0) {
                Intent finishIntent = new Intent(LauncherActivityShortcutService.FINISH_ACTION);
                sendBroadcast(finishIntent);

                Intent launchIntent = new Intent(this, LauncherActivityShortcutService.class);
                launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

                Log.i("LightningLauncherService", "Opening launcher activity from accessibility event");
                startActivity(launchIntent);
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        startActivity(launchIntent);
                    }
                }, 650);
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        startActivity(launchIntent);
                    }
                }, 800);
            }
        }
    }
    public void onInterrupt() {}
}