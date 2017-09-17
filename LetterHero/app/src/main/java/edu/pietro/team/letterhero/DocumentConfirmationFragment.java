package edu.pietro.team.letterhero;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * Use the {@link DocumentConfirmationFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DocumentConfirmationFragment extends Fragment {
    private Handler mHandler = new Handler();

    public DocumentConfirmationFragment() {
        // Required empty public constructor
    }


    public static DocumentConfirmationFragment newInstance() {
        Bundle args = new Bundle();
        DocumentConfirmationFragment fragment = new DocumentConfirmationFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_document_detected, container, false);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                MainActivity.getCurrentActivity().resetConfirmationView();
            }
        }, 5000);

        final Button okb = (Button) v.findViewById(R.id.ok_button);
        okb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.getCurrentActivity().resetConfirmationView();
            }
        });

        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
