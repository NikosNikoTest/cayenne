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

package org.apache.cayenne.modeler.dialog.db;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.validator.ValidationDisplayHandler;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.project2.Project;
import org.apache.cayenne.project2.validation.ValidationInfo;
import org.apache.cayenne.project2.validation.ValidationResults;
import org.apache.cayenne.project2.validation.ProjectValidator;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.swing.ObjectBinding;
import org.apache.cayenne.swing.TableBindingBuilder;

/**
 */
public class TableSelectorController extends CayenneController {

    protected TableSelectorView view;
    protected ObjectBinding tableBinding;

    protected DbEntity table;
    protected List<DbEntity> tables;
    protected int permanentlyExcludedCount;
    protected Map excludedTables;
    protected List<DbEntity> selectableTablesList;

    protected Map<String, String> validationMessages;

    public TableSelectorController(ProjectController parent) {
        super(parent);
        this.view = new TableSelectorView();
        this.excludedTables = new HashMap();
        this.selectableTablesList = new ArrayList();
        this.validationMessages = new HashMap<String, String>();
        initController();
    }

    // ----- properties -----

    public Component getView() {
        return view;
    }

    /**
     * Called by table binding script to set current table.
     */
    public void setTable(DbEntity table) {
        this.table = table;
    }

    /**
     * Returns DbEntities that are excluded from DB generation.
     */
    public Collection getExcludedTables() {
        return excludedTables.values();
    }

    public List getTables() {
        return tables;
    }

    public boolean isIncluded() {
        if (table == null) {
            return false;
        }

        return !excludedTables.containsKey(table.getName());
    }

    public void setIncluded(boolean b) {
        if (table == null) {
            return;
        }

        if (b) {
            excludedTables.remove(table.getName());
        }
        else {
            excludedTables.put(table.getName(), table);
        }

        tableSelectedAction();
    }

    /**
     * A callback action that updates the state of Select All checkbox.
     */
    public void tableSelectedAction() {
        int unselectedCount = excludedTables.size() - permanentlyExcludedCount;

        if (unselectedCount == selectableTablesList.size()) {
            view.getCheckAll().setSelected(false);
        }
        else if (unselectedCount == 0) {
            view.getCheckAll().setSelected(true);
        }
    }

    public Object getProblem() {
        return (table != null) ? validationMessages.get(table.getName()) : null;
    }

    // ------ other stuff ------

    protected void initController() {

        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);

        builder.bindToAction(view.getCheckAll(), "checkAllAction()");

        TableBindingBuilder tableBuilder = new TableBindingBuilder(builder);

        tableBuilder.addColumn(
                "",
                "setTable(#item), included",
                Boolean.class,
                true,
                Boolean.TRUE);
        tableBuilder.addColumn(
                "Table",
                "#item.name",
                String.class,
                false,
                "XXXXXXXXXXXXXXXX");
        tableBuilder.addColumn(
                "Problems",
                "setTable(#item), problem",
                String.class,
                false,
                "XXXXXXXXXXXXXXXXXXXXXXXXXXXXX");

        this.tableBinding = tableBuilder.bindToTable(view.getTables(), "tables");
    }

    /**
     * Performs validation of DbEntities in the current DataMap. Returns a collection of
     * ValidationInfo objects describing the problems.
     */
    public void updateTables(DataMap dataMap) {
        this.tables = new ArrayList(dataMap.getDbEntities());

        excludedTables.clear();
        validationMessages.clear();

        // if there were errors, filter out those related to
        // non-derived DbEntities...

        // TODO: this is inefficient.. we need targeted validation
        // instead of doing it on the whole project

        Project project = getApplication().getProject();

        ProjectValidator projectValidator = getApplication().getInjector().getInstance(
                ProjectValidator.class);
        ValidationResults validationResults = projectValidator.validate(project
                .getRootNode());

        int validationCode = validationResults.getMaxSeverity();

        if (validationCode >= ValidationDisplayHandler.WARNING) {

            for (ValidationInfo nextProblem : validationResults.getValidationResults()) {
                Entity failedEntity = null;

                if (nextProblem.getObject() instanceof DbAttribute) {
                    DbAttribute failedAttribute = (DbAttribute) nextProblem.getObject();
                    failedEntity = failedAttribute.getEntity();
                }
                else if (nextProblem.getObject() instanceof DbRelationship) {
                    DbRelationship failedRelationship = (DbRelationship) nextProblem
                            .getObject();
                    failedEntity = failedRelationship.getSourceEntity();
                }
                else if (nextProblem.getObject() instanceof DbEntity) {
                    failedEntity = (Entity) nextProblem.getObject();
                }

                if (failedEntity == null) {
                    continue;
                }

                excludedTables.put(failedEntity.getName(), failedEntity);
                validationMessages.put(failedEntity.getName(), nextProblem.getMessage());
            }
        }

        // Find selectable tables
        permanentlyExcludedCount = excludedTables.size();
        selectableTablesList.clear();
        for (DbEntity table : tables) {
            if (false == excludedTables.containsKey(table.getName())) {
                selectableTablesList.add(table);
            }
        }

        tableBinding.updateView();
        tableSelectedAction();
    }

    public void checkAllAction() {

        boolean isCheckAllSelected = view.getCheckAll().isSelected();

        if (isCheckAllSelected) {
            selectableTablesList.clear();
            selectableTablesList.addAll(tables);
            excludedTables.clear();
        }
        else {
            excludedTables.clear();
            for (DbEntity table : tables) {
                excludedTables.put(table.getName(), table);
            }
            selectableTablesList.clear();
        }

        tableBinding.updateView();
    }
}
