package dev.osunolimits.plugins.events.admin;

import dev.osunolimits.plugins.ShiinaEventListener;
import dev.osunolimits.plugins.events.ShiinaEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class OnDonationEvent extends ShiinaEvent {
    private String transactionId;
    private Double amount;
    private String currency;
    private String customer;

    @Override
    public void call(ShiinaEventListener listener) {
        listener.onDonationEvent(this);
    }

}
