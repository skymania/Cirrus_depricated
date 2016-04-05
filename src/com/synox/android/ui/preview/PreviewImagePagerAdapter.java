/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2015  ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.synox.android.ui.preview;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import android.accounts.Account;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import com.synox.android.datamodel.FileDataStorageManager;
import com.synox.android.datamodel.OCFile;
import com.synox.android.ui.fragment.FileFragment;
import com.synox.android.utils.FileStorageUtils;

/**
 * Adapter class that provides Fragment instances
 */
//public class PreviewImagePagerAdapter extends PagerAdapter {
public class PreviewImagePagerAdapter extends FragmentStatePagerAdapter {
    
    private Vector<OCFile> mImageFiles;
    private Account mAccount;
    private Set<Object> mObsoleteFragments;
    private Set<Integer> mObsoletePositions;
    private Set<Integer> mDownloadErrors;
    private Map<Integer, FileFragment> mCachedFragments;

    /**
     * Constructor.
     * 
     * @param fragmentManager   {@link FragmentManager} instance that will handle
     *                          the {@link Fragment}s provided by the adapter.
     * @param parentFolder      Folder where images will be searched for.
     * @param storageManager    Bridge to database.
     */
    public PreviewImagePagerAdapter(FragmentManager fragmentManager, OCFile parentFolder,
                                    Account account, FileDataStorageManager storageManager /*,
                                    boolean onlyOnDevice*/) {
        super(fragmentManager);
        
        if (fragmentManager == null) {
            throw new IllegalArgumentException("NULL FragmentManager instance");
        }
        if (parentFolder == null) {
            throw new IllegalArgumentException("NULL parent folder");
        } 
        if (storageManager == null) {
            throw new IllegalArgumentException("NULL storage manager");
        }

        mAccount = account;
        FileDataStorageManager mStorageManager = storageManager;
        // TODO Enable when "On Device" is recovered ?
        mImageFiles = mStorageManager.getFolderImages(parentFolder/*, false*/);
        
        mImageFiles = FileStorageUtils.sortFolder(mImageFiles);
        
        mObsoleteFragments = new HashSet<>();
        mObsoletePositions = new HashSet<>();
        mDownloadErrors = new HashSet<>();
        mCachedFragments = new HashMap<>();
    }
    
    /**
     * Returns the image files handled by the adapter.
     * 
     * @return  A vector with the image files handled by the adapter.
     */
    protected OCFile getFileAt(int position) {
        return mImageFiles.get(position);
    }

    
    public Fragment getItem(int i) {
        OCFile file = mImageFiles.get(i);
        Fragment fragment = PreviewImageFragment.newInstance(file,
                    mObsoletePositions.contains(Integer.valueOf(i)), mAccount);
        mObsoletePositions.remove(Integer.valueOf(i));
        return fragment;
    }

    public int getFilePosition(OCFile file) {
        return mImageFiles.indexOf(file);
    }
    
    @Override
    public int getCount() {
        return mImageFiles.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mImageFiles.get(position).getFileName();
    }

    
    public void updateFile(int position, OCFile file) {
        FileFragment fragmentToUpdate = mCachedFragments.get(Integer.valueOf(position));
        if (fragmentToUpdate != null) {
            mObsoleteFragments.add(fragmentToUpdate);
        }
        mObsoletePositions.add(Integer.valueOf(position));
        mImageFiles.set(position, file);
    }
    
    
    public void updateWithDownloadError(int position) {
        FileFragment fragmentToUpdate = mCachedFragments.get(Integer.valueOf(position));
        if (fragmentToUpdate != null) {
            mObsoleteFragments.add(fragmentToUpdate);
        }
        mDownloadErrors.add(Integer.valueOf(position));
    }
    
    public void clearErrorAt(int position) {
        FileFragment fragmentToUpdate = mCachedFragments.get(Integer.valueOf(position));
        if (fragmentToUpdate != null) {
            mObsoleteFragments.add(fragmentToUpdate);
        }
        mDownloadErrors.remove(Integer.valueOf(position));
    }
    
    
    @Override
    public int getItemPosition(Object object) {
        if (mObsoleteFragments.contains(object)) {
            mObsoleteFragments.remove(object);
            return POSITION_NONE;
        }
        return super.getItemPosition(object);
    }


    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Object fragment = super.instantiateItem(container, position);
        mCachedFragments.put(Integer.valueOf(position), (FileFragment)fragment);
        return fragment;
    }
    
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
       mCachedFragments.remove(Integer.valueOf(position));
       super.destroyItem(container, position, object);
    }


    public boolean pendingErrorAt(int position) {
        return mDownloadErrors.contains(Integer.valueOf(position));
    }

    /**
     * Reset the image zoom to default value for each CachedFragments
     */
    public void resetZoom() {
        Iterator<FileFragment> entries = mCachedFragments.values().iterator();
        while (entries.hasNext()) {
            FileFragment fileFragment = entries.next();
            if (fileFragment instanceof PreviewImageFragment) {
                ((PreviewImageFragment) fileFragment).getImageView().resetZoom();
            }
        }
    }
}
