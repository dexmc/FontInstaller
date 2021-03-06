package com.chromium.fontinstaller;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class StorageInstall extends Activity {

	Button selectRegular, install;
	String fontFile = null;
	String regPath, regFile;
	TextView regPathTV;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.storage_install);

		// Look up the AdView as a resource and load a request.
		AdView adView = (AdView) findViewById(R.id.adView);
		AdRequest adRequest = new AdRequest.Builder()
		.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)        // All emulators
		.addTestDevice("2797F5D9304B6B3A15771A0519A4F687")  // HTC Desire
		.addTestDevice("D674E5DF79F70B01D8866A5F99A2ACBA") // Samsung i9000
    	.build();
		adView.loadAd(adRequest);
		
		CustomAlerts.showBasicAlert("Warning", "This feature is still currently in development." +
				" Only use it if you know what you are doing, and ensure that you only select .ttf font files. Some" +
				"font files will not work and may result in a bootloop. Use with caution.", StorageInstall.this);
		regPathTV = (TextView)findViewById(R.id.regPath);

		selectRegular = (Button)findViewById(R.id.selectRegular);
		selectRegular.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v){
				showFileListDialog(Environment.getExternalStorageDirectory().toString(), StorageInstall.this);
			}
		});

		install = (Button)findViewById(R.id.install);
		install.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v){
				regPath = regPathTV.getText().toString();

				int index = regPath.lastIndexOf("/");
				regFile = regPath.substring(index + 1);

				//installation start
				AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>()  { 
					//display progress dialog while fonts are copied in background
					ProgressDialog copyProgress;

					@Override
					protected void onPreExecute() {
						super.onPreExecute();						
						copyProgress = new ProgressDialog (StorageInstall.this);
						copyProgress.setMessage("Installing specified font...");
						copyProgress.setCancelable(false);
						copyProgress.setCanceledOnTouchOutside(false);
						copyProgress.show();					
					}

					@Override
					protected Void doInBackground(Void... params) {					

						try {
							Process process = Runtime.getRuntime().exec("su");
							OutputStream stdin = process.getOutputStream();

							stdin.write(("mount -o rw,remount /system\n").getBytes());
							stdin.write(("mkdir /sdcard/TempFonts\n").getBytes());
							stdin.write(("cp " + regPath + " /sdcard/TempFonts\n").getBytes());
							stdin.write(("mv /sdcard/TempFonts/" + regFile + " /sdcard/TempFonts/Roboto-Regular.ttf\n").getBytes());
							stdin.write(("cp /sdcard/TempFonts/Roboto-Regular.ttf /system/fonts\n").getBytes());

							stdin.flush();
							stdin.close();

							process.waitFor();
							process.destroy();
						} 
						catch (IOException | InterruptedException e) {
							e.printStackTrace();
						}
						return null;
					}

					@Override
					protected void onPostExecute(Void result) {
						super.onPostExecute(result);
						if (copyProgress != null) {
							if (copyProgress.isShowing()) {
								copyProgress.dismiss();
							}
						}
						CustomAlerts.showRebootAlert("Installation successful", "You must reboot for the changes to take effect", "Reboot", StorageInstall.this);

					}
				};
				task.execute((Void[])null);
				//installation end

			}
		});		

	}


	/*
	 * Credits for this mini file explorer go to
	 * the user schwiz on StackOverflow
	 */
	private File[] fileList;
	private String[] filenameList;
	private File[] loadFileList(String directory) {
		File path = new File(directory);

		if(path.exists()) {
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String filename) {
					return true;
				}
			};

			//if null return an empty array instead
			File[] list = path.listFiles(filter); 
			return list == null? new File[0] : list;
		} else {
			return new File[0];
		}
	}

	public void showFileListDialog(final String directory, final Context context){
		Dialog dialog = null;
		AlertDialog.Builder builder = new Builder(context);

		File[] tempFileList = loadFileList(directory);

		//if directory is root, no need to up one directory
		if(directory.equals("/")){
			fileList = new File[tempFileList.length];
			filenameList = new String[tempFileList.length];

			//iterate over tempFileList
			for(int i = 0; i < tempFileList.length; i++){
				fileList[i] = tempFileList[i];
				filenameList[i] = tempFileList[i].getName();
			}
		} else {
			fileList = new File[tempFileList.length+1];
			filenameList = new String[tempFileList.length+1];

			//add an "up" option as first item
			fileList[0] = new File(upOneDirectory(directory));
			filenameList[0] = "...";

			//iterate over tempFileList
			for(int i = 0; i < tempFileList.length; i++){
				fileList[i+1] = tempFileList[i];
				filenameList[i+1] = tempFileList[i].getName();
			}
		}

		builder.setTitle("Choose your font file:");

		builder.setItems(filenameList, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				File chosenFile = fileList[which];

				if(chosenFile.isDirectory()) {
					showFileListDialog(chosenFile.getAbsolutePath(), context);
				}
				else {
					dialog.cancel();
					fontFile = chosenFile.getAbsolutePath();
					regPathTV.setText(fontFile);
				}
			}
		});

		dialog = builder.create();
		dialog.show();
	}

	public String upOneDirectory(String directory){
		String[] dirs = directory.split("/");
		StringBuilder stringBuilder = new StringBuilder("");

		for(int i = 0; i < dirs.length-1; i++)
			stringBuilder.append(dirs[i]).append("/");

		return stringBuilder.toString();
	}

}
