package de.lars.messaging;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ChatRoomAdapter extends BaseAdapter {
    List<ChatRoom> chatRooms = new ArrayList();
    Context context;

    public ChatRoomAdapter(Context context) {
        this.context = context;
    }

    public void add(ChatRoom room) {
        this.chatRooms.add(room);
        notifyDataSetChanged(); // to render the list we need to notify
    }

    @Override
    public int getCount() {
        return chatRooms.size();
    }

    @Override
    public Object getItem(int i) {
        return chatRooms.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }


    // This is the backbone of the class, it handles the creation of single ListView row (chat bubble)
    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        ChatRoomViewHolder holder = new ChatRoomViewHolder();
        LayoutInflater messageInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        ChatRoom message = chatRooms.get(i);

        convertView = messageInflater.inflate(R.layout.chat_room, null);
        holder.title = (TextView) convertView.findViewById(R.id.titleTv);
        holder.lastMessage = (TextView) convertView.findViewById(R.id.lastMessageTv);
        holder.avatar = convertView.findViewById(R.id.avatar);
        convertView.setTag(holder);

        holder.title.setText(message.getTitle());
        holder.lastMessage.setText(message.getLastMessage());
        GradientDrawable drawable = (GradientDrawable) holder.avatar.getBackground();
        drawable.setColor(Color.parseColor(message.getData().getColor()));


        return convertView;
    }

}

class ChatRoomViewHolder {
    public View avatar;
    public TextView title;
    public TextView lastMessage;
}
