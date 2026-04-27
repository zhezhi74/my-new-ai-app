package com.example.chatservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.service.OpenAiService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.time.Duration;

@Configuration
public class AiConfig {

    /**
     * 【核心修复】：明确指定 Bean 的名称为 deepSeekChatService
     * 这样才能匹配 ChatService 构造函数里的 @Qualifier("deepSeekChatService")
     */
    @Bean(name = "deepSeekChatService")
    @Qualifier("deepSeekChatService")
    @Primary // 作为主聊天的默认 AI 服务
    public OpenAiService deepSeekChatService(
            @Value("${ai.deepseek.api-key}") String apiKey,
            @Value("${ai.deepseek.base-url}") String baseUrl
    ) {
        return createOpenAiService(apiKey, baseUrl);
    }

    // 公共的创建方法（保留你原有的逻辑）
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
                .connectTimeout(Duration.ofSeconds(60))
                .readTimeout(Duration.ofSeconds(60))
                .writeTimeout(Duration.ofSeconds(60))
                .build();

        // 确保 baseUrl 格式正确
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        OpenAiApi api = retrofit.create(OpenAiApi.class);
        return new OpenAiService(api, client.dispatcher().executorService());
    }
}