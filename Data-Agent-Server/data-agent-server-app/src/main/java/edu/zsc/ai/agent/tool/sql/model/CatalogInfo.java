package edu.zsc.ai.agent.tool.sql.model;

import java.util.List;

/**
 * Catalog (database) with its schema list.
 * For MySQL, schemas is empty. For PostgreSQL, schemas contains actual schema names.
 */
public record CatalogInfo(String name, List<String> schemas) {}
