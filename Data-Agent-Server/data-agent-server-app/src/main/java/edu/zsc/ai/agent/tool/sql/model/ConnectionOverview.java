package edu.zsc.ai.agent.tool.sql.model;

import java.util.List;

/**
 * Connection overview with nested catalog/schema hierarchy.
 */
public record ConnectionOverview(Long id, String name, String dbType, List<CatalogInfo> catalogs) {}
