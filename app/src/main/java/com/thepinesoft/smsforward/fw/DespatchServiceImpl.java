package com.thepinesoft.smsforward.fw;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.telephony.SmsManager;
import android.util.Log;

import com.thepinesoft.smsforward.global.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vtran on 8/3/17.
 */

public class DespatchServiceImpl extends IntentService  implements ApplicationService{
    private final SmsManager smsManager;
    public DespatchServiceImpl(){
        super("EmailToSMSService");
        smsManager = SmsManager.getDefault();
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        execute(null);
    }

    @Override
    public ServiceErrorCode execute(ContentValues params){
        SQLiteDatabase msgDb = Autowired.getMsgDatabase();
        String unsentMsgQuery = "SELECT id, content, fr_no, to_no, fr_email, to_email, status FROM msg" +
                " WHERE status = 'A' ORDER BY id ASC";
        if (msgDb != null) {
            Cursor cursor = msgDb.rawQuery(unsentMsgQuery, null);
            Log.d("DespatchService", "queried onto Database");
            if (!cursor.isAfterLast() && !cursor.isBeforeFirst()) {
                int idxId = cursor.getColumnIndex("id");
                //int idxFrNo = cursor.getColumnIndex("fr_no");//for future use
                int idxToNo = cursor.getColumnIndex("to_no");
                int idxMsg = cursor.getColumnIndex("content");
                List<String> updatedIds = new ArrayList<>();
                StringBuilder args = new StringBuilder();
                while (!cursor.isAfterLast() && !cursor.isBeforeFirst()) {
                    String msg = cursor.getString(idxMsg);
                    //String frNo = cursor.getString(idxFrNo); //this is for future use
                    String toNo = cursor.getString(idxToNo);
                    int id = cursor.getInt(idxId);
                    smsManager.sendTextMessage(toNo, null, msg, null, null);
                    updatedIds.add(String.valueOf(id));
                    cursor.moveToNext();
                    args.append(",?");
                }
                ContentValues contentValues = new ContentValues();
                contentValues.put("status", "S");
                String selection = "id IN (" + args.toString().substring(1) + ")";//remove first ,
                msgDb.update("msg", contentValues, selection, updatedIds.toArray(new String[0]));
            }
            cursor.close();
        }
        return ServiceErrorCode.OK;
    }
}
