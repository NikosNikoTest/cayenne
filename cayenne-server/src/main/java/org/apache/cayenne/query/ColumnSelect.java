/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.Property;
import org.apache.cayenne.map.EntityResolver;

/**
 * <p>A selecting query providing individual properties based on the root object.</p>
 * <p>
 *     It can be properties of the object itself, properties of related entities
 *     or some function calls (including aggregate functions).
 * </p><p>
 * Usage examples: <pre>
 *      // select list of names:
 *      List&lt;String&gt; names = ColumnSelect.query(Artist.class, Artist.ARTIST_NAME).select(context);
 *
 *      // select count:
 *      Property<Long> countProperty = Property.create(FunctionExpressionFactory.countExp(), Long.class);
 *      long count = ColumnSelect.query(Artist.class, countProperty).selectOne();
 *
 *      // select only required properties of an entity:
 *      List&lt;Object[]&gt; data = ColumnSelect.query(Artist.class, Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH)
 *                                  .where(Artist.ARTIST_NAME.like("Picasso%))
 *                                  .select(context);
 * </pre></p>
 * @since 4.0
 */
public class ColumnSelect<T> extends FluentSelect<T, ColumnSelect<T>> {

    private Collection<Property<?>> columns;
    private boolean havingExpressionIsActive = false;
    private boolean singleColumn = true;
    private Expression having;

    /**
     *
     * @param entityType base persistent class that will be used as a root for this query
     */
    public static <T> ColumnSelect<T> query(Class<T> entityType) {
        return new ColumnSelect<T>().entityType(entityType);
    }

    /**
     *
     * @param entityType base persistent class that will be used as a root for this query
     * @param column single column to select
     */
    public static <E> ColumnSelect<E> query(Class<?> entityType, Property<E> column) {
        return new ColumnSelect<>().entityType(entityType).column(column);
    }

    /**
     *
     * @param entityType base persistent class that will be used as a root for this query
     * @param firstColumn column to select
     * @param otherColumns columns to select
     */
    public static ColumnSelect<Object[]> query(Class<?> entityType, Property<?> firstColumn, Property<?>... otherColumns) {
        return new ColumnSelect<Object[]>().entityType(entityType).columns(firstColumn, otherColumns);
    }

    protected ColumnSelect() {
        super();
    }

    /**
     * Copy constructor to convert ObjectSelect to ColumnSelect
     */
    protected ColumnSelect(ObjectSelect<T> select) {
        super();
        this.entityType = select.entityType;
        this.entityName = select.entityName;
        this.dbEntityName = select.dbEntityName;
        this.where = select.where;
        this.orderings = select.orderings;
        this.prefetches = select.prefetches;
        this.limit = select.limit;
        this.offset = select.offset;
        this.pageSize = select.pageSize;
        this.statementFetchSize = select.statementFetchSize;
        this.cacheStrategy = select.cacheStrategy;
        this.cacheGroups = select.cacheGroups;
    }

    @Override
    protected Query createReplacementQuery(EntityResolver resolver) {
        SelectQuery<?> replacement = (SelectQuery)super.createReplacementQuery(resolver);
        replacement.setColumns(columns);
        replacement.setHavingQualifier(having);
        replacement.setCanReturnScalarValue(singleColumn);
        return replacement;
    }

    /**
     * <p>Select only specific properties.</p>
     * <p>Can be any properties that can be resolved against root entity type
     * (root entity properties, function call expressions, properties of relationships, etc).</p>
     * <p>
     * <pre>
     * List&lt;Object[]&gt; columns = ColumnSelect.query(Artist.class)
     *                                    .columns(Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH)
     *                                    .select(context);
     * </pre>
     *
     * @param firstProperty first property
     * @param otherProperties array of properties to select
     * @see ColumnSelect#column(Property)
     * @see ColumnSelect#columns(Collection)
     */
    @SuppressWarnings("unchecked")
    public ColumnSelect<Object[]> columns(Property<?> firstProperty, Property<?>... otherProperties) {
        if (columns == null) {
            columns = new ArrayList<>(otherProperties.length + 1);
        }
        columns.add(firstProperty);
        Collections.addAll(columns, otherProperties);
        singleColumn = false;
        return (ColumnSelect<Object[]>)this;
    }

    /**
     * <p>Select only specific properties.</p>
     * <p>Can be any properties that can be resolved against root entity type
     * (root entity properties, function call expressions, properties of relationships, etc).</p>
     * <p>
     * @param properties collection of properties, <b>must</b> contain at least one element
     * @see ColumnSelect#columns(Property, Property[])
     */
    @SuppressWarnings("unchecked")
    public ColumnSelect<Object[]> columns(Collection<Property<?>> properties) {
        if (properties == null){
            throw new NullPointerException("properties is null");
        }
        if (properties.isEmpty()) {
            throw new IllegalArgumentException("properties must contain at least one element");
        }

        if (this.columns == null) {
            this.columns = new ArrayList<>(properties.size());
        }

        columns.addAll(properties);
        singleColumn = false;
        return (ColumnSelect<Object[]>)this;
    }

    /**
     * <p>Select one specific property.</p>
     * <p>Can be any property that can be resolved against root entity type
     * (root entity property, function call expression, property of relationships, etc)</p>
     * <p>If you need several columns use {@link ColumnSelect#columns(Property, Property[])} method as subsequent
     * call to this method will override previous columns set via this or
     * {@link ColumnSelect#columns(Property, Property[])} method.</p>
     * <p>
     * <pre>
     * List&lt;String&gt; names = ColumnSelect.query(Artist.class, Artist.ARTIST_NAME).select(context);
     * </pre>
     *
     * @param property single property to select
     * @see ColumnSelect#columns(Property, Property[])
     */
    @SuppressWarnings("unchecked")
    protected  <E> ColumnSelect<E> column(Property<E> property) {
        if (this.columns == null) {
            this.columns = new ArrayList<>(1);
        } else {
            this.columns.clear(); // if we don't clear then return type will be incorrect
        }
        this.columns.add(property);
        return (ColumnSelect<E>) this;
    }

    /**
     * Appends a having qualifier expression of this query. An equivalent to
     * {@link #and(Expression...)} that can be used a syntactic sugar.
     *
     * @return this object
     */
    public ColumnSelect<T> having(Expression expression) {
        havingExpressionIsActive = true;
        return and(expression);
    }

    /**
     * Appends a having qualifier expression of this query, using provided expression
     * String and an array of position parameters. This is an equivalent to
     * calling "and".
     *
     * @return this object
     */
    public ColumnSelect<T> having(String expressionString, Object... parameters) {
        havingExpressionIsActive = true;
        return and(ExpressionFactory.exp(expressionString, parameters));
    }

    /**
     * AND's provided expressions to the existing WHERE or HAVING clause expression.
     *
     * @return this object
     */
    @SuppressWarnings("unchecked")
    public ColumnSelect<T> and(Collection<Expression> expressions) {

        if (expressions == null || expressions.isEmpty()) {
            return this;
        }

        Collection<Expression> all;
        Expression activeExpression = getActiveExpression();

        if (activeExpression != null) {
            all = new ArrayList<>(expressions.size() + 1);
            all.add(activeExpression);
            all.addAll(expressions);
        } else {
            all = expressions;
        }

        setActiveExpression(ExpressionFactory.and(all));
        return this;
    }

    /**
     * OR's provided expressions to the existing WHERE or HAVING clause expression.
     *
     * @return this object
     */
    @SuppressWarnings("unchecked")
    public ColumnSelect<T> or(Collection<Expression> expressions) {
        if (expressions == null || expressions.isEmpty()) {
            return this;
        }

        Collection<Expression> all;
        Expression activeExpression = getActiveExpression();

        if (activeExpression != null) {
            all = new ArrayList<>(expressions.size() + 1);
            all.add(activeExpression);
            all.addAll(expressions);
        } else {
            all = expressions;
        }

        setActiveExpression(ExpressionFactory.or(all));
        return this;
    }

    private void setActiveExpression(Expression exp) {
        if(havingExpressionIsActive) {
            having = exp;
        } else {
            where = exp;
        }
    }

    private Expression getActiveExpression() {
        if(havingExpressionIsActive) {
            return having;
        } else {
            return where;
        }
    }

    public Collection<Property<?>> getColumns() {
        return columns;
    }

    /**
     * Returns a HAVING clause Expression of this query.
     */
    public Expression getHaving() {
        return having;
    }
}
