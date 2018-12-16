package de.lars.messaging;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Messaging extends AppCompatActivity {

    private String chatID;
    private String chatPartner;

    FirebaseFirestore db;
    FirebaseAuth auth;

    private MessageAdapter messageAdapter;
    private ListView messagesView;

    private UserData partnerData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messaging);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        chatID = getIntent().getStringExtra("CHAT_ID");
        chatPartner = getIntent().getStringExtra("CHAT_PARTNER");

        partnerData = new UserData(chatPartner);

        messageAdapter = new MessageAdapter(this, auth.getUid());
        messagesView = findViewById(R.id.messages_view);
        messagesView.setAdapter(messageAdapter);


        final DocumentReference docRef = db.collection(MainActivity.CHAT_DB).document(chatID);
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("FIREBASE_REALTIMEDATA", "Listen failed.", e);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    Log.d("FIREBASE_REALTIMEDATA", "Current data: " + snapshot.getData());
                    ProcessData(snapshot);
                } else {
                    Log.d("FIREBASE_REALTIMEDATA", "Current data: null");
                }
            }
        });

    }

    private void ProcessData(DocumentSnapshot snapshot){
        messageAdapter.messages.clear();
        ArrayList<Object> data = (ArrayList<Object>)snapshot.getData().get("data");
        for(int i = 0; i < data.size(); i ++){
            Message m = new Message((HashMap<String, Object>)data.get(i), partnerData);
            messageAdapter.add(m);
            messagesView.setSelection(messagesView.getCount() - 1);
        }
    }

    public void onSendMessage(View v){
        EditText messageEt = findViewById(R.id.messageText);
        String message = messageEt.getText().toString();
        messageEt.setText("");
        UserData user = new UserData(auth.getCurrentUser().getDisplayName());
        final Message m  = new Message(user, auth.getUid(), message);

        DocumentReference docRef = db.collection(MainActivity.CHAT_DB).document(chatID);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();

                    final Long index = (Long)document.getData().get(MainActivity.CHAT_DB_INDEX) + 1;

                    final DocumentReference chatDB = db.collection(MainActivity.CHAT_DB).document(chatID);

                    Map<String, Object> data = new HashMap<>();
                    data.put("index", index);
                    data.put("user", m.getUserID());
                    data.put("message", m.getMessage());

                    chatDB.update("data", FieldValue.arrayUnion(data));
                    chatDB.update(MainActivity.CHAT_DB_INDEX, index);

                }

            }
        });

    }
}
