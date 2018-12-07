package com.rafaelbermudez.encuestas.Utilities;

public class Utilities {

    //Constants fields Poll table
    public static final String POLL_TABLE = "poll";
    public static final String ID_FIELD = "id";
    public static final String UPLOADED_FIELD = "uploaded";
    public static final String FIRSTNAME_FIELD = "firstname";
    public static final String LASTNAME_FIELD = "lastname";
    public static final String AGE_FIELD = "age";
    public static final String ANSWER1_FIELD = "answer1";
    public static final String ANSWER2_FIELD = "answer2";
    public static final String ANSWER3_FIELD = "answer3";

    public static final String CREATE_POLL_TABLE = "CREATE TABLE " + POLL_TABLE + " ("+ ID_FIELD +
            " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "+ UPLOADED_FIELD + " INTEGER, " + FIRSTNAME_FIELD + " TEXT, "+ LASTNAME_FIELD +" TEXT, " + AGE_FIELD +
            " INTEGER, "+ ANSWER1_FIELD +" TEXT, " + ANSWER2_FIELD +" TEXT, " + ANSWER3_FIELD + " TEXT)";
}
