package de.lars.messaging;

import java.util.Random;

public class UserData {

    private String name;
    private String color;

    public UserData(String name, String color) {
        this.name = name;
        this.color = color;
    }

    public UserData(String name) {
        this.name = name;
        this.color = getRandomColor();
    }


    // Add an empty constructor so we can later parse JSON into MemberData using Jackson
    public UserData() {
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    private String getRandomColor() {
        Random r = new Random();
        StringBuffer sb = new StringBuffer("#");
        while(sb.length() < 7){
            sb.append(Integer.toHexString(r.nextInt()));
        }
        return sb.toString().substring(0, 7);
    }


}
