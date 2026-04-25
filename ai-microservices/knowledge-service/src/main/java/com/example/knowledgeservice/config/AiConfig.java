package com.example.knowledgeservice.config; // 注意：包名可能需要你根据自己的项目结构修改

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
    // 连接本地Ollama
    @Bean
    @Qualifier("embeddingOpenAiService") // 使用 @Qualifier 来标识这个特定的 Bean
    public OpenAiService embeddingOpenAiService(
            @Value("${ai.ollama-embedding.api-key}") String apiKey,
            @Value("${ai.ollama-embedding.base-url}") String baseUrl
    ) {
        // createOpenAiService 方法会被自动调用
        return createOpenAiService(apiKey, baseUrl);
    }


    // 公共的创建方法
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

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        OpenAiApi api = retrofit.create(OpenAiApi.class);
        return new OpenAiService(api);
    }

    // Weaviate的配置
    @Bean
    public WeaviateClient weaviateClient() {
        Config config = new Config("http", "localhost:8090");
        return new WeaviateClient(config);
    }
}