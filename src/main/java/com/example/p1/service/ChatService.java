package com.example.p1.service;

import com.example.p1.config.AiConfiguration;
import com.example.p1.controller.ChatController;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final WebClient webClient;
    private final AiConfiguration config;

    public ChatService(WebClient geminiWebClient, AiConfiguration config) {
        this.webClient = geminiWebClient;
        this.config = config;
    }

    public Mono<String> generateResponse(String message) {
        logger.debug("Generating response for message: {}", message);

        GeminiRequest request = createSimpleRequest(message);

        return callGeminiAPI(request)
                .map(this::extractTextFromResponse)
                .doOnSuccess(response -> logger.debug("Generated response: {}", response))
                .doOnError(error -> logger.error("Error generating response", error));
    }

    public Mono<String> generateConversationResponse(List<ChatController.ConversationRequest.Message> messages) {
        logger.debug("Generating conversation response for {} messages", messages.size());

        String conversationContext = buildConversationContext(messages);
        return generateResponse(conversationContext);
    }

    public Flux<String> generateStreamingResponse(String message) {
        // For demonstration - in real implementation, you'd use Gemini's streaming API
        return generateResponse(message)
                .flatMapMany(response -> {
                    String[] words = response.split("\\s+");
                    return Flux.fromArray(words)
                            .delayElements(Duration.ofMillis(100))
                            .map(word -> word + " ");
                });
    }

    private Mono<GeminiResponse> callGeminiAPI(GeminiRequest request) {
        return webClient.post()
                .uri("/{model}:generateContent?key={apiKey}",
                        config.getModel(), config.getKey())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GeminiResponse.class)
                .timeout(Duration.ofMillis(config.getTimeout()));
    }

    private GeminiRequest createSimpleRequest(String message) {
        GeminiRequest.Part part = new GeminiRequest.Part(message);
        GeminiRequest.Content content = new GeminiRequest.Content(List.of(part));
        return new GeminiRequest(List.of(content));
    }

    private String buildConversationContext(List<ChatController.ConversationRequest.Message> messages) {
        StringBuilder context = new StringBuilder();
        context.append("Previous conversation:\n");

        for (ChatController.ConversationRequest.Message msg : messages) {
            context.append(msg.getRole().toUpperCase())
                    .append(": ")
                    .append(msg.getContent())
                    .append("\n");
        }

        return context.toString();
    }

    private String extractTextFromResponse(GeminiResponse response) {
        if (response.getCandidates() != null && !response.getCandidates().isEmpty()) {
            GeminiResponse.Candidate candidate = response.getCandidates().get(0);
            if (candidate.getContent() != null &&
                    candidate.getContent().getParts() != null &&
                    !candidate.getContent().getParts().isEmpty()) {
                return candidate.getContent().getParts().get(0).getText();
            }
        }
        return "No response generated";
    }

    // Gemini API DTOs
    public static class GeminiRequest {
        private List<Content> contents;

        public GeminiRequest(List<Content> contents) {
            this.contents = contents;
        }

        public List<Content> getContents() { return contents; }
        public void setContents(List<Content> contents) { this.contents = contents; }

        public static class Content {
            private List<Part> parts;

            public Content(List<Part> parts) {
                this.parts = parts;
            }

            public List<Part> getParts() { return parts; }
            public void setParts(List<Part> parts) { this.parts = parts; }
        }

        public static class Part {
            private String text;

            public Part(String text) {
                this.text = text;
            }

            public String getText() { return text; }
            public void setText(String text) { this.text = text; }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeminiResponse {
        private List<Candidate> candidates;

        public List<Candidate> getCandidates() { return candidates; }
        public void setCandidates(List<Candidate> candidates) { this.candidates = candidates; }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Candidate {
            private Content content;

            public Content getContent() { return content; }
            public void setContent(Content content) { this.content = content; }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Content {
            private List<Part> parts;

            public List<Part> getParts() { return parts; }
            public void setParts(List<Part> parts) { this.parts = parts; }
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Part {
            private String text;

            public String getText() { return text; }
            public void setText(String text) { this.text = text; }
        }
    }
}