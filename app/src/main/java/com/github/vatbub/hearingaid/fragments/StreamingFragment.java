package com.github.vatbub.hearingaid.fragments;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.vatbub.hearingaid.R;
import com.ohoussein.playpause.PlayPauseView;

import static android.content.pm.PackageManager.PERMISSION_DENIED;

public class StreamingFragment extends Fragment {
    private static final String SUPERPOWERED_INITIALIZED_BUNDLE_KEY = "superpoweredInitialized";
    private static final String IS_STREAMING_BUNDLE_KEY = "isStreaming";

    static {
        System.loadLibrary("HearingAidAudioProcessor");
    }

    private boolean isStreaming;
    private View createdView;
    private boolean superpoweredInitialized = false;
    private BottomSheetBehavior mBottomSheetBehavior;

    public StreamingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (permissions.length == 0 || grantResults.length == 0 || grantResults[0] == PERMISSION_DENIED) {
            setStreaming(false);
            ((PlayPauseView) findViewById(R.id.mainToggleButton)).change(!isStreamingEnabled());
            return;
        }

        updateStreamingState();
    }

    public boolean isStreamingEnabled() {
        return isStreaming;
    }

    private void updateStreamingState() {
        initSuperpoweredIfNotInitialized();
        if (isStreamingEnabled()) {
            Snackbar.make(findViewById(R.id.fragment_content), R.string.fragment_streaming_snackbar_start_streaming, 3000).show();
        } else {
            Snackbar.make(findViewById(R.id.fragment_content), R.string.fragment_streaming_snackbar_stop_streaming, 3000).show();
        }
        onPlayPause(isStreamingEnabled());
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean allPermissionsGranted() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            return false;

        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            superpoweredInitialized = savedInstanceState.getBoolean(SUPERPOWERED_INITIALIZED_BUNDLE_KEY);
            setStreaming(savedInstanceState.getBoolean(IS_STREAMING_BUNDLE_KEY));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SUPERPOWERED_INITIALIZED_BUNDLE_KEY, superpoweredInitialized);
        outState.putBoolean(IS_STREAMING_BUNDLE_KEY, isStreamingEnabled());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_streaming, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        createdView = view;

        findViewById(R.id.mainToggleButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                setStreaming(!isStreamingEnabled());
                ((PlayPauseView) v).change(!isStreamingEnabled());
                if (!allPermissionsGranted()) {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                } else {
                    updateStreamingState();
                }
            }
        });

        View bottomSheet = findViewById(R.id.bottom_sheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private <T extends View> T findViewById(@IdRes int id) {
        return createdView.findViewById(id);
    }

    /**
     * Initializes the superpowered sdk and associated c++ code.
     * No-op if already initialized.
     */
    private void initSuperpoweredIfNotInitialized() {
        if (superpoweredInitialized)
            return;

        // Get the device's sample rate and buffer size to enable low-latency Android audio io, if available.
        String samplerateString = null, buffersizeString = null;
        AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        assert audioManager != null;
        samplerateString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
        buffersizeString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        if (samplerateString == null) samplerateString = "44100";
        if (buffersizeString == null) buffersizeString = "512";

        HearingAidAudioProcessor(Integer.parseInt(samplerateString), Integer.parseInt(buffersizeString));

        superpoweredInitialized = true;
    }

    private native void HearingAidAudioProcessor(int samplerate, int buffersize);

    private native void onPlayPause(boolean play);

    public void setStreaming(boolean streaming) {
        isStreaming = streaming;
    }
}
