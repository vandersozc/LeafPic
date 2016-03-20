package com.leafpic.app;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.leafpic.app.Adapters.AlbumsAdapter;
import com.leafpic.app.Base.Album;
import com.leafpic.app.Base.HandlingAlbums;
import com.leafpic.app.Views.GridSpacingItemDecoration;
import com.leafpic.app.Views.ThemedActivity;
import com.leafpic.app.utils.ColorPalette;
import com.leafpic.app.utils.Measure;
import com.leafpic.app.utils.StringUtils;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.iconics.view.IconicsImageView;


public class AlbumsActivity extends ThemedActivity {

    //region PUBLIC VARIABLES
    HandlingAlbums albums = new HandlingAlbums(AlbumsActivity.this);
    RecyclerView mRecyclerView;
    AlbumsAdapter adapt;
    FloatingActionButton fabCamera;
    DrawerLayout mDrawerLayout;
    Toolbar toolbar;
    boolean editmode = false, hidden = false;
    boolean click = false;
    private SwipeRefreshLayout SwipeContainerRV;
    private View.OnLongClickListener albumOnLongCLickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            TextView a = (TextView) v.findViewById(R.id.album_name);
            adapt.notifyItemChanged(albums.toggleSelectAlbum(a.getTag().toString()));
            editmode = true;
            invalidateOptionsMenu();
            return true;
        }
    };
    private View.OnClickListener albumOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (click) {
                TextView a = (TextView) v.findViewById(R.id.album_name);
                if (editmode) {
                    adapt.notifyItemChanged(albums.toggleSelectAlbum(a.getTag().toString()));
                    invalidateOptionsMenu();
                } else {
                    Album album = albums.getAlbum(a.getTag().toString());
                    Intent intent = new Intent(AlbumsActivity.this, PhotosActivity.class);
                    /**TODO:IMPLEMENT ANIMATION**/
                    //intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    //intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    Bundle b = new Bundle();
                    b.putParcelable("album", album);
                    intent.putExtras(b);
                    startActivity(intent);
                }
            }

        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_albums);

        if (savedInstanceState != null) {
            albums = savedInstanceState.getParcelable("albums");
            StringUtils.showToast(getApplicationContext(), "porcodio le instance");
        }
        /**** START APP ****/

        /*
            SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            boolean isFirstStart = SP.getBoolean("firstStart", true);
            if (isFirstStart) {
                SharedPreferences.Editor e = SP.edit();
                e.putBoolean("firstStart", false);
                e.apply();
                StartAppIntro();
            }
        */

        /**** SET UP UI ****/
        initUI();
        setupUI();

        /**** SWIPE REFRESH ****/
        //RefreshListener();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
    }

    private void StartAppIntro() {

        Thread AppIntroThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(AlbumsActivity.this, IntroActivity.class);
                startActivity(i);
            }
        });
        AppIntroThread.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        albums.clearSelectedAlbums();
        setupUI();
        invalidateOptionsMenu();
        //setRecentApp(getString(R.string.app_name));
        new PrepareAlbumTask().execute();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("albums", albums);
    }

    private void LoadAlbumsData() {
        albums.loadPreviewAlbums();
    }

    public void initUI() {

        /**** TOOLBAR ****/
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);

        /**** RECYCLER VIEW ****/
        int nSpan = 2;//nColumns
        mRecyclerView = (RecyclerView) findViewById(R.id.grid_albums);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, nSpan));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addItemDecoration(new GridSpacingItemDecoration(nSpan, Measure.pxToDp(3, getApplicationContext()), true));

        /**** SWIPE TO REFRESH ****/
        SwipeContainerRV = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        SwipeContainerRV.setColorSchemeResources(R.color.accent_blue);
        SwipeContainerRV.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new PrepareAlbumTask().execute();
            }
        });

        //TODO show the fucking refresh when app start
        //SwipeContainerRV.setRefreshing(true);

        /**** ALBUM UI LOAD ***/
        adapt = new AlbumsAdapter(albums.dispAlbums, getApplicationContext());

        /**** ON ALBUM LONG CLICK ***/
        //adapt.setOnLongClickListener(null);

        /**** ON ALBUMS CLICK ***/
        //adapt.setOnClickListener(null);

        mRecyclerView.setAdapter(adapt);

        /**** DRAWER ****/
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.addDrawerListener(new ActionBarDrawerToggle(this,
                mDrawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                //Put your code here
                // materialMenu.animateIconState(MaterialMenuDrawable.IconState.BURGER);
            }

            public void onDrawerOpened(View drawerView) {
                //Put your code here
                //materialMenu.animateIconState(MaterialMenuDrawable.IconState.ARROW);
            }
        });

        /**** FAB ***/
        fabCamera = (FloatingActionButton) findViewById(R.id.fab_camera);
        fabCamera.setBackgroundTintList(ColorStateList.valueOf(getAccentColor()));
        fabCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
                startActivity(i);
            }
        });

        setRecentApp(getString(R.string.app_name));
    }

    //region UI/GRAPHIC
    public void setupUI() {
        toolbar.setBackgroundColor(getPrimaryColor());
        setStatusBarColor();
        setNavBarColor();
        fabCamera.setBackgroundTintList(ColorStateList.valueOf(getAccentColor()));
        setDrawerTheme();
        mRecyclerView.setBackgroundColor(getBackgroundColor());
    }

    public void setDrawerTheme() {
        RelativeLayout DrawerHeader = (RelativeLayout) findViewById(R.id.Drawer_Header);
        DrawerHeader.setBackgroundColor(getPrimaryColor());

        LinearLayout DrawerBody = (LinearLayout) findViewById(R.id.Drawer_Body);
        DrawerBody.setBackgroundColor(getBackgroundColor());

        ScrollView DrawerScroll = (ScrollView) findViewById(R.id.Drawer_Body_Scroll);
        DrawerScroll.setBackgroundColor(getBackgroundColor());

        View DrawerDivider2 = findViewById(R.id.Drawer_Body_Divider);
        DrawerDivider2.setBackgroundColor(ContextCompat.getColor(AlbumsActivity.this, R.color.drawer_transparent_gray));

        /** drawer items **/
        TextView txtDD = (TextView) findViewById(R.id.Drawer_Default_Item);
        TextView txtDH = (TextView) findViewById(R.id.Drawer_Hidden_Item);
        TextView txtDMoments = (TextView) findViewById(R.id.Drawer_Moments_Item);
        TextView txtDS = (TextView) findViewById(R.id.Drawer_Setting_Item);
        TextView txtDDonate = (TextView) findViewById(R.id.Drawer_Donate_Item);
        TextView txtGithub = (TextView) findViewById(R.id.Drawer_github_Item);
        TextView txtWall = (TextView) findViewById(R.id.Drawer_wallpapers_Item);


        IconicsImageView imgDD = (IconicsImageView) findViewById(R.id.Drawer_Default_Icon);
        IconicsImageView imgWall = (IconicsImageView) findViewById(R.id.Drawer_wallpapers_Icon);
        IconicsImageView imgDH = (IconicsImageView) findViewById(R.id.Drawer_Hidden_Icon);
        IconicsImageView imgDMoments = (IconicsImageView) findViewById(R.id.Drawer_Moments_Icon);
        IconicsImageView imgDDonate = (IconicsImageView) findViewById(R.id.Drawer_Donate_Icon);
        IconicsImageView imgGithub = (IconicsImageView) findViewById(R.id.Drawer_github_Icon);
        IconicsImageView imgDS = (IconicsImageView) findViewById(R.id.Drawer_Setting_Icon);

        /**textViews Colors*/
        int color = getTextColor();
        txtDD.setTextColor(color);
        txtDH.setTextColor(color);
        txtDMoments.setTextColor(color);
        txtDS.setTextColor(color);
        txtDDonate.setTextColor(color);
        txtGithub.setTextColor(color);
        txtWall.setTextColor(color);

        color = isDarkTheme()
                ? ColorPalette.getLightBackgroundColor(getApplicationContext())
                : ColorPalette.getDarkBackgroundColor(getApplicationContext());

        imgDD.setColor(color);
        imgDDonate.setColor(color);
        imgDH.setColor(color);
        imgDMoments.setColor(color);
        imgGithub.setColor(color);
        imgDS.setColor(color);
        imgWall.setColor(color);

        /****DRAWER CLICK LISTENER****/
        findViewById(R.id.ll_drawer_Setting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AlbumsActivity.this, SettingActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.ll_drawer_github).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://github.com/DNLDsht/LeafPic"));
                startActivity(i);
            }
        });
    }

    //region PERMISSION
    public void checkPermissions() {

        /* TODO: ASK IN FUTURE IF YOU NEED IT
        if (ContextCompat.checkSelfPermission(AlbumsActivity.this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(AlbumsActivity.this,
                    Manifest.permission.INTERNET))
                StringUtils.showToast(AlbumsActivity.this, "eddai dammi internet");
            else
                ActivityCompat.requestPermissions(AlbumsActivity.this,
                        new String[]{Manifest.permission.INTERNET}, 1);
        }
        */
        /**** STORAGE PERMISSION ****/
        if (ContextCompat.checkSelfPermission(AlbumsActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(AlbumsActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE))
                StringUtils.showToast(AlbumsActivity.this, this.getString(R.string.storage_permision_denied));
            else {
                ActivityCompat.requestPermissions(AlbumsActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            }
        } else LoadAlbumsData();
    }
    //endregion

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 0:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    //TODO ma che porco dio bisogna fare qua?
                    break;
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    StringUtils.showToast(AlbumsActivity.this, "I GOT INTERNET");
                break;
        }
    }

    void updateSelectedStuff() {
        int c;
        try {
            if ((c = albums.getSelectedCount()) != 0) {
                toolbar.setTitle(c + "/" + albums.dispAlbums.size());
                toolbar.setNavigationIcon(new IconicsDrawable(this)
                        .icon(GoogleMaterial.Icon.gmd_check)
                        .color(Color.WHITE)
                        .sizeDp(20));
                toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        editmode = false;
                        invalidateOptionsMenu();
                        albums.clearSelectedAlbums();
                        adapt.notifyDataSetChanged();
                    }
                });
                toolbar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (albums.getSelectedCount() == albums.dispAlbums.size())
                            albums.clearSelectedAlbums();
                        else albums.selectAllAlbums();
                        adapt.notifyDataSetChanged();
                        invalidateOptionsMenu();
                    }
                });
            } else {
                toolbar.setTitle(getString(R.string.app_name));
                toolbar.setNavigationIcon(new IconicsDrawable(this)
                        .icon(GoogleMaterial.Icon.gmd_menu)
                        .color(Color.WHITE)
                        .sizeDp(20));
                toolbar.setOnClickListener(null);
                toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mDrawerLayout.openDrawer(GravityCompat.START);
                    }
                });
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

    }
    //endregion

    //region MENU
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_albums, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        MenuItem opt;
        if (editmode) {
            setOptionsAlbmuMenusItemsVisible(menu, true);
            opt = menu.findItem(R.id.sort_action);
            opt.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            opt = menu.findItem(R.id.deleteAction);
            opt.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        } else {
            setOptionsAlbmuMenusItemsVisible(menu, false);
            opt = menu.findItem(R.id.sort_action);
            opt.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            opt = menu.findItem(R.id.deleteAction);
            opt.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }

        if (hidden) {
            opt = menu.findItem(R.id.refreshhiddenAlbumsButton);
            opt.setEnabled(true).setVisible(true);
            opt = menu.findItem(R.id.hideAlbumButton);
            opt.setTitle(getString(R.string.unhide));
        } else {
            opt = menu.findItem(R.id.refreshhiddenAlbumsButton);
            opt.setEnabled(false).setVisible(false);
            opt = menu.findItem(R.id.hideAlbumButton);
            opt.setTitle(getString(R.string.hide));
        }
        if (albums.getSelectedCount() == 0) {
            editmode = false;
            invalidateOptionsMenu();
        }
        updateSelectedStuff();
        return super.onPrepareOptionsMenu(menu);
    }

    private void setOptionsAlbmuMenusItemsVisible(final Menu menu, boolean val) {
        MenuItem opt = menu.findItem(R.id.hideAlbumButton);
        opt.setEnabled(val).setVisible(val);
        opt = menu.findItem(R.id.deleteAction);
        opt.setEnabled(val).setVisible(val);
        opt = menu.findItem(R.id.excludeAlbumButton);
        opt.setEnabled(val).setVisible(val);

        opt = menu.findItem(R.id.select_all_albums_action);
        opt.setEnabled(val).setVisible(val);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.filterPhotos:
                final PopupMenu popupfilter = new PopupMenu(AlbumsActivity.this, findViewById(R.id.filterPhotos));
                popupfilter.setGravity(Gravity.AXIS_PULL_BEFORE);
                popupfilter.getMenuInflater().inflate(R.menu.filter, popupfilter.getMenu());
                break;

            case R.id.sort_action:
                if (albums.getSelectedCount()==0) {//TODO: MUST BE FUKING FIXED
                    View sort_btn = findViewById(R.id.sort_action);
                    PopupMenu popup = new PopupMenu(AlbumsActivity.this, sort_btn);
                    popup.setGravity(Gravity.AXIS_CLIP);
                    popup.getMenuInflater()
                            .inflate(R.menu.sort, popup.getMenu());
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
                            Toast.makeText(
                                    AlbumsActivity.this,
                                    "You Clicked: " + item.getTitle(),
                                    Toast.LENGTH_SHORT
                            ).show();
                            return true;
                        }
                    });
                    popup.show(); //TODO: CRASH HERE WHEN YOU OPEN SORT WITH SELECTED ITEM PORCO DIOOOOOO
                }
                break;

            case R.id.refreshhiddenAlbumsButton:
                albums.loadPreviewHiddenAlbums();
                adapt.notifyDataSetChanged();
                break;

            case R.id.select_all_albums_action:
                albums.selectAllAlbums();
                adapt.notifyDataSetChanged();
                invalidateOptionsMenu();
                break;

            case R.id.excludeAlbumButton:
                AlertDialog.Builder builder = new AlertDialog.Builder(AlbumsActivity.this);
                builder.setMessage(R.string.exclude_album_message)
                        .setPositiveButton( this.getString(R.string.exclude_action), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                albums.excludeSelectedAlbums();
                                adapt.notifyDataSetChanged();
                                invalidateOptionsMenu();
                            }
                        })
                        .setNegativeButton(this.getString(R.string.cancel_action), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                builder.show();
                break;

            case R.id.deleteAction:
                AlertDialog.Builder builder1 = new AlertDialog.Builder(AlbumsActivity.this);
                builder1.setMessage(R.string.delete_album_message)
                        .setPositiveButton(this.getString(R.string.delete_action), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                albums.deleteSelectedAlbums();
                                adapt.notifyDataSetChanged();
                                invalidateOptionsMenu();
                            }
                        })
                        .setNegativeButton( this.getString(R.string.cancel_action), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        });
                builder1.show();
                break;

            case R.id.hideAlbumButton:
                if (hidden) {
                    albums.unHideSelectedAlbums();
                    adapt.notifyDataSetChanged();
                    invalidateOptionsMenu();
                } else {

                    AlertDialog.Builder builder2 = new AlertDialog.Builder(AlbumsActivity.this);
                    builder2.setMessage(R.string.delete_album_message)
                            .setPositiveButton( this.getString(R.string.hide_action), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    albums.hideSelectedAlbums();
                                    adapt.notifyDataSetChanged();
                                    invalidateOptionsMenu();
                                }
                            })
                            .setNeutralButton( this.getString(R.string.exclude_action), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    albums.excludeSelectedAlbums();
                                    adapt.notifyDataSetChanged();
                                    invalidateOptionsMenu();
                                }
                            })
                            .setNegativeButton( this.getString(R.string.cancel_action), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            });
                    builder2.show();
                }
                break;
            case R.id.settingsTry_albums_action:
                Intent asd = new Intent(AlbumsActivity.this, SettingActivity.class);
                startActivity(asd);
                break;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START))
            mDrawerLayout.closeDrawer(GravityCompat.START);
        else finish();
    }

    //endregion

    public class PrepareAlbumTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            click = false;
            SwipeContainerRV.setRefreshing(true);
            adapt.setOnLongClickListener(null);
            adapt.setOnClickListener(null);
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            checkPermissions();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

            adapt.updateDataset(albums.dispAlbums);
            adapt.setOnClickListener(albumOnClickListener);
            adapt.setOnLongClickListener(albumOnLongCLickListener);
            click = true;
            SwipeContainerRV.setRefreshing(false);
        }
    }
}
