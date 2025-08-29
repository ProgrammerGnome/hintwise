package com.example.hintwise.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "saved_texts")
public class SavedText {

    @PrimaryKey(autoGenerate = true)
    public int localId; // Room saját ID-je

    public String id; // JSON-beli id (pl. "C.473")
    public String problem;

    public SavedText(String id, String problem) {
        this.id = id;
        this.problem = problem;
    }
}
