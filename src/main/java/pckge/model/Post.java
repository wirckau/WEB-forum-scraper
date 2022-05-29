package pckge.model;

public class Post {
    private String author;
    private String comment;
    private String date;

    public Post (String author, String date, String comment) {
        this.author = author;
        this.date = date;
        this.comment = comment;
    }
    public String getAuthor() {
        return author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }
    public String getComment() {
        return comment;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }
    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }
}