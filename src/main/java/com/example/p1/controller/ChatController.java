package com.example.p1.controller;

import com.example.p1.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/message")
    public Mono<ResponseEntity<ChatResponse>> sendMessage(@Valid @RequestBody ChatRequest request) {
        return chatService.generateResponse(request.getMessage())
                .map(response -> ResponseEntity.ok(new ChatResponse(response, true, null)))
                .onErrorReturn(ResponseEntity.badRequest()
                        .body(new ChatResponse(null, false, "Failed to generate response")));
    }

    @PostMapping("/conversation")
    public Mono<ResponseEntity<ChatResponse>> sendConversation(@Valid @RequestBody ConversationRequest request) {
        return chatService.generateConversationResponse(request.getMessages())
                .map(response -> ResponseEntity.ok(new ChatResponse(response, true, null)))
                .onErrorReturn(ResponseEntity.badRequest()
                        .body(new ChatResponse(null, false, "Failed to generate response")));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamMessage(@Valid @RequestBody ChatRequest request) {
        return chatService.generateStreamingResponse(request.getMessage());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Chat Service",
                "timestamp", System.currentTimeMillis()
        ));
    }

    // DTOs
    public static class ChatRequest {
        @NotBlank(message = "Message cannot be blank")
        private String message;

        public ChatRequest() {}

        public ChatRequest(String message) {
            this.message = message;
        }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class ConversationRequest {
        @Valid
        private List<Message> messages;

        public ConversationRequest() {}

        public ConversationRequest(List<Message> messages) {
            this.messages = messages;
        }

        public List<Message> getMessages() { return messages; }
        public void setMessages(List<Message> messages) { this.messages = messages; }

        public static class Message {
            @NotBlank
            private String role; // "user" or "assistant"
            @NotBlank
            private String content;

            public Message() {}

            public Message(String role, String content) {
                this.role = role;
                this.content = content;
            }

            public String getRole() { return role; }
            public void setRole(String role) { this.role = role; }

            public String getContent() { return content; }
            public void setContent(String content) { this.content = content; }
        }
    }

    public static class ChatResponse {
        private String response;
        private boolean success;
        private String error;
        private long timestamp;

        public ChatResponse(String response, boolean success, String error) {
            this.response = response;
            this.success = success;
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }

        public String getResponse() { return response; }
        public void setResponse(String response) { this.response = response; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}