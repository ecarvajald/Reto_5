package co.edu.unal.chatbot;

public class Message {
    public String text;
    public boolean isUser; // true = usuario, false = bot

    public Message(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
    }
}
