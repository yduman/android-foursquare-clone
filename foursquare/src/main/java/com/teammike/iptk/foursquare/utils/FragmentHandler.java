package com.teammike.iptk.foursquare.utils;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;

/**
 * Handler class for fragment transactions
 * @author Yadullah Duman
 */
public class FragmentHandler {

    public static void addFragment(int id, Fragment fragment, FragmentManager fragmentManager) {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(id, fragment).commit();
    }

    public static void replaceFragment(int id, Fragment fragment, FragmentManager fragmentManager) {
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(id, fragment).addToBackStack("tag").commit();
    }

}
