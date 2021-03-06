package com.example.myaudioplayerapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;

import com.example.myaudioplayerapp.adapters.MusicAdapter;
import com.example.myaudioplayerapp.adapters.ViewPagerAdapter;
import com.example.myaudioplayerapp.fragments.AlbumFragment;
import com.example.myaudioplayerapp.fragments.SongFragment;
import com.example.myaudioplayerapp.models.MusicFile;
import com.example.myaudioplayerapp.services.MusicPlayerService;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    private static final String TAG = "MainActivity";

    public static final int REQUEST_CODE = 1;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ViewPagerAdapter adapter;
    private final ArrayList<String> tabTitles = new ArrayList<String>(Arrays.asList("Songs","Albums"));
    public static ArrayList<MusicFile> musicFiles;
    public static ArrayList<MusicFile> albums = new ArrayList<>();

    public static MusicPlayMode currentMusicPLayMode = MusicPlayMode.REPEAT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permissionCheck();

        adapter = new ViewPagerAdapter(this);
        adapter.addFragment(SongFragment.getInstance());
        adapter.addFragment(new AlbumFragment());

        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tab.setText(tabTitles.get(position));
            }
        }).attach();
    }

    private void initViews(){
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
    }

    private void permissionCheck(){
        if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
        }else{
            musicFiles = getAllAudio(this);
            initViews();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode==REQUEST_CODE){
            if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
                musicFiles = getAllAudio(this);
                initViews();
            }else if (grantResults[0]==PackageManager.PERMISSION_DENIED){
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
            }
        }
    }

    public static ArrayList<MusicFile> getAllAudio(Context context){
        ArrayList<MusicFile> audioList = new ArrayList<>();
        Set<String> duplicate = new HashSet<>();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
        };

        Cursor cursor = context.getContentResolver().query(uri,projection,null,null,null);

        if(cursor!=null){
            while (cursor.moveToNext()){
                String path = cursor.getString(0);
                String title = cursor.getString(1);
                String artist = cursor.getString(2);
                String album = cursor.getString(3);
                String duration = cursor.getString(4);

                MusicFile musicFile = new MusicFile(path,title,artist,album,duration);
                audioList.add(musicFile);

                if(!duplicate.contains(album)){
                    albums.add(musicFile);
                    duplicate.add(album);
                }
            }
            cursor.close();
        }

        return audioList;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MediaPlayer mediaPlayer = MusicPlayerService.getInstance().getMyMediaPlayer();
        if(mediaPlayer!=null && !mediaPlayer.isPlaying()){
            Intent intent = new Intent(this,MusicPlayerService.class);
            stopService(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search,menu);

        MenuItem menuItem = menu.findItem(R.id.searchOption);
        SearchView searchView = (SearchView) menuItem.getActionView();

        searchView.setOnQueryTextListener(this);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        String userInput = s.toLowerCase();
        ArrayList<MusicFile> myFiles = new ArrayList<>();

        for(MusicFile song : musicFiles){
            if(song.getTitle().toLowerCase().contains(userInput)){
                myFiles.add(song);
            }
        }

        SongFragment.getInstance().getMusicAdapter().updateMusicFiles(myFiles);

        return true;
    }
}