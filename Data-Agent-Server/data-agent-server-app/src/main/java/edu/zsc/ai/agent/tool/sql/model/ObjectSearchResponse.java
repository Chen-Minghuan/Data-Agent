package edu.zsc.ai.agent.tool.sql.model;

import java.util.List;

/**
 * Search result wrapper with truncation info.
 */
public record ObjectSearchResponse(List<ObjectSearchResult> results, int totalCount, boolean truncated) {}
