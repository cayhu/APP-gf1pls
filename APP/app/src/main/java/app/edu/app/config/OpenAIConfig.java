package app.edu.app.config;

/**
 * OpenAI Configuration
 * Lưu các thông tin cấu hình OpenAI API
 */
public class OpenAIConfig {
    // API Key - Trong production nên lưu trong secure storage hoặc server
    public static final String API_KEY = "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";
    
    // API Endpoint
    public static final String API_URL = "https://api.openai.com/v1/chat/completions";
    
    // Model mặc định (có thể thay đổi)
    public static final String DEFAULT_MODEL = "gpt-4o-mini";
    
    // Các model có sẵn
    public static final String[] AVAILABLE_MODELS = {
        "gpt-4o-mini",
        "gpt-4o",
        "gpt-4-turbo",
        "gpt-5-nano-2025-08-07",
        "gpt-5-2025-08-07",
        "gpt-5",
        "gpt-5-pro-2025-10-06",
        "gpt-5-pro",
        "gpt-realtime",
        "gpt-5-mini",
        "gpt-4o-realtime-preview-2024-12-17",
        "gpt-5-mini-2025-08-07",
        "gpt-4-turbo-preview",
        "gpt-5-nano",
        "gpt-5-chat-latest",
        "o4-mini-deep-research",
        "gpt-4o-realtime-preview-2025-06-03",
        "gpt-5-search-api"
    };
    
    // Temperature cho AI (0.0 - 2.0, càng cao càng sáng tạo)
    public static final double DEFAULT_TEMPERATURE = 0.7;
    
    // Max tokens cho response
    public static final int MAX_TOKENS = 500;
}

