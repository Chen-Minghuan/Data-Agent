package edu.zsc.ai.agent.tool.sql.model;

import edu.zsc.ai.plugin.model.metadata.IndexMetadata;

import java.util.List;

/**
 * Combined object detail: DDL + row count + indexes.
 * For VIEW: rowCount is present, indexes is empty.
 * For FUNCTION/PROCEDURE/TRIGGER: only ddl is present.
 */
public record ObjectDetail(String ddl, Long rowCount, List<IndexMetadata> indexes) {}
