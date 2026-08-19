package com.lulala.langchain4j.toolspecification.service;

import java.util.List;

/**
 * @author shenjh
 * @version 1.0
 * @since 2026/8/19 16:21
 */
public interface IBaiduSearchService {
    
    /**
     * 根据给定的查询条件，在百度上搜索相关的 URL
     * @param query
     * @return java.util.List<java.lang.String> 
     * @author shenjh
     * @since 2026/8/19 16:33
     */
    List<String> search(String query);
}
