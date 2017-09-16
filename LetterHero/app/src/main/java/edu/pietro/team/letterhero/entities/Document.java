package edu.pietro.team.letterhero.entities;

import android.media.Image;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Document {

    private Image mImage;

    private String mType;

    private String mSender;

    private String mDateOfLetter;

    private Map<String, String> mContext;

    public Image getImage() {
        return mImage;
    }

    public void setImage(Image image) {
        this.mImage = image;
    }

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        this.mType = type;
    }

    public String getSender() {
        return mSender;
    }

    public void setSender(String sender) {
        this.mSender = sender;
    }

    public String getDateOfLetter() {
        return mDateOfLetter;
    }

    public void setDateOfLetter(String dateOfLetter) {
        this.mDateOfLetter = dateOfLetter;
    }

    public Map<String, String> getContext() {
        return mContext;
    }

    public void setContext(Map<String, String> context) {
        this.mContext = context;
    }

    public static Document fromJSON(JSONObject json) {
        Document document = new Document();
        try {
            document.setType(json.getString("type"));
            document.setSender(json.getString("sender"));
            document.setDateOfLetter(json.getString("dateOfLetter"));
            JSONObject jsonContext = json.getJSONObject("context");
            Iterator<?> contextKeys = jsonContext.keys();
            Map<String, String> context = new HashMap<>();

            while (contextKeys.hasNext()) {
                String key = (String) contextKeys.next();
                Object value = jsonContext.get(key);
                if (value instanceof String) {
                    context.put(key, (String) value);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return document;
    }


}
