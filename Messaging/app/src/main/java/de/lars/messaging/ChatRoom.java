package de.lars.messaging;

public class ChatRoom {
    private String title;
    private String lastMessage;
    private String id;
    private UserData data; // data of the user that sent this message

    public ChatRoom(String id, String title, String lastMessage, UserData data) {
        this.id = id;
        this.title = title;
        this.lastMessage = lastMessage;
        this.data = data;
    }

    public String getTitle() {
        return title;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public UserData getData() {
        return data;
    }

    public String getId(){
        return id;
    }

}
