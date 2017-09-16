package edu.pietro.team.letterhero.entities;

import android.graphics.Bitmap;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Document {

    private Bitmap mBitmap;

    private String mType;

    private String mCategory;

    private String mSender;

    private String mDateOfLetter;

    private Map<String, String> mContext;

    private Map<String, String> mNumbers;

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.mBitmap = bitmap;
    }

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        this.mType = type;
    }

    public String getCategory() {
        return mCategory;
    }

    public void setCategory(String category) {
        this.mCategory = category;
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

    public Map<String, String> getNumbers() {
        return mNumbers;
    }

    public void setNumbers(Map<String, String> numbers) {
        this.mNumbers = numbers;
    }

    public static Document fromJSON(JSONObject json) {
        Document document = new Document();
        try {
            document.setType(json.getString("type"));
            document.setCategory(json.getString("category"));
            document.setSender(json.getString("company"));
            JSONObject jsonContext = json.getJSONObject("context");
            JSONObject jsonNumbers = json.getJSONObject("numbers");
            Iterator<?> contextKeys = jsonContext.keys();
            Iterator<?> numbersKeys = jsonNumbers.keys();
            Map<String, String> context = new HashMap<>();
            Map<String, String> numbers = new HashMap<>();
            while (contextKeys.hasNext()) {
                String key = (String) contextKeys.next();
                Object value = jsonContext.get(key);
                if (value instanceof String) {
                    context.put(key, (String) value);
                }
            }
            while (numbersKeys.hasNext()) {
                String key = (String) numbersKeys.next();
                Object value = jsonNumbers.get(key);
                if (value instanceof String) {
                    numbers.put(key, (String) value);
                }
            }
            document.setContext(context);
            document.setNumbers(numbers);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return document;
    }


}
