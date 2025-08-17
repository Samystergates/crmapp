package com.web.appts.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class MonteurData {

    private static final String mBerkom = "Frank van Berkom";
    private static final String mRinie = "Rinie Eling";
    private static final String mJos = "Jos Geurden";
    private static final String mCoppel = "Frank Coppelmans";
    private static final String mEd = "Ed Barten";
    private static final String mPeter = "Peter Rovers";
    private static final String mRick = "Rick Kocken";
    private static final String mPerry = "Perry van Vught";
    private static final String mThiel = "Frank van Thiel";

    public static String getmBerkom() {
        return mBerkom;
    }

    public static String getmRinie() {
        return mRinie;
    }

    public static String getmJos() {
        return mJos;
    }

    public static String getmCoppel() {
        return mCoppel;
    }

    public static String getmEd() {
        return mEd;
    }

    public static String getmPeter() {
        return mPeter;
    }

    public static String getmRick() {
        return mRick;
    }

    public static String getmPerry() {
        return mPerry;
    }

    public static String getmThiel() {
        return mThiel;
    }

    public static List<String> getAllMonteurs() {
        List<String> monteurs = new ArrayList<>();
        monteurs.add(getmBerkom());
        monteurs.add(getmRinie());
        monteurs.add(getmJos());
        monteurs.add(getmCoppel());
        monteurs.add(getmEd());
        monteurs.add(getmPeter());
        monteurs.add(getmRick());
        monteurs.add(getmPerry());
        monteurs.add(getmThiel());

        return monteurs;

    }
}
