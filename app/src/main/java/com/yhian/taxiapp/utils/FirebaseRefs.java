package com.yhian.taxiapp.utils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseRefs {

    private static final String DATABASE_URL = "https://taxiapp-a56b0-default-rtdb.firebaseio.com/";

    private FirebaseRefs() {
    }

    public static FirebaseDatabase database() {
        return FirebaseDatabase.getInstance(DATABASE_URL);
    }

    public static DatabaseReference root() {
        return database().getReference();
    }
}
