package edu.pietro.team.letterhero;


import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import edu.pietro.team.letterhero.event.OnErrorDuringDetectionPostProcessing;
import edu.pietro.team.letterhero.event.OnStartDetectionPostProcessing;

import edu.pietro.team.letterhero.event.OnImageCaptureRequested;
import edu.pietro.team.letterhero.event.OnStopMessage;

public class ScanOverlayFragment extends Fragment {

    private EventBus mDefaultEventBus;

    private OnScanOverlayFragmentInteractionListener mListener;

    private static final int TIMEOUT_PROCESSING_MILLIS = 10000;

    private static final int TIME_ERROR_MILLIS = 3000;

    private boolean mIsProcessing = false;

    private Handler mHandler = new Handler();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_scan_overlay, container, false);
        mDefaultEventBus = EventBus.getDefault();

        ImageView takeSnap = (ImageView) v.findViewById(R.id.snap);
        takeSnap.setOnClickListener(new ImageView.OnClickListener() {
            public void onClick(View v) {
                mDefaultEventBus.post(new OnImageCaptureRequested());
            }
        });

        return v;
    }

    public static ScanOverlayFragment newInstance() {
        Bundle args = new Bundle();
        ScanOverlayFragment fragment = new ScanOverlayFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnScanOverlayFragmentInteractionListener) {
            mListener = (OnScanOverlayFragmentInteractionListener) activity;
        }
        EventBus.getDefault().register(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnScanOverlayFragmentInteractionListener) {
            mListener = (OnScanOverlayFragmentInteractionListener) context;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(OnStartDetectionPostProcessing event) {
        if (!mIsProcessing) {
            mIsProcessing = true;
            if (event.message != null) {
                startProcessingStatus(event.message, TIMEOUT_PROCESSING_MILLIS);
            } else {
                startProcessingStatus(TIMEOUT_PROCESSING_MILLIS);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(OnErrorDuringDetectionPostProcessing event) {
        mIsProcessing = false;
        startErrorStatus(event.message, TIME_ERROR_MILLIS);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(OnStopMessage event) {
        mIsProcessing = false;
        stopStatus();
    }

    // OnSwitchToPayment ...
    // mIsProcessing = false

    @Override
    public void onDetach() {
        super.onDetach();
        EventBus.getDefault().unregister(this);
        mListener = null;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (mListener != null) {
            mListener.onScannerVisibilityChange(isVisibleToUser);
        }
    }

    public interface OnScanOverlayFragmentInteractionListener {
        /**
         * Gets called when there is a visibility change for this fragment.
         */
        void onScannerVisibilityChange(boolean isVisible);
    }

    private void startProcessingStatus(String status, int timeoutMillis) {
        final TextView processingStatus = (TextView) getActivity().findViewById(R.id.processing_status);
        processingStatus.setText(status);
        processingStatus.setTextColor(getResources().getColor(android.R.color.black));
        processingStatus.setBackgroundResource(R.drawable.status_background_processing);
        processingStatus.setVisibility(View.VISIBLE);
        startProcessingStatus(timeoutMillis);
    }

    private void startProcessingStatus(int timeoutMillis) {
        final ImageView imageView = (ImageView) getActivity().findViewById(R.id.logo);
        imageView.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.pulse));

        // After 10 seconds we declare failure...
        mHandler.removeMessages(0);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mIsProcessing) {
                    stopStatus();
                    mIsProcessing = false;
                }
            }
        }, timeoutMillis);
    }

    private void startErrorStatus(String status, int timeoutMillis) {
        final ImageView imageView = (ImageView) getActivity().findViewById(R.id.logo);
        final TextView processingStatus = (TextView) getActivity().findViewById(R.id.processing_status);

        processingStatus.setText(status);
        processingStatus.setTextColor(getResources().getColor(android.R.color.white));
        processingStatus.setBackgroundResource(R.drawable.status_background_error);
        processingStatus.setVisibility(View.VISIBLE);
        imageView.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.shake));

        // After 10 seconds we declare failure...
        mHandler.removeMessages(0);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopStatus();
            }
        }, timeoutMillis);
    }

    private void stopStatus() {
        final ImageView imageView = (ImageView) getActivity().findViewById(R.id.logo);
        final TextView processingStatus = (TextView) getActivity().findViewById(R.id.processing_status);

        imageView.clearAnimation();
        processingStatus.setVisibility(View.INVISIBLE);
    }
}