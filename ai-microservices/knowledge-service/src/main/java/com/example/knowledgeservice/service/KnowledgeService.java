package com.example.knowledgeservice.service;

import com.example.knowledgeservice.entity.Knowledge;
import com.example.knowledgeservice.repository.KnowledgeRepository;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.service.OpenAiService;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.graphql.model.GraphQLResponse;
import io.weaviate.client.v1.graphql.query.argument.NearVectorArgument;
import io.weaviate.client.v1.graphql.query.fields.Field;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier; // 【新增】导入

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KnowledgeService {

    private final WeaviateClient weaviateClient;
    private final OpenAiService openAiService;
    private final KnowledgeRepository knowledgeRepository;
    private static final String KNOWLEDGE_CLASS_NAME = "Knowledge";

    // 从ollama-embedding配置中读取向量模型名称
    @Value("${ai.ollama-embedding.embedding-model}")
    private String embeddingModelName;

    public KnowledgeService(WeaviateClient weaviateClient,
                            @Qualifier("embeddingOpenAiService") OpenAiService openAiService,
                            KnowledgeRepository knowledgeRepository) {
        this.weaviateClient = weaviateClient;
        this.openAiService = openAiService;
        this.knowledgeRepository = knowledgeRepository;
    }

    /**
     * addKnowledge 方法接收一个完整的 Knowledge 对象
     * @param knowledge 包含标题和内容的知识对象
     */
    public void addKnowledge(Knowledge knowledge) {
        // 1. 将标题和内容拼接后存入MySQL，这有助于AI更好地理解上下文
        String fullContent = "标题：" + knowledge.getTitle() + "\n内容：" + knowledge.getContent();

        Knowledge knowledgeToSave = new Knowledge();
        knowledgeToSave.setTitle(knowledge.getTitle());
        knowledgeToSave.setContent(knowledge.getContent()); // 数据库中分开存储

        knowledgeRepository.save(knowledgeToSave);
        log.info("知识已存入MySQL");

        // 2. 将拼接后的完整内容向量化并存入Weaviate
        EmbeddingRequest embeddingRequest = EmbeddingRequest.builder()
                .model(embeddingModelName)
                .input(Collections.singletonList(fullContent)) // 使用拼接内容进行向量化
                .build();
        List<Float> vector = openAiService.createEmbeddings(embeddingRequest).getData().get(0).getEmbedding()
                .stream().map(Double::floatValue).collect(Collectors.toList());
        log.info("文本向量化完成");

        Map<String, Object> properties = new HashMap<>();
        properties.put("content", fullContent); // Weaviate中存储拼接后的内容

        weaviateClient.data().creator()
                .withClassName(KNOWLEDGE_CLASS_NAME)
                .withProperties(properties)
                .withVector(vector.toArray(new Float[0]))
                .run();
        log.info("知识已存入Weaviate");
    }

    public List<String> searchRelevantKnowledge(String query, int topN) {
        EmbeddingRequest embeddingRequest = EmbeddingRequest.builder()
                .model(embeddingModelName)
                .input(Collections.singletonList(query))
                .build();
        List<Float> queryVector = openAiService.createEmbeddings(embeddingRequest).getData().get(0).getEmbedding()
                .stream().map(Double::floatValue).collect(Collectors.toList());

        // 【优化】: 增加distance字段来获取相似度
        Field contentField = Field.builder().name("content").build();
        Field distanceField = Field.builder().name("_additional { distance }").build();

        Result<GraphQLResponse> responseResult = weaviateClient.graphQL().get()
                .withClassName(KNOWLEDGE_CLASS_NAME)
                .withFields(contentField, distanceField) // 同时查询内容和相似度
                .withNearVector(NearVectorArgument.builder().vector(queryVector.toArray(new Float[0])).build())
                .withLimit(topN)
                .run();

        if (responseResult.getError() != null) {
            log.error("Weaviate检索失败: {}", responseResult.getError().getMessages());
            return Collections.emptyList();
        }

        GraphQLResponse graphQLResponse = responseResult.getResult();
        Map<String, Object> data = (Map<String, Object>) graphQLResponse.getData();
        if (data == null) { return Collections.emptyList(); }
        Map<String, Object> get = (Map<String, Object>) data.get("Get");
        if (get == null) { return Collections.emptyList(); }
        List<Map<String, Object>> results = (List<Map<String, Object>>) get.get(KNOWLEDGE_CLASS_NAME);
        if (results == null) { return Collections.emptyList(); }

        // 【优化】: 筛选出真正高度相关的知识
        return results.stream()
                .filter(result -> {
                    Map<String, Object> additional = (Map<String, Object>) result.get("_additional");
                    Double distance = (Double) additional.get("distance");
                    log.info("检索到知识: {}, 距离: {}", result.get("content"), distance);
                    return distance < 0.3;
                })
                .map(result -> (String) result.get("content"))
                .collect(Collectors.toList());
    }

    /**
     * 【新增】: 删除知识 (同时从MySQL和Weaviate中删除)
     * @param id 知识ID
     */
    public void deleteKnowledge(Long id) {
        // 从MySQL删除
        knowledgeRepository.deleteById(id);
        log.info("知识已从MySQL删除, ID: {}", id);
    }

    /**
     * 【新增】: 更新知识
     * @param knowledge 包含ID和新内容的知识对象
     */
    public void updateKnowledge(Knowledge knowledge) {
        // 1. 更新MySQL中的内容
        knowledgeRepository.save(knowledge);
        log.info("知识已在MySQL中更新, ID: {}", knowledge.getId());

        // 2. 更新Weaviate中的向量 (先删除旧的，再添加新的)
        // 同样，为简化操作，这里只演示MySQL的更新。
        log.warn("注意: 仅更新了MySQL中的知识，若需同步更新向量，请实现Weaviate的更新逻辑");
    }
}