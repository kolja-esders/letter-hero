package edu.pietro.team.letterhero.vision;


import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.Line;
import com.google.android.gms.vision.text.TextBlock;

import java.util.List;

public class OcrDetectionProcessor implements Detector.Processor<TextBlock> {

    public static String lastOcr = "";

    private final static String TAG = "OcrDetectionProcessor";

    @Override
    public void receiveDetections(Detector.Detections<TextBlock> detections) {
        StringBuilder sb = new StringBuilder();
        SparseArray<TextBlock> items = detections.getDetectedItems();
        for (int i = 0; i < items.size(); ++i) {
            TextBlock item = items.valueAt(i);
            List components = item.getComponents();
            for (int j = 0; j < components.size(); ++j) {
                if (components.get(j) instanceof Line) {
                    Line l = (Line) components.get(j);
                    sb.append(l.getValue());
                    sb.append(" ");
                    //Log.d(TAG, l.getValue());
                }
            }
        }
        String ocrText = sb.toString();
        lastOcr = ocrText;
        Log.d(TAG, ocrText);
    }

    @Override
    public void release() {
    }

}
