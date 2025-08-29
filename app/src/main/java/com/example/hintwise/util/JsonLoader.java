package com.example.hintwise.util;

import android.content.Context;

import com.example.hintwise.data.SavedText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;

public class JsonLoader {

    public static List<SavedText> loadProblems(Context context) {
        try {
            InputStream is = context.getAssets().open("test_problems.json");
            InputStreamReader reader = new InputStreamReader(is);

            Type listType = new TypeToken<List<SavedText>>(){}.getType();
            return new Gson().fromJson(reader, listType);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
