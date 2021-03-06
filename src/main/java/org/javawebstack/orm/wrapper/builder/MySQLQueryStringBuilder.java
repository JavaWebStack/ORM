package org.javawebstack.orm.wrapper.builder;

import org.javawebstack.orm.Repo;
import org.javawebstack.orm.SQLMapper;
import org.javawebstack.orm.TableInfo;
import org.javawebstack.orm.query.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MySQLQueryStringBuilder implements QueryStringBuilder {

    public static final MySQLQueryStringBuilder INSTANCE = new MySQLQueryStringBuilder();

    public SQLQueryString buildInsert(TableInfo info, Map<String, Object> values) {
        List<Object> params = new ArrayList<>();
        StringBuilder sb = new StringBuilder("INSERT INTO `");
        sb.append(info.getTableName());
        sb.append("` (");
        List<String> cols = new ArrayList<>();
        List<String> vals = new ArrayList<>();
        for (Map.Entry<String, Object> columnValueMapping : values.entrySet()) {
            cols.add("`" + columnValueMapping.getKey() + "`");
            vals.add("?");
            params.add(columnValueMapping.getValue());
        }
        sb.append(String.join(",", cols));
        sb.append(") VALUES (");
        sb.append(String.join(",", vals));
        sb.append(");");
        return new SQLQueryString(sb.toString(), params);
    }

    public SQLQueryString buildQuery(Query<?> query, boolean count) {
        Repo<?> repo = query.getRepo();
        List<Object> parameters = new ArrayList<>();
        StringBuilder sb = new StringBuilder("SELECT ")
                .append(count ? "COUNT(*)" : "*")
                .append(" FROM `")
                .append(repo.getInfo().getTableName())
                .append('`');
        QueryGroup<?> where = query.getWhereGroup();
        checkWithDeleted(repo, query.isWithDeleted(), where);
        if (!where.getQueryElements().isEmpty()) {
            SQLQueryString qs = convertGroup(repo.getInfo(), where);
            sb.append(" WHERE ").append(qs.getQuery());
            parameters.addAll(qs.getParameters());
        }

        QueryOrderBy orderBy = query.getOrder();
        if (!orderBy.isEmpty()) {
            sb.append(" ORDER BY ")
                .append(orderBy.toString(repo.getInfo()));
        }

        Integer offset = query.getOffset();
        Integer limit = query.getLimit();
        if (offset != null && limit == null)
            limit = Integer.MAX_VALUE;
        if (limit != null) {
            sb.append(" LIMIT ?");
            if (offset != null) {
                sb.append(",?");
                parameters.add(offset);
            }
            parameters.add(limit);
        }
        return new SQLQueryString(sb.toString(), SQLMapper.mapParams(repo, parameters));
    }

    public SQLQueryString buildUpdate(Query<?> query, Map<String, Object> values) {
        Repo<?> repo = query.getRepo();
        if (repo.getInfo().hasUpdated())
            values.put(repo.getInfo().getColumnName(repo.getInfo().getUpdatedField()), Timestamp.from(Instant.now()));
        List<Object> parameters = new ArrayList<>();
        List<String> sets = new ArrayList<>();
        values.forEach((key, value) -> {
            sets.add("`" + key + "`=?");
            parameters.add(value);
        });
        StringBuilder sb = new StringBuilder("UPDATE `")
                .append(repo.getInfo().getTableName())
                .append("` SET ")
                .append(String.join(",", sets));
        QueryGroup<?> where = query.getWhereGroup();
        checkWithDeleted(repo, query.isWithDeleted(), where);
        if (!where.getQueryElements().isEmpty()) {
            SQLQueryString qs = convertGroup(repo.getInfo(), where);
            sb.append(" WHERE ").append(qs.getQuery());
            parameters.addAll(qs.getParameters());
        }
        sb.append(';');
        return new SQLQueryString(sb.toString(), SQLMapper.mapParams(repo, parameters).toArray());
    }

    public SQLQueryString buildDelete(Query<?> query) {
        Repo<?> repo = query.getRepo();
        QueryGroup<?> where = query.getWhereGroup();
        List<Object> parameters = new ArrayList<>();
        StringBuilder sb = new StringBuilder("DELETE FROM `")
                .append(repo.getInfo().getTableName())
                .append('`');
        if (!where.getQueryElements().isEmpty()) {
            SQLQueryString qs = convertGroup(repo.getInfo(), where);
            sb.append(" WHERE ").append(qs.getQuery());
            parameters = qs.getParameters();
        }
        return new SQLQueryString(sb.toString(), SQLMapper.mapParams(repo, parameters));
    }

    private void checkWithDeleted(Repo<?> repo, boolean withDeleted, QueryGroup<?> where) {
        if (repo.getInfo().isSoftDelete() && !withDeleted) {
            if (!where.getQueryElements().isEmpty())
                where.getQueryElements().add(0, QueryConjunction.AND);
            where.getQueryElements().add(0, new QueryCondition(new QueryColumn(repo.getInfo().getColumnName(repo.getInfo().getSoftDeleteField())), "IS NULL", null));
        }
    }

    private SQLQueryString convertElement(TableInfo info, QueryElement element) {
        if(element instanceof QueryCondition)
            return convertCondition(info, (QueryCondition) element);
        if(element instanceof QueryConjunction)
            return new SQLQueryString(((QueryConjunction) element).name());
        if(element instanceof QueryExists) {
            QueryExists<?> queryExists = (QueryExists<?>) element;
            SQLQueryString qs = buildQuery(queryExists.getQuery(), false);
            return new SQLQueryString((queryExists.isNot() ? "NOT " : "") + "EXISTS (" + qs.getQuery() + ")", qs.getParameters());
        }
        if(element instanceof QueryGroup)
            return convertGroup(info, (QueryGroup<?>) element);
        return null;
    }

    private SQLQueryString convertGroup(TableInfo info, QueryGroup<?> group) {
        StringBuilder sb = new StringBuilder("(");
        List<Object> parameters = new ArrayList<>();
        for (QueryElement element : group.getQueryElements()) {
            if (sb.length() > 1)
                sb.append(' ');
            SQLQueryString s = convertElement(info, element);
            sb.append(s.getQuery());
            parameters.addAll(s.getParameters());
        }
        sb.append(')');
        return new SQLQueryString(sb.toString(), parameters);
    }

    private SQLQueryString convertCondition(TableInfo info, QueryCondition condition) {
        StringBuilder sb = new StringBuilder();
        if (condition.isNot())
            sb.append("NOT ");
        List<Object> parameters = new ArrayList<>();
        if (condition.getLeft() instanceof QueryColumn) {
            sb.append(((QueryColumn) condition.getLeft()).toString(info));
        } else {
            sb.append('?');
            parameters.add(condition.getLeft());
        }
        sb.append(' ');
        sb.append(condition.getOperator());
        if (condition.hasRight()) {
            sb.append(' ');
            if (condition.getOperator().endsWith("IN")) {
                Object[] values = (Object[]) condition.getRight();
                sb.append("(").append(IntStream.range(0, values.length).mapToObj(i -> "?").collect(Collectors.joining(","))).append(")");
                parameters.addAll(Arrays.asList(values));
            } else if (condition.getRight() instanceof QueryColumn) {
                sb.append(((QueryColumn) condition.getRight()).toString(info));
            } else {
                sb.append('?');
                parameters.add(condition.getRight());
            }
        }
        return new SQLQueryString(sb.toString(), parameters);
    }

}
