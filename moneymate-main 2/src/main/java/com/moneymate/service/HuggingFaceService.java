package com.moneymate.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class HuggingFaceService {

    @Value("${huggingface.api.key}")
    private String apiKey;

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public HuggingFaceService() {
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    public String generateSpendingStrategy(Map<String, Object> userData) {
        try {
            String prompt = buildPrompt(userData);
            
            // Hugging Face Inference API - 한글 모델 사용
            String modelUrl = "https://api-inference.huggingface.co/models/beomi/llama-2-ko-7b";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("inputs", prompt);
            requestBody.put("parameters", Map.of(
                "max_new_tokens", 500,
                "temperature", 0.7,
                "top_p", 0.9,
                "do_sample", true
            ));

            RequestBody body = RequestBody.create(
                objectMapper.writeValueAsString(requestBody),
                MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                .url(modelUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

            System.out.println("=== Hugging Face API 호출 ===");
            System.out.println("Model: " + modelUrl);

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("Hugging Face API 오류: " + response.code());
                    System.err.println("응답: " + response.body().string());
                    return getDefaultStrategy(userData);
                }

                String responseBody = response.body().string();
                System.out.println("=== API 응답 ===");
                System.out.println(responseBody.substring(0, Math.min(200, responseBody.length())));
                
                // 응답 파싱
                JsonNode root = objectMapper.readTree(responseBody);
                
                // 배열 형태로 반환됨
                if (root.isArray() && root.size() > 0) {
                    String generatedText = root.get(0).path("generated_text").asText();
                    // 프롬프트 제거하고 답변만 추출
                    if (generatedText.contains("답변:")) {
                        return generatedText.split("답변:")[1].trim();
                    }
                    return generatedText.replace(prompt, "").trim();
                }
                
                return getDefaultStrategy(userData);
            }

        } catch (Exception e) {
            System.err.println("Hugging Face API 호출 실패: " + e.getMessage());
            e.printStackTrace();
            return getDefaultStrategy(userData);
        }
    }

    private String buildPrompt(Map<String, Object> userData) {
        StringBuilder prompt = new StringBuilder();
        
        Integer totalSpent = (Integer) userData.get("totalSpent");
        Integer budget = (Integer) userData.get("budget");
        Map<String, Integer> categorySpending = (Map<String, Integer>) userData.get("categorySpending");

        if (totalSpent == null || totalSpent == 0) {
            prompt.append("사용자는 이번 달 아직 지출 내역이 없습니다.\n\n");
            prompt.append("질문: 지출 내역이 없는 사용자에게 가계부 관리 시작 방법을 3가지 조언해주세요.\n");
            prompt.append("답변:");
            return prompt.toString();
        }

        prompt.append("사용자의 이번 달 소비 분석:\n");
        prompt.append("- 총 예산: ").append(budget != null ? budget + "원" : "설정 안 됨").append("\n");
        prompt.append("- 총 지출: ").append(totalSpent).append("원\n");
        
        if (budget != null && budget > 0) {
            int ratio = (int) ((totalSpent * 100.0) / budget);
            prompt.append("- 예산 사용률: ").append(ratio).append("%\n");
        }

        prompt.append("\n카테고리별 지출:\n");
        if (categorySpending != null && !categorySpending.isEmpty()) {
            categorySpending.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .forEach(entry -> {
                    prompt.append("- ").append(entry.getKey())
                          .append(": ").append(entry.getValue()).append("원\n");
                });
        }

        prompt.append("\n질문: 위 소비 패턴을 분석하고, 실천 가능한 절약 전략 3-5가지를 제안해주세요. ");
        prompt.append("각 전략은 이모지(✅, 💡, ⚠️)로 시작하고 친근한 반말 톤으로 작성해주세요.\n");
        prompt.append("답변:");

        return prompt.toString();
    }

    private String getDefaultStrategy(Map<String, Object> userData) {
        Integer totalSpent = (Integer) userData.get("totalSpent");
        Integer budget = (Integer) userData.get("budget");
        
        StringBuilder strategy = new StringBuilder();
        
        if (totalSpent == null || totalSpent == 0) {
            strategy.append("✅ 가계부를 시작하셨네요! 지출을 꾸준히 기록해보세요.\n\n");
            strategy.append("💡 먼저 예산을 설정하면 더 체계적인 관리가 가능해요.\n\n");
            strategy.append("⚠️ 작은 지출도 모두 기록하는 습관을 들여보세요!");
            return strategy.toString();
        }

        if (budget != null && budget > 0) {
            int ratio = (int) ((totalSpent * 100.0) / budget);
            if (ratio > 100) {
                strategy.append("⚠️ 예산을 초과했어요! 남은 기간 지출을 줄여야 해요.\n\n");
            } else if (ratio > 80) {
                strategy.append("⚠️ 예산의 80% 이상 사용 중이에요. 신중하게 소비하세요.\n\n");
            } else {
                strategy.append("✅ 예산을 잘 지키고 있어요! 계속 이렇게 관리하세요.\n\n");
            }
        }

        strategy.append("💡 매일 지출을 기록하고 주간 단위로 리뷰해보세요.\n\n");
        strategy.append("✅ 충동구매를 줄이기 위해 24시간 규칙을 활용하세요!\n\n");
        strategy.append("⚠️ 고정 지출과 변동 지출을 구분해서 관리하면 좋아요.");

        return strategy.toString();
    }
}
