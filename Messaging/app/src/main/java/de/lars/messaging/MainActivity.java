package de.lars.messaging;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class MainActivity extends AppCompatActivity {

    public static final int RC_SIGN_IN = 2;

    FirebaseFirestore db;
    FirebaseAuth auth;

    public static final String USER_DB = "users";
    public static final String USER_DB_USERNAME = "userName";
    public static final String CHAT_DB = "chats";
    public static final String CHAT_DB_INDEX = "index";
    public static final String USER_DB_CHAT = "chats";

    private ChatRoomAdapter chatRoomAdapter;
    private ListView chatRoomView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        chatRoomAdapter = new ChatRoomAdapter(this);
        chatRoomView = findViewById(R.id.chat_room_list);
        chatRoomView.setAdapter(chatRoomAdapter);

        chatRoomView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ChatRoom chatRoom = (ChatRoom)parent.getItemAtPosition(position);

                Intent intent = new Intent(getBaseContext(), Messaging.class);
                intent.putExtra("CHAT_ID", chatRoom.getId());
                intent.putExtra("CHAT_PARTNER", chatRoom.getTitle());
                startActivity(intent);
            }
        });

    }

    @Override
    protected void onStart(){
        super.onStart();
        FirebaseUser user = auth.getCurrentUser();
        if(user == null){
            SignIn();
        }else{
            CheckForFirestoreData(user);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                CheckForFirestoreData(user);
            } else {
                Log.e("FIREBASE_LOGIN", response.getError().getErrorCode() + " was the Error" );
            }
        }
    }

    private void SignIn(){
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build());

        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);
    }

    private void CheckForFirestoreData(final FirebaseUser user){
        db.collection(USER_DB)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            boolean foundDocument = false;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.i("FIRESTORE_USER", document.getId() + " => " + document.getData());
                                if(document.getId().equals(user.getUid())){
                                    Log.i("FIRESTORE_USER_FOUND", document.getId() + " => " + document.getData());
                                    foundDocument = true;
                                    LoadChatRooms();
                                }
                            }
                            if(!foundDocument){
                                CreateDocument(user);
                            }
                        } else {
                            Log.w("FIRESTORE_USER", "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    private void CreateDocument(FirebaseUser user){

        Map<String, Object> docData = new HashMap<>();
        docData.put(USER_DB_USERNAME, user.getDisplayName());
        docData.put(USER_DB_CHAT, Collections.emptyList());


        db.collection(USER_DB).document(user.getUid()).set(docData).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i("FIRESTORE_USER", "Added new Document for user " + auth.getUid());
                LoadChatRooms();
            }
        });
    }

    private void LoadChatRooms(){
        chatRoomAdapter.chatRooms.clear();
        DocumentReference docRef = db.collection(USER_DB).document(auth.getUid());
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Map<String, Object> data = document.getData();
                        Log.d("USER_DATA_NAME" , data.get("userName").toString());
                        Log.d("USER_DATA_CHATS" , data.get("chats").getClass().toString());
                        ArrayList<String> chats = (ArrayList<String>)data.get("chats");
                        for(String chat : chats){
                            GetChatRoomPartnerName(chat);
                        }
                    } else {
                        Log.d("USER_DATA", "No such document");
                    }
                } else {
                    Log.d("USER_DATA", "get failed with ", task.getException());
                }
            }
        });
    }

    private void DisplayChatRoom(String chats, String id){

        final UserData data = new UserData(chats);
        final ChatRoom chatRoom = new ChatRoom(id, data.getName(), "LAST MESSAGE", data);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                chatRoomAdapter.add(chatRoom);
                chatRoomView.setSelection(chatRoomView.getCount() - 1);
            }
        });
    }

    private void GetChatRoomPartnerName(final String chat){
        db.collection(USER_DB)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                if(document.getId().equals(auth.getUid())) continue;
                                ArrayList<String> chats = (ArrayList<String>)document.getData().get(USER_DB_CHAT);
                                if(chats == null){
                                    return;
                                }
                                if(chats.contains(chat)){
                                    String user = (String)document.getData().get(USER_DB_USERNAME);
                                    DisplayChatRoom(user, chat);
                                    return;
                                }
                            }
                        } else {
                            Log.w("FIRESTORE_USER", "Error getting documents.", task.getException());
                        }
                    }
                });


    }

    public void onCreateNewChat(View v){
        db.collection(USER_DB)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            Random r = new Random();
                            int max = task.getResult().size() - 1;
                            int randomNum =  r.nextInt((max - 0) + 1) + 0;
                            int index = 0;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                if(index == randomNum){
                                    if(document.getId().equals(auth.getUid())) continue;
                                    if(document.getData().get(USER_DB_USERNAME) != null)
                                        CreateChat(document);
                                    else
                                        Log.e("FIRESTORE_USERNAME", "Could find Firestore userName");

                                    Log.i("FIRESTORE_USER", document.getId() + " => " + document.getData().get("userName"));
                                }
                                index++;
                            }
                        } else {
                            Log.w("FIRESTORE_USER", "Error getting documents.", task.getException());
                        }
                    }
                });
    }


    private void CreateChat(final QueryDocumentSnapshot partner){
        Toast.makeText(this, "Result: " +  partner.getData().get(USER_DB_USERNAME).toString(), Toast.LENGTH_LONG).show();

        Map<String, Object> data = new HashMap<>();
        data.put(CHAT_DB_INDEX, -1);
        data.put("data", Collections.emptyList());

        // Add a new document with a generated ID
        db.collection(CHAT_DB)
                .add(data)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d("", "DocumentSnapshot added with ID: " + documentReference.getId());
                        AddChatToUser(documentReference.getId(), partner.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("", "Error adding document", e);
                    }
                });
    }

    private void AddChatToUser(String id, String partnerId){
        DocumentReference curUser = db.collection(USER_DB).document(auth.getUid());
        curUser.update(USER_DB_CHAT, FieldValue.arrayUnion(id))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                LoadChatRooms();
            }
        });

        DocumentReference partnerUser = db.collection(USER_DB).document(partnerId);
        partnerUser.update(USER_DB_CHAT, FieldValue.arrayUnion(id));

    }

    public void onLogOut(View v){
        auth.signOut();
        finish();
    }
}
