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
import io.weaviate.client.v1.schema.model.DataType;
import io.weaviate.client.v1.schema.model.Property;
import io.weaviate.client.v1.schema.model.WeaviateClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;

// 【修复点1】：显式导入所有必要的 Java Util 工具类，避免 IDE 识别不到
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// 【修复点2】：兼容大部分老版 Spring Boot，使用 javax 代替 jakarta
import javax.annotation.PostConstruct;

@Service
@Slf4j
public class KnowledgeService {

    private final WeaviateClient weaviateClient;
    private final OpenAiService openAiService;
    private final KnowledgeRepository knowledgeRepository;

    // 全新架构：双轨物理索引
    private static final String FACTS_CLASS = "Medical_Facts";
    private static final String COT_CLASS = "Diagnostic_CoT";

    @Value("${ai.ollama-embedding.model:bge-m3:latest}")
    private String embeddingModel;

    public KnowledgeService(WeaviateClient weaviateClient,
                            @Qualifier("embeddingOpenAiService") OpenAiService openAiService,
                            KnowledgeRepository knowledgeRepository) {
        this.weaviateClient = weaviateClient;
        this.openAiService = openAiService;
        this.knowledgeRepository = knowledgeRepository;
    }

    /**
     * 系统启动时：自动初始化双轨 Schema
     */
    @PostConstruct
    public void initSchema() {
        createClass(FACTS_CLASS, "存储经过清洗的客观医学事实");
        createClass(COT_CLASS, "存储细粒度意图对应的逻辑推理模板与专家思维链对");
    }

    private void createClass(String className, String desc) {
        try {
            Result<Boolean> exists = weaviateClient.schema().exists().withClassName(className).run();
            if (exists.getResult() != null && exists.getResult()) return;

            WeaviateClass clazz = WeaviateClass.builder()
                    .className(className)
                    .description(desc)
                    .vectorizer("none")
                    .properties(Arrays.asList(
                            Property.builder().name("content").dataType(Arrays.asList(DataType.TEXT)).build(),
                            Property.builder().name("category").dataType(Arrays.asList(DataType.TEXT)).build(),
                            Property.builder().name("mysqlId").dataType(Arrays.asList(DataType.TEXT)).build()
                    )).build();
            weaviateClient.schema().classCreator().withClass(clazz).run();
            log.info("+++ 成功创建 Weaviate 索引: {}", className);
        } catch (Exception e) {
            log.error("初始化 Schema 失败: {}", className, e);
        }
    }

    /**
     * 智能路由工具：判断知识该进哪个库
     */
    private String getTargetClass(String category) {
        if (category != null && (category.contains("思维链") || category.contains("推理") || category.contains("逻辑"))) {
            return COT_CLASS;
        }
        return FACTS_CLASS;
    }

    /**
     * 新增知识 (双写同步)
     */
    public void addKnowledge(Knowledge knowledge) {
        Knowledge saved = knowledgeRepository.save(knowledge);
        List<Double> vector = getVector(knowledge.getContent());
        String targetClass = getTargetClass(knowledge.getCategory());

        Map<String, Object> props = new HashMap<>();
        props.put("content", knowledge.getContent());
        props.put("category", knowledge.getCategory() == null ? "默认分类" : knowledge.getCategory());
        props.put("mysqlId", String.valueOf(saved.getId()));

        weaviateClient.data().creator()
                .withClassName(targetClass)
                .withID(UUID.nameUUIDFromBytes(String.valueOf(saved.getId()).getBytes()).toString())
                .withProperties(props)
                .withVector(vector.stream().map(Double::floatValue).toArray(Float[]::new))
                .run();
        log.info("知识已成功双写同步: MySQL ID = [{}], Weaviate 库 = [{}]", saved.getId(), targetClass);
    }

    /**
     * 核心双维检索
     */
    public List<String> search(String query) {
        List<Double> queryVector = getVector(query);
        Float[] vector = queryVector.stream().map(Double::floatValue).toArray(Float[]::new);

        List<String> facts = searchFromClass(FACTS_CLASS, vector, 3);
        List<String> cots = searchFromClass(COT_CLASS, vector, 2);

        List<String> combinedResults = new ArrayList<>();
        combinedResults.addAll(facts);
        combinedResults.addAll(cots);

        log.info("【双轨检索完成】: 共召回有效知识 {} 条", combinedResults.size());
        return combinedResults;
    }

    @SuppressWarnings("unchecked")
    private List<String> searchFromClass(String className, Float[] vector, int limit) {
        Field content = Field.builder().name("content").build();
        Field _additional = Field.builder().name("_additional").fields(Field.builder().name("distance").build()).build();

        Result<GraphQLResponse> result = weaviateClient.graphQL().get()
                .withClassName(className)
                .withFields(content, _additional)
                .withNearVector(NearVectorArgument.builder().vector(vector).build())
                .withLimit(limit)
                .run();

        // 【修复点3】：修改日志打印方法，提升对不同版本 Weaviate SDK 的兼容性
        if (result.hasErrors()) {
            log.error("Weaviate GraphQL 检索报错: {}", result.getError() != null ? result.getError().toString() : "未知错误");
            return Collections.emptyList();
        }

        if (result.getResult() == null || result.getResult().getData() == null) {
            return Collections.emptyList();
        }

        Map<String, Object> data = (Map<String, Object>) result.getResult().getData();
        if (data.get("Get") == null) return Collections.emptyList();

        List<Map<String, Object>> list = (List<Map<String, Object>>) ((Map<String, Object>) data.get("Get")).get(className);
        if (list == null) return Collections.emptyList();

        return list.stream()
                .filter(res -> {
                    Map<String, Object> additional = (Map<String, Object>) res.get("_additional");
                    // 兼容不同版本的返回类型
                    Double distance = additional.get("distance") instanceof Number ?
                            ((Number) additional.get("distance")).doubleValue() : null;
                    log.info("[{}] 检索到知识: {}, 距离: {}", className, res.get("content"), distance);
                    return distance != null && distance < 0.3;
                })
                .map(res -> (String) res.get("content"))
                .collect(Collectors.toList());
    }

    /**
     * 删除知识
     */
    public void deleteKnowledge(Long id) {
        Knowledge knowledge = knowledgeRepository.findById(id).orElse(null);
        if (knowledge != null) {
            String targetClass = getTargetClass(knowledge.getCategory());
            String weaviateId = UUID.nameUUIDFromBytes(String.valueOf(id).getBytes()).toString();

            weaviateClient.data().deleter()
                    .withClassName(targetClass)
                    .withID(weaviateId)
                    .run();
            log.info("已从 Weaviate [{}] 清除向量数据", targetClass);
        }

        knowledgeRepository.deleteById(id);
        log.info("知识已从MySQL删除, ID: {}", id);
    }

    /**
     * 更新知识
     */
    public void updateKnowledge(Knowledge knowledge) {
        if (knowledge.getId() != null) {
            deleteKnowledge(knowledge.getId());
        }
        addKnowledge(knowledge);
        log.info("知识更新完成，ID: {}", knowledge.getId());
    }

    /**
     * 获取向量
     */
    private List<Double> getVector(String text) {
        EmbeddingRequest request = EmbeddingRequest.builder()
                .model(embeddingModel)
                .input(Collections.singletonList(text))
                .build();
        return openAiService.createEmbeddings(request).getData().get(0).getEmbedding();
    }
}