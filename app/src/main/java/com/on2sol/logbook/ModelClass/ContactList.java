package com.on2sol.logbook.ModelClass;

import android.content.Context;
import android.databinding.ObservableArrayList;
import android.util.Log;
import android.view.View;

import com.on2sol.logbook.APICalls.VolleyCall;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

/**
 * Created by Umair Saeed on 9/23/2017.
 */

public class ContactList implements VolleyCall.DataInterface{
    private static final String TAG = "ContactList";
    public ObservableArrayList<Contact> list = new ObservableArrayList<>();
    private int mTotalCount;
    private Realm realm;
    private VolleyCall volleyCall;
    private Context context;

    public interface DataProcess{
        public void onProcessSuccess();
    }
    private DataProcess dataProcess;
    public ContactList(DataProcess callbackClass, Context context) {
        this.context = context;
        this.dataProcess = callbackClass;
        realm = Realm.getDefaultInstance();
        volleyCall = new VolleyCall(ContactList.this, context);
    }

    public void fetchData() {
        volleyCall.getDataFromServer();
    }

    public void store(final String name, final String email, final String address, final String image){
        volleyCall.storeData(name, email, address, image);
    }
    public void deleteDat(String email) {
        volleyCall.deleteData(email);
    }

    public void save(View view, final Contact contact){
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm bgRealm) {
                if (checkIfExists(bgRealm, contact.email)){
                    Contact cObj = bgRealm.where(Contact.class).equalTo("email", contact.email).findFirst();
                    cObj.name = contact.name;
                    cObj.address = contact.address;
                    cObj.profile = contact.profile;
                }
                else{
                    Contact realmContact = bgRealm.createObject(Contact.class);
                    realmContact.name = contact.name;
                    realmContact.email = contact.email;
                    realmContact.profile = contact.profile;
                    realmContact.address = contact.address;
                }
            }
    }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                // Transaction was a success.
                dataProcess.onProcessSuccess();
                Log.d(TAG,"Realm.Transaction.OnSuccess()");

            }
        }, new Realm.Transaction.OnError() {
            @Override
            public void onError(Throwable error) {
                // Transaction failed and was automatically canceled.
                Log.d(TAG,"onError(Throwable error)");
            }
        });
    }


    public ObservableArrayList<Contact> get(View view){
        RealmResults<Contact> results = realm.where(Contact.class).findAll();
        Log.d(TAG, String.valueOf(results));

        ObservableArrayList<Contact> retVal = new ObservableArrayList<Contact>();
        for (int i=0; i<results.size(); i++) {
            Contact realmObj = results.get(i);
            retVal.add(realmObj);
        }
        return retVal;
    }

    private void deleteData(final String email){
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm bgRealm) {
                if (checkIfExists(bgRealm, email)){
                    RealmResults<Contact> result = bgRealm.where(Contact.class).equalTo("email", email).findAll();
                    result.deleteAllFromRealm();
                    Log.d(TAG, "deleteData2");
                }
            }
        }, new Realm.Transaction.OnSuccess() {
            @Override
            public void onSuccess() {
                // Transaction was a success.
                dataProcess.onProcessSuccess();
                Log.d(TAG,"Realm.Transaction.OnSuccess()");

            }
        }, new Realm.Transaction.OnError() {
            @Override
            public void onError(Throwable error) {
                // Transaction failed and was automatically canceled.
                Log.d(TAG,"onError(Throwable error)");
            }
        });

//        realm.executeTransaction(new Realm.Transaction() {
//            @Override
//            public void execute(Realm bgRealm) {
//                RealmResults<Contact> result = bgRealm.where(Contact.class).equalTo("email", email).findAll();
//                result.deleteAllFromRealm();
//                Log.d(TAG, "deleteData2");
//                dataProcess.onProcessSuccess();
//            }
//        });
    }

    @Override
    public void onDataRetrived(String response) {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(response);
            if (jsonObject.getString("status").equalsIgnoreCase("1")){
                JSONArray jsonArray = jsonObject.getJSONArray("value");
                for (int i=0; i<jsonArray.length()-1; i++){
                    JSONObject obj = jsonArray.getJSONObject(i);
                    Contact c = new Contact();
                    c.email = obj.getString("email");
                    c.name = obj.getString("name");
                    c.profile = obj.getString("image");
                    c.address = obj.getString("address");
                    this.save(null, c);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDataStore(Contact response) {
//        JSONObject jsonObject = null;
//        try {
//            jsonObject = new JSONObject(response);
//            if (jsonObject.getString("status").equalsIgnoreCase("1") ){
//                JSONArray jsonArray = jsonObject.getJSONArray("value");
//                for (int i=0; i<jsonArray.length(); i++){
//                    JSONObject obj = jsonArray.getJSONObject(i);
//                    Contact c = new Contact();
//                    c.email = obj.getString("email");
//                    c.name = obj.getString("name");
//                    c.address = obj.getString("address");
//                    c.profile = obj.getString("image");
                    this.save(null, response);
//                }
//            }
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void onDeleteData(String response) {
        deleteData(response);
    }

    private boolean checkIfExists(Realm realmM, String email){
        RealmQuery<Contact> query = realmM.where(Contact.class).equalTo("email", email);
        return query.count() != 0;
    }
}
