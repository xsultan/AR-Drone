package com.parrot.freeflight.activities.base;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.parrot.freeflight.R;

public class CopyOfMainActivity extends Activity {

	RelativeLayout leftRL;
	RelativeLayout rightRL;
	DrawerLayout drawerLayout;

	private ListView leftListView, rightListView;
	private ArrayAdapter<String> listAdapter_left, listAdapter_right;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// I'm removing the ActionBar.
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main1);

		leftRL = (RelativeLayout) findViewById(R.id.whatYouWantInLeftDrawer);
		rightRL = (RelativeLayout) findViewById(R.id.whatYouWantInRightDrawer);
		drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

		leftListView = (ListView) findViewById(R.id.leftListView);
		rightListView = (ListView) findViewById(R.id.rightListView);

		String[] leftItems = new String[] { "Mercury", "Venus", "Earth",
				"Mars", "Jupiter" };
		String[] righttItems = new String[] { "Saturn", "Uranus", "Neptune" };

		ArrayList<String> leftItemsList = new ArrayList<String>();
		leftItemsList.addAll(Arrays.asList(leftItems));

		ArrayList<String> righttItemsList = new ArrayList<String>();
		righttItemsList.addAll(Arrays.asList(righttItems));

		// Create ArrayAdapter using the planet list.
		listAdapter_left = new ArrayAdapter<String>(this, R.layout.list_item,
				leftItemsList);
		listAdapter_right = new ArrayAdapter<String>(this, R.layout.list_item,
				righttItemsList);

		// Set the ArrayAdapter as the ListView's adapter.
		leftListView.setAdapter(listAdapter_left);
		leftListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Toast.makeText(getApplicationContext(),
						Integer.toString(position), Toast.LENGTH_SHORT).show();
			}
		});

		rightListView.setAdapter(listAdapter_right);
		rightListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Toast.makeText(getApplicationContext(),
						Integer.toString(position), Toast.LENGTH_SHORT).show();
			}
		});

	}

}
