package edu.pietro.team.letterhero.event;


import edu.pietro.team.letterhero.MainActivity;
import edu.pietro.team.letterhero.helper.ProcessingState;
import edu.pietro.team.letterhero.social.MoneyTransfer;

public class OnPaymentInit {

    private final MoneyTransfer mPurchase;
    private final ProcessingState mAssumedProcessingState;

    public OnPaymentInit(MoneyTransfer purchase, ProcessingState assumedPS) {
        mPurchase = purchase;
        mAssumedProcessingState = assumedPS;

        MainActivity.getCurrentActivity().setCurrentTransfer(mPurchase);

    }

    public MoneyTransfer getPurchase() {
        return mPurchase;
    }
    public ProcessingState getAssumedProcessingState() { return mAssumedProcessingState; }

}
