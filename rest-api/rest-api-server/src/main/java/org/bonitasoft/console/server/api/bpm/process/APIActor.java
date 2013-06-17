/**
 * Copyright (C) 2011 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.console.server.api.bpm.process;

import static org.bonitasoft.web.rest.api.model.bpm.process.ActorItem.ATTRIBUTE_PROCESS_ID;
import static org.bonitasoft.web.toolkit.client.data.item.template.ItemHasDualName.ATTRIBUTE_NAME;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bonitasoft.console.server.api.ConsoleAPI;
import org.bonitasoft.console.server.datastore.bpm.process.ActorDatastore;
import org.bonitasoft.console.server.datastore.bpm.process.ProcessDatastore;
import org.bonitasoft.engine.bpm.actor.ActorCriterion;
import org.bonitasoft.web.rest.api.model.bpm.process.ActorDefinition;
import org.bonitasoft.web.rest.api.model.bpm.process.ActorItem;
import org.bonitasoft.web.toolkit.client.common.exception.api.APIFilterMandatoryException;
import org.bonitasoft.web.toolkit.client.common.util.MapUtil;
import org.bonitasoft.web.toolkit.client.data.item.ItemDefinition;
import org.bonitasoft.web.toolkit.server.api.APIHasGet;
import org.bonitasoft.web.toolkit.server.api.APIHasSearch;
import org.bonitasoft.web.toolkit.server.api.APIHasUpdate;
import org.bonitasoft.web.toolkit.server.api.Datastore;
import org.bonitasoft.web.toolkit.server.search.ItemSearchResult;

/**
 * @author Julien Mege
 * @author Séverin Moussel
 * 
 */
public class APIActor extends ConsoleAPI<ActorItem> implements
        APIHasGet<ActorItem>,
        APIHasSearch<ActorItem>,
        APIHasUpdate<ActorItem>
{

    @Override
    protected ItemDefinition defineItemDefinition() {
        return ActorDefinition.get();
    }

    @Override
    protected Datastore defineDefaultDatastore() {
        return new ActorDatastore(getEngineSession());
    }

    @Override
    protected List<String> defineReadOnlyAttributes() {
        return Arrays.asList(ATTRIBUTE_PROCESS_ID, ATTRIBUTE_NAME);
    }

    @Override
    public String defineDefaultSearchOrder() {
        return ActorCriterion.NAME_ASC.name();
    }

    @Override
    public ItemSearchResult<ActorItem> search(final int page, final int resultsByPage, final String search, final String orders,
            final Map<String, String> filters) {

        // Process id is a mandatory filter
        if (MapUtil.isBlank(filters, ActorItem.ATTRIBUTE_PROCESS_ID)) {
            throw new APIFilterMandatoryException(ActorItem.ATTRIBUTE_PROCESS_ID);
        }

        return super.search(page, resultsByPage, search, orders, filters);
    }

    @Override
    protected void fillDeploys(final ActorItem item, final List<String> deploys) {
        if (isDeployable(ATTRIBUTE_PROCESS_ID, deploys, item)) {
            item.setDeploy(ATTRIBUTE_PROCESS_ID, new ProcessDatastore(getEngineSession()).get(item.getProcessId()));
        }
    }

    @Override
    protected void fillCounters(final ActorItem item, final List<String> counters) {
        final ActorDatastore actorDatastore = (ActorDatastore) getDefaultDatastore();

        if (counters.contains(ActorItem.COUNTER_USERS)) {
            item.setAttribute(ActorItem.COUNTER_USERS, actorDatastore.countUsers(item.getId()));
        }
        if (counters.contains(ActorItem.COUNTER_GROUPS)) {
            item.setAttribute(ActorItem.COUNTER_GROUPS, actorDatastore.countGroups(item.getId()));
        }
        if (counters.contains(ActorItem.COUNTER_ROLES)) {
            item.setAttribute(ActorItem.COUNTER_ROLES, actorDatastore.countRoles(item.getId()));
        }
        if (counters.contains(ActorItem.COUNTER_MEMBERSHIPS)) {
            item.setAttribute(ActorItem.COUNTER_MEMBERSHIPS, actorDatastore.countMemberships(item.getId()));
        }

    }

}
