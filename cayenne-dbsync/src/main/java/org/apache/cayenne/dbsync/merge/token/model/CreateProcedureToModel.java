/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.dbsync.merge.token.model;

import org.apache.cayenne.dbsync.merge.context.MergerContext;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Procedure;

/**
 * @since 4.2
 */
public class CreateProcedureToModel extends AbstractToModelToken {

    private Procedure procedure;

    public CreateProcedureToModel(Procedure procedure) {
        super("Add procedure to model", 125);
        this.procedure = procedure;
    }

    @Override
    public String getTokenValue() {
        return procedure.getName();
    }

    @Override
    public MergerToken createReverse(MergerTokenFactory factory) {
        return factory.createDropProcedureToDb(procedure);
    }

    @Override
    public void execute(MergerContext context) {
        DataMap dataMap = context.getDataMap();
        dataMap.addProcedure(procedure);

        context.getDelegate().procedureAdded(procedure);
    }
}
