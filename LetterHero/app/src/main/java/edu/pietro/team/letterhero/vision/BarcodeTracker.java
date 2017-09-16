package edu.pietro.team.letterhero.vision;

import android.util.Log;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;

import org.greenrobot.eventbus.EventBus;

import edu.pietro.team.letterhero.event.OnErrorDuringDetectionPostProcessing;
import edu.pietro.team.letterhero.event.OnPaymentInit;
import edu.pietro.team.letterhero.helper.ProcessingState;
import edu.pietro.team.letterhero.helper.api.AmazonProductAdvertisingAPI;
import edu.pietro.team.letterhero.social.Item;
import edu.pietro.team.letterhero.social.MoneyTransfer;
import edu.pietro.team.letterhero.social.User;


public class BarcodeTracker extends Tracker<Barcode> {
    private final static String TAG = "BarcodeTracker";

    public void onNewItem(int id, Barcode face) {
        Log.i(TAG, "Barcode entered.");
        // Won't display long enough. Don't show for now.
        //EventBus.getDefault().post(new OnStartDetectionPostProcessing("Processing barcode..."));

        String ean13 = face.displayValue;
        Item item = AmazonProductAdvertisingAPI.findByEAN13(ean13);
        if (item != null) {
            User seller = User.AMAZON;
            EventBus.getDefault().post(new OnPaymentInit(
                    new MoneyTransfer(seller, item, item.getRetailPrice()),
                    ProcessingState.NOLOCK
            ));
        } else {
            EventBus.getDefault().post(new OnErrorDuringDetectionPostProcessing("No product found"));
        }
    }

    public void onUpdate(Detector.Detections<Barcode> detections, Barcode face) {
    }

    public void onDone() {
        Log.i(TAG, "Barcode left.");
    }
}
