/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

package org.alfresco.solr.query;

import java.io.IOException;

import org.alfresco.repo.search.adaptor.QueryConstants;
import org.alfresco.solr.cache.CacheConstants;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.Weight;
import org.apache.lucene.util.FixedBitSet;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.BitDocSet;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.SolrIndexSearcher;

public class SolrDenySetScorer extends AbstractSolrCachingScorer
{

    SolrDenySetScorer(Weight weight, DocSet in, LeafReaderContext context, SolrIndexSearcher searcher)
    {
        super(weight, in, context, searcher);
    }

    public static SolrDenySetScorer createDenySetScorer(Weight weight, LeafReaderContext context, SolrIndexSearcher searcher, String authorities, LeafReader reader) throws IOException
    {
        DocSet deniedDocSet = (DocSet) searcher.cacheLookup(CacheConstants.ALFRESCO_DENIED_CACHE, authorities);

        if (deniedDocSet == null)
        {

            String[] auths = authorities.substring(1).split(authorities.substring(0, 1));

            deniedDocSet = new BitDocSet(new FixedBitSet(searcher.maxDoc()));

            BooleanQuery.Builder bQuery = new BooleanQuery.Builder();
            for(String current : auths)
            {
                bQuery.add(new TermQuery(new Term(QueryConstants.FIELD_DENIED, current)), Occur.SHOULD);
            }

            DocSet aclDocs = searcher.getDocSet(bQuery.build());
            
            BooleanQuery.Builder aQuery = new BooleanQuery.Builder();
            for (DocIterator it = aclDocs.iterator(); it.hasNext(); /**/)
            {
                int docID = it.nextDoc();
                // Obtain the ACL ID for this ACL doc.
                long aclID = searcher.getSlowAtomicReader().getNumericDocValues(QueryConstants.FIELD_ACLID).get(docID);
                SchemaField schemaField = searcher.getSchema().getField(QueryConstants.FIELD_ACLID);
                Query query = schemaField.getType().getFieldQuery(null, schemaField, Long.toString(aclID));
                aQuery.add(query,  Occur.SHOULD);
                
                if((aQuery.build().clauses().size() > 999) || !it.hasNext())
                {
                    DocSet docsForAclId = searcher.getDocSet(aQuery.build());                
                    deniedDocSet = deniedDocSet.union(docsForAclId);
                       
                    aQuery = new BooleanQuery.Builder();
                }
            }
          
            
            // Exclude the ACL docs from the results, we only want real docs that match.
            // Probably not very efficient, what we really want is remove(docID)
            deniedDocSet = deniedDocSet.andNot(aclDocs);
            searcher.cacheInsert(CacheConstants.ALFRESCO_DENIED_CACHE, authorities, deniedDocSet);
        }
        
        // TODO: cache the full set? e.g. searcher.cacheInsert(CacheConstants.ALFRESCO_READERSET_CACHE, authorities, readableDocSet)
        // plus check of course, for presence in cache at start of method.
        return new SolrDenySetScorer(weight, deniedDocSet, context, searcher);
        
        
        
    }

}
