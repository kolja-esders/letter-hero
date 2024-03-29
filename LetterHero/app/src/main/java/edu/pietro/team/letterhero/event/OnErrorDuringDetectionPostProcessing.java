package edu.pietro.team.letterhero.event;

/**
 * Fired when there was an error during the post-detection processing.
 */
public class OnErrorDuringDetectionPostProcessing {

    public String message;

    public OnErrorDuringDetectionPostProcessing() {
    }

    public OnErrorDuringDetectionPostProcessing(String message) {
        this.message = message;
    }
}
