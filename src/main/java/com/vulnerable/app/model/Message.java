package com.vulnerable.app.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String author;

    // ⚠️ VULNERABILITY: Content rendered as raw HTML — enables XSS
    @Column(columnDefinition = "TEXT")
    private String content;

    // Constructors
    public Message() {}

    public Message(Long id, String author, String content) {
        this.id = id;
        this.author = author;
        this.content = content;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
