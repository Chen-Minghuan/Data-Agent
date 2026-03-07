package edu.zsc.ai.plugin.mysql.validator;

import edu.zsc.ai.plugin.model.sql.SqlType;
import edu.zsc.ai.plugin.mysql.parser.MySqlParser;
import edu.zsc.ai.plugin.mysql.parser.MySqlParserBaseVisitor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Visits the ANTLR parse tree to extract SqlType, referenced table names, and column names.
 */
public class MySqlAstExtractor extends MySqlParserBaseVisitor<Void> {

    @Getter
    private SqlType sqlType = SqlType.UNKNOWN;
    private final Set<String> tables = new LinkedHashSet<>();
    private final Set<String> columns = new LinkedHashSet<>();

    public List<String> getTables() {
        return new ArrayList<>(tables);
    }

    public List<String> getColumns() {
        return new ArrayList<>(columns);
    }

    // --- SQL Type detection from top-level statement alternatives ---

    @Override
    public Void visitDmlStatement(MySqlParser.DmlStatementContext ctx) {
        if (ctx.selectStatement() != null || ctx.withStatement() != null || ctx.tableStatement() != null) {
            sqlType = SqlType.SELECT;
        } else if (ctx.insertStatement() != null || ctx.replaceStatement() != null) {
            sqlType = SqlType.INSERT;
        } else if (ctx.updateStatement() != null) {
            sqlType = SqlType.UPDATE;
        } else if (ctx.deleteStatement() != null) {
            sqlType = SqlType.DELETE;
        }
        return visitChildren(ctx);
    }

    @Override
    public Void visitDdlStatement(MySqlParser.DdlStatementContext ctx) {
        String text = ctx.getStart().getText().toUpperCase();
        sqlType = switch (text) {
            case "CREATE" -> SqlType.CREATE;
            case "ALTER" -> SqlType.ALTER;
            case "DROP" -> SqlType.DROP;
            case "TRUNCATE" -> SqlType.TRUNCATE;
            case "RENAME" -> SqlType.ALTER;
            default -> SqlType.UNKNOWN;
        };
        return visitChildren(ctx);
    }

    @Override
    public Void visitTransactionStatement(MySqlParser.TransactionStatementContext ctx) {
        String text = ctx.getStart().getText().toUpperCase();
        sqlType = switch (text) {
            case "START", "BEGIN" -> SqlType.BEGIN;
            case "COMMIT" -> SqlType.COMMIT;
            case "ROLLBACK" -> SqlType.ROLLBACK;
            default -> SqlType.UNKNOWN;
        };
        return visitChildren(ctx);
    }

    @Override
    public Void visitAdministrationStatement(MySqlParser.AdministrationStatementContext ctx) {
        if (ctx.showStatement() != null) {
            sqlType = SqlType.SHOW;
        } else if (ctx.setStatement() != null) {
            sqlType = SqlType.SET;
        } else if (ctx.grantStatement() != null || ctx.grantProxy() != null) {
            sqlType = SqlType.GRANT;
        } else if (ctx.revokeStatement() != null || ctx.revokeProxy() != null) {
            sqlType = SqlType.REVOKE;
        }
        return visitChildren(ctx);
    }

    @Override
    public Void visitUtilityStatement(MySqlParser.UtilityStatementContext ctx) {
        if (ctx.simpleDescribeStatement() != null || ctx.fullDescribeStatement() != null) {
            sqlType = SqlType.DESCRIBE;
        } else if (ctx.useStatement() != null) {
            sqlType = SqlType.USE;
        }
        return visitChildren(ctx);
    }

    // --- Extract table names ---

    @Override
    public Void visitTableName(MySqlParser.TableNameContext ctx) {
        tables.add(ctx.getText());
        return visitChildren(ctx);
    }

    // --- Extract column names ---

    @Override
    public Void visitFullColumnName(MySqlParser.FullColumnNameContext ctx) {
        columns.add(ctx.getText());
        return visitChildren(ctx);
    }
}
