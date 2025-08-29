package com.example.hintwise.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SavedTextDao {

    @Insert
    void insert(SavedText text);

    @Insert
    void insertAll(List<SavedText> texts);

    @Query("SELECT * FROM saved_texts ORDER BY RANDOM() LIMIT 1")
    SavedText getRandomText();

    @Query("SELECT COUNT(*) FROM saved_texts")
    int count();
}
