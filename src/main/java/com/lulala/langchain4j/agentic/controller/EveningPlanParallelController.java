package com.lulala.langchain4j.agentic.controller;

import com.lulala.langchain4j.agentic.domain.EveningPlan;
import com.lulala.langchain4j.agentic.service.EveningPlannerAgent;
import com.lulala.langchain4j.agentic.service.EveningPlannerAgent02;
import com.lulala.langchain4j.agentic.service.FoodExpert;
import com.lulala.langchain4j.agentic.service.MovieExpert;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * 参见：https://langchain4j.cn/tutorials/agents.html<br/>
 * 浪漫之夜计划 - 并行工作流（Parallel workflow）
 * @author shenjh
 * @version 1.0
 * @since 2026/6/14 16:53
 */
@RestController
@RequestMapping("/eveningPlanParallel")
public class EveningPlanParallelController {

    @Autowired
    private ChatModel gptChatModel;

    @GetMapping("/eveningPlan")
    public List<EveningPlan> eveningPlan() {
        FoodExpert foodExpert = AgenticServices
                .agentBuilder(FoodExpert.class)
                .chatModel(gptChatModel)
                .outputKey("meals")
                .build();
        MovieExpert movieExpert = AgenticServices
                .agentBuilder(MovieExpert.class)
                .chatModel(gptChatModel)
                .outputKey("movies")
                .build();

        EveningPlannerAgent eveningPlannerAgent = AgenticServices
                .parallelBuilder(EveningPlannerAgent.class)
                .subAgents(foodExpert, movieExpert)
                .executor(Executors.newFixedThreadPool(2))
                .outputKey("plans")
                .output(agenticScope -> {
                    List<String> movies = agenticScope.readState("moves", List.of());
                    List<String> meals = agenticScope.readState("meals", List.of());
                    List<EveningPlan> movesAndMeals = new ArrayList<>();
                    for (int i = 0; i < movies.size(); i++) {
                        if (i >= meals.size()) {
                            break;
                        }
                        movesAndMeals.add(new EveningPlan(movies.get(i), meals.get(i)));
                    }
                    return movesAndMeals;
                })
                .build();

        List<EveningPlan> eveningPlanList = eveningPlannerAgent.plan("浪漫");
        return eveningPlanList;
    }

    @GetMapping("/eveningPlan02")
    public List<EveningPlan> eveningPlan02() {
        EveningPlannerAgent02 eveningPlannerAgent = AgenticServices
                .createAgenticSystem(EveningPlannerAgent02.class, gptChatModel);
        return eveningPlannerAgent.plan("浪漫");
    }
}
