package com.example.knowledgeservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.service.OpenAiService;
import io.weaviate.client.Config;
import io.weaviate.client.WeaviateClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.time.Duration;

@Configuration
public class AiConfig {

    // =========================================================================
    // 1. 本地 Ollama (bge-m3) 向量化模型配置
    // =========================================================================
    @Bean
    @Qualifier("embeddingOpenAiService")
    public OpenAiService embeddingOpenAiService(
            @Value("${ai.ollama-embedding.api-key}") String apiKey,
            @Value("${ai.ollama-embedding.base-url}") String baseUrl
    ) {
        return createOpenAiService(apiKey, baseUrl);
    }

    private OpenAiService createOpenAiService(String apiKey, String baseUrl) {
        ObjectMapper mapper = OpenAiService.defaultObjectMapper();
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request request = chain.request()
                            .newBuilder()
                            .header("Authorization", "Bearer " + apiKey)
                            .build();
                    return chain.proceed(request);
                })
                // 向量化可能需要一定时间，保留你原本设置的 60 秒超时，非常稳妥
                .connectTimeout(Duration.ofSeconds(60))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(60))
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        OpenAiApi api = retrofit.create(OpenAiApi.class);
        return new OpenAiService(api);
    }

    // =========================================================================
    // 2. Weaviate 向量数据库客户端配置
    // =========================================================================
    @Bean
    public WeaviateClient weaviateClient(@Value("${ai.weaviate.host:localhost:8090}") String host) {
        // 使用配置文件中的 host，如果没有配置则默认使用 localhost:8090
        Config config = new Config("http", host);
        return new WeaviateClient(config);
    }

    // =========================================================================
    // 3. 【新增】：官方 DeepSeek (Chat) 模型配置
    // =========================================================================
    @Bean
    @Qualifier("deepSeekChatService")
    public OpenAiService deepSeekChatService(
            @Value("${ai.deepseek.api-key}") String apiKey
    ) {
        // DeepSeek 的官方接口地址与 OpenAI 完全兼容，直接复用你的底层构建器
        // 注意：DeepSeek 官方 baseUrl 必须以斜杠 / 结尾
        return createOpenAiService(apiKey, "https://api.deepseek.com/");
    }

}