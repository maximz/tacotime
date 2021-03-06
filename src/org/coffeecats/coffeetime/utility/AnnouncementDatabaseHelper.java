/** This class implements the Database Access Object helper used by the TacoTime game engine. 
 * We use this to save and load games to/from the "saved game database", in conjunction with
 * GameDatabase (which is the primary Database Access Object).
 * 
 * Inspired / educated by http://www.vogella.com/articles/AndroidSQLite/article.html#overview
 */

package org.coffeecats.coffeetime.utility;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class AnnouncementDatabaseHelper extends SQLiteOpenHelper{
	public static final String DATABASE_NAME = "tacotime_annoucements.db";
	private static final int DATABASE_VERSION = 1;

	public AnnouncementDatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(createSavedCharacterDB());
	}
	
	/** Generates the String used to correctly create the saved character database and 
	 * set up the tables.
	 * @return
	 */
	private String createSavedCharacterDB() {	
		return(ServerAnnouncement.createServerAnnouncementDBString());
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {		
		Log.w(AnnouncementDatabaseHelper.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + ServerAnnouncement.TABLE_NAME);
		onCreate(db);
	}
}
