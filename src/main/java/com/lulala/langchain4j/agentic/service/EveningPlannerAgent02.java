package com.lulala.langchain4j.agentic.service;

import com.lulala.langchain4j.agentic.domain.EveningPlan;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.ParallelAgent;
import dev.langchain4j.agentic.declarative.ParallelExecutor;
import dev.langchain4j.service.V;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 声明式 API写法
 * @author shenjh
 * @version 1.0
 * @since 2026/6/15 10:26
 */
public interface EveningPlannerAgent02 {

    @ParallelAgent(outputKey = "plans", subAgents = {
            FoodExpert02.class,
            MovieExpert02.class
    }, description = "根据美食专家与电影专家推荐的餐点和电影，将它们组合成“浪漫之夜计划”")
    List<EveningPlan> plan(@V("mood") String mood);

    @ParallelExecutor
    static ExecutorService executor() {
        return Executors.newFixedThreadPool(2);
    }

    @Output
    static List<EveningPlan> createPlans(
            @V("movies") List<String> movies,
            @V("meals") List<String> meals
    ) {
        List<EveningPlan> movesAndMeals = new ArrayList<>();
        for (int i = 0; i < movies.size(); i++) {
            if (i >= meals.size()) {
                break;
            }
            movesAndMeals.add(new EveningPlan(movies.get(i), meals.get(i)));
        }
        return movesAndMeals;
    }
}
