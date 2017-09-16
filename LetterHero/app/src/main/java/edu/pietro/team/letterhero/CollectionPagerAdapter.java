package edu.pietro.team.letterhero;


import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

public class CollectionPagerAdapter extends FragmentPagerAdapter {

    ScanOverlayFragment scf = ScanOverlayFragment.newInstance();
    DocumentConfirmationFragment pif = DocumentConfirmationFragment.newInstance();

    public CollectionPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int i) {
        if (i == 0) {
            return scf;
        } else if (i == 1) {
            return pif;
        }
        return null;
    }

    @Override
    public int getCount() {
        return 2;
    }

}
