package dev.osunolimits.plugins;

import dev.osunolimits.plugins.events.actions.OnAccountDeletionEvent;
import dev.osunolimits.plugins.events.actions.OnBeatmapFavoriteEvent;
import dev.osunolimits.plugins.events.actions.OnGfxDeletionEvent;
import dev.osunolimits.plugins.events.actions.OnRegisterEvent;
import dev.osunolimits.plugins.events.actions.OnUserFlagChangeEvent;
import dev.osunolimits.plugins.events.actions.OnUserNameChangeEvent;
import dev.osunolimits.plugins.events.admin.OnAddDonorEvent;
import dev.osunolimits.plugins.events.admin.OnAuditLogEvent;
import dev.osunolimits.plugins.events.admin.OnDonationEvent;
import dev.osunolimits.plugins.events.admin.OnMultiAccountDetectionEvent;
import dev.osunolimits.plugins.events.admin.OnRankBeatmapEvent;
import dev.osunolimits.plugins.events.clans.OnUserDenyClanEvent;
import dev.osunolimits.plugins.events.clans.OnUserGetKickedClanEvent;
import dev.osunolimits.plugins.events.clans.OnUserJoinClanEvent;
import dev.osunolimits.plugins.events.clans.OnUserRequestJoinClanEvent;
import dev.osunolimits.plugins.events.clans.OnUserUnDenyClanEvent;

public class ShiinaEventListener {

    // ADMIN
    public void onAddDonorEvent(OnAddDonorEvent event) {
    }

    public void onRankBeatmapEvent(OnRankBeatmapEvent event) {
    }

    public void onMultiAccountDetectionEvent(OnMultiAccountDetectionEvent event) {
    }

    public void onAuditLogEvent(OnAuditLogEvent event) {
    }

    public void onDonationEvent(OnDonationEvent event) {
        
    }

    // ACTIONS
    public void onRegisterEvent(OnRegisterEvent event) {
    }

    public void onBeatmapFavoriteEvent(OnBeatmapFavoriteEvent event) {
    }

    public void onUserNameChangeEvent(OnUserNameChangeEvent event) {
    }

    public void onUserFlagChangeEvent(OnUserFlagChangeEvent event) {
    }

    public void onAccountDeletionEvent(OnAccountDeletionEvent event) {
    }

    public void onGfxDeletionEvent(OnGfxDeletionEvent event) {
        
    }

    // CLANS
    public void onUserJoinClanEvent(OnUserJoinClanEvent event) {
    }

    public void onUserGetKickedClanEvent(OnUserGetKickedClanEvent event) {
    }

    public void onUserDenyClanEvent(OnUserDenyClanEvent event) {
    }

    public void onUserUnDenyClanEvent(OnUserUnDenyClanEvent event) {
    }

    public void onUserRequestJoinClanEvent(OnUserRequestJoinClanEvent event) {
    }
}
