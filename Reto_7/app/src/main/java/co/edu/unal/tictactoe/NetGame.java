package co.edu.unal.tictactoe;

public class NetGame {
    public String id;
    public String hostId;
    public String guestId;
    public String status;    // "waiting", "active", "finished"
    public long   createdAt; // ServerValue.TIMESTAMP no serializa directo, guardamos long

    // Firebase necesita constructor vacío
    public NetGame() {}

    public NetGame(String id, String hostId, String status, long createdAt) {
        this.id = id;
        this.hostId = hostId;
        this.status = status;
        this.createdAt = createdAt;
    }
}
