package org.helllabs.android.xmp.browser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.helllabs.android.xmp.R;
import org.helllabs.android.xmp.browser.playlist.Playlist;
import org.helllabs.android.xmp.browser.playlist.PlaylistAdapter;
import org.helllabs.android.xmp.browser.playlist.PlaylistItem;
import org.helllabs.android.xmp.browser.playlist.PlaylistUtils;
import org.helllabs.android.xmp.modarchive.Search;
import org.helllabs.android.xmp.player.PlayerActivity;
import org.helllabs.android.xmp.preferences.Preferences;
import org.helllabs.android.xmp.service.PlayerService;
import org.helllabs.android.xmp.util.ChangeLog;
import org.helllabs.android.xmp.util.FileUtils;
import org.helllabs.android.xmp.util.Log;
import org.helllabs.android.xmp.util.Message;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;


public class PlaylistMenu extends ActionBarActivity implements PlaylistAdapter.OnItemClickListener {
	private static final String TAG = "PlaylistMenu";
	private static final int SETTINGS_REQUEST = 45;
	private static final int PLAYLIST_REQUEST = 46;
	private SharedPreferences prefs;
	private String mediaPath;
	private int deletePosition;
	private PlaylistAdapter playlistAdapter;

	@Override
	public void onCreate(final Bundle icicle) {		
		super.onCreate(icicle);
		setContentView(R.layout.playlist_menu);

		final RecyclerView recyclerView = (RecyclerView)findViewById(R.id.plist_menu_list);

        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
		
		playlistAdapter = new PlaylistAdapter(PlaylistMenu.this, new ArrayList<PlaylistItem>(), false, PlaylistAdapter.LAYOUT_CARD);
        playlistAdapter.setOnItemClickListener(this);
   		recyclerView.setAdapter(playlistAdapter);

		registerForContextMenu(recyclerView);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		if (!checkStorage()) {
			Message.fatalError(this, getString(R.string.error_storage));
		}

		if (!Preferences.DATA_DIR.isDirectory()) {
			if (Preferences.DATA_DIR.mkdirs()) {
				PlaylistUtils.createEmptyPlaylist(this, getString(R.string.empty_playlist), getString(R.string.empty_comment));
			} else {
				Message.fatalError(this, getString(R.string.error_datadir));
			}
		}

		final ChangeLog changeLog = new ChangeLog(this);
		changeLog.show();
		
		if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0) {
			startPlayerActivity();
		}
		
		enableHomeButton();
		
		updateList();
	}
	
	@TargetApi(14)
	private void enableHomeButton() {
		if (Build.VERSION.SDK_INT >= 14) {
			getSupportActionBar().setHomeButtonEnabled(true);
		}
	}
	
	@Override
	public void onNewIntent(final Intent intent) {
		
		// If we launch from launcher and we're playing a module, go straight to the player activity
		
		if ((intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0) {
			startPlayerActivity();
		}
	}

	@Override
	public void onItemClick(final PlaylistAdapter adapter, final View view, final int position) {
		if (position == 0) {
			final Intent intent = new Intent(PlaylistMenu.this, FilelistActivity.class);
			startActivityForResult(intent, PLAYLIST_REQUEST);
		} else {
			final Intent intent = new Intent(PlaylistMenu.this, PlaylistActivity.class);
			intent.putExtra("name", adapter.getItem(position).getName());
			startActivityForResult(intent, PLAYLIST_REQUEST);
		}
	}
	
	private void startPlayerActivity() {
		if (prefs.getBoolean(Preferences.START_ON_PLAYER, true)) {
			if (PlayerService.isAlive) {
				final Intent playerIntent = new Intent(this, PlayerActivity.class);
				startActivity(playerIntent);
			}
		}
	}
	
	private static boolean checkStorage() {
		final String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			return true;
		} else {
			Log.e(TAG, "External storage state error: " + state);
			return false;
		}
	}

	private void updateList() {
		mediaPath = prefs.getString(Preferences.MEDIA_PATH, Preferences.DEFAULT_MEDIA_PATH);

		playlistAdapter.clear();
		final PlaylistItem browserItem = new PlaylistItem(PlaylistItem.TYPE_SPECIAL, "File browser", "Files in " + mediaPath);
		browserItem.setImageRes(R.drawable.browser);
		playlistAdapter.add(browserItem);

		for (final String name : PlaylistUtils.listNoSuffix()) {
			final PlaylistItem item = new PlaylistItem(PlaylistItem.TYPE_PLAYLIST, name, Playlist.readComment(this, name));	// NOPMD
			item.setImageRes(R.drawable.list);
			playlistAdapter.add(item);
		}

		playlistAdapter.notifyDataSetChanged();
	}


	// Playlist context menu

	@Override
	public void onCreateContextMenu(final ContextMenu menu, final View view, final ContextMenu.ContextMenuInfo menuInfo) {
		//final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
		menu.setHeaderTitle("Playlist options");

        final int position = playlistAdapter.getPosition();

		if (position == 0) {					// Module list
			menu.add(Menu.NONE, 0, 0, "Change directory");
			//menu.add(Menu.NONE, 1, 1, "Add to playlist");
		} else {									// Playlists
			menu.add(Menu.NONE, 0, 0, "Rename");
			menu.add(Menu.NONE, 1, 1, "Edit comment");
			menu.add(Menu.NONE, 2, 2, "Delete playlist");
		}
	}

	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		//final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();

        final int index = item.getItemId();
        final int position = playlistAdapter.getPosition();

		if (position == 0) {		// First item of list
			if (index == 0) {			// First item of context menu
				changeDir(this);
				return true;
			}
		} else {
			switch (index) {
			case 0:						// Rename
				renameList(this, position -1);
				updateList();
				return true;
			case 1:						// Edit comment
				editComment(this, position -1);
				updateList();
				return true;
			case 2:						// Delete
				deletePosition = position - 1;
				Message.yesNoDialog(this, "Delete", "Are you sure to delete playlist " +
						PlaylistUtils.listNoSuffix()[deletePosition] + "?", new Runnable() {
					@Override
					public void run() {
						Playlist.delete(PlaylistMenu.this, PlaylistUtils.listNoSuffix()[deletePosition]);
						updateList();
					}
				});

				return true;
			default:
				break;
			}			
		}

		return true;
	}

	private void renameList(final Activity activity, final int index) {
		final String name = PlaylistUtils.listNoSuffix()[index];
		final InputDialog alert = new InputDialog(activity);		  
		alert.setTitle("Rename playlist");
		alert.setMessage("Enter the new playlist name:");
		alert.input.setText(name);		

		alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {  
			public void onClick(final DialogInterface dialog, final int whichButton) {
				final String value = alert.input.getText().toString();
				
				if (!Playlist.rename(activity, name, value)) {
					Message.error(activity, getString(R.string.error_rename_playlist));
				}

				updateList();
			}  
		});  

		alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {  
			public void onClick(final DialogInterface dialog, final int whichButton) {  
				// Canceled.  
			}  
		});  

		alert.show(); 
	}

	private void changeDir(final Context context) {
		final InputDialog alert = new InputDialog(context);		  
		alert.setTitle("Change directory");  
		alert.setMessage("Enter the mod directory:");
		alert.input.setText(mediaPath);

		alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {  
			public void onClick(final DialogInterface dialog, final int whichButton) {  
				final String value = alert.input.getText().toString();
				if (!value.equals(mediaPath)) {
					final SharedPreferences.Editor editor = prefs.edit();
					editor.putString(Preferences.MEDIA_PATH, value);
					editor.commit();
					updateList();
				}
			}  
		});  

		alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {  
			public void onClick(final DialogInterface dialog, final int whichButton) {  
				// Canceled.  
			}  
		});  

		alert.show(); 
	}

	private void editComment(final Activity activity, final int index) {
		final String name = PlaylistUtils.listNoSuffix()[index];
		final InputDialog alert = new InputDialog(activity);		  
		alert.setTitle("Edit comment");
		alert.setMessage("Enter the new comment for " + name + ":");  
		alert.input.setText(Playlist.readComment(activity, name));

		alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {  
			public void onClick(final DialogInterface dialog, final int whichButton) {  
				final String value = alert.input.getText().toString().replace("\n", " ");				
				final File file = new File(Preferences.DATA_DIR, name + Playlist.COMMENT_SUFFIX);
				try {
					file.delete();
					file.createNewFile();
					FileUtils.writeToFile(file, value);
				} catch (IOException e) {
					Message.error(activity, getString(R.string.error_edit_comment));
				}

				updateList();
			}  
		});  

		alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {  
			public void onClick(final DialogInterface dialog, final int whichButton) {
				// Canceled.  
			}  
		});  

		alert.show(); 
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		switch (requestCode) {
		case SETTINGS_REQUEST:
			if (resultCode == RESULT_OK) {
				updateList();
			}
			break;
		case PLAYLIST_REQUEST:
			updateList();
			break;
		default:
			break;
		}
	}


	// Menu

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);

		// Calling super after populating the menu is necessary here to ensure that the
		// action bar helpers have a chance to handle this event.
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch(item.getItemId()) {
		case android.R.id.home:
			startPlayerActivity();
			break;
		case R.id.menu_new_playlist:
			PlaylistUtils.newPlaylistDialog(this, new Runnable() {
				public void run() {
					updateList();
				}
			});
			break;
		case R.id.menu_prefs:		
			startActivityForResult(new Intent(this, Preferences.class), SETTINGS_REQUEST);
			break;
		case R.id.menu_refresh:
			updateList();
			break;
		case R.id.menu_download:
			startActivity(new Intent(this, Search.class));
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

    public void fabClick(final View view) {
        PlaylistUtils.newPlaylistDialog(this, new Runnable() {
            public void run() {
                updateList();
            }
        });
    }
}
