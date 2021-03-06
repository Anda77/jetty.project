//
// ========================================================================
// Copyright (c) 1995-2020 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under
// the terms of the Eclipse Public License 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0
//
// This Source Code may also be made available under the following
// Secondary Licenses when the conditions for such availability set
// forth in the Eclipse Public License, v. 2.0 are satisfied:
// the Apache License v2.0 which is available at
// https://www.apache.org/licenses/LICENSE-2.0
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.session.infinispan;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jetty.server.session.SessionData;
import org.infinispan.Cache;
import org.infinispan.query.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;

public class EmbeddedQueryManager implements QueryManager
{
    private Cache<String, SessionData> _cache;

    public EmbeddedQueryManager(Cache<String, SessionData> cache)
    {
        _cache = cache;
    }

    @Override
    public Set<String> queryExpiredSessions(long time)
    {
        QueryFactory qf = Search.getQueryFactory(_cache);
        Query q = qf.from(SessionData.class).select("id").having("expiry").lte(time).build();

        List<Object[]> list = q.list();
        Set<String> ids = new HashSet<>();
        for (Object[] sl : list)
        {
            ids.add((String)sl[0]);
        }
        return ids;
    }

    @Override
    public Set<String> queryExpiredSessions()
    {
        return queryExpiredSessions(System.currentTimeMillis());
    }
}
