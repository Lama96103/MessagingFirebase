package de.lars.messaging;

import com.firebase.ui.auth.data.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class Message {

    private UserData user;
    private String userID;
    private String message;

    public Message(UserData user, String userID, String message) {
        this.user = user;
        this.userID = userID;
        this.message = message;
    }

    public Message(HashMap<String, Object> data, UserData user) {
        this.user = user;
        this.userID = (String)data.get("user");
        this.message = (String)data.get("message");
    }

    public UserData getUser() {
        return user;
    }

    public String getMessage() {
        return message;
    }

    public String getUserID() {
        return userID;
    }

    public ArrayList<String> getList(){
        ArrayList<String> result = new ArrayList<>();
        result.add(userID);
        result.add(message);
        return result;
    }
}
