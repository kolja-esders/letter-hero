package edu.pietro.team.letterhero.event;


public class FeedFilterClicked {

    public final boolean showOnlyPersonalStories;

    public FeedFilterClicked(boolean showOnlyPersonalStories) {
        this.showOnlyPersonalStories = showOnlyPersonalStories;
    }

}