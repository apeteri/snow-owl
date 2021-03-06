/*
 * Copyright 2019 B2i Healthcare Pte Ltd, http://b2i.sg
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.b2international.snowowl.snomed.core.ql;

import com.b2international.index.query.Expression;
import com.b2international.snowowl.core.domain.BranchContext;
import com.b2international.snowowl.core.events.util.Promise;
import com.b2international.snowowl.snomed.datastore.request.SnomedRequests;

/**
 * @since 6.12
 */
public final class SnomedQueryExpression {

	private final String query;
	
	private Promise<Expression> expressionPromise;

	private SnomedQueryExpression(String ql) {
		this.query = ql.trim();
	}
	
	public String getQl() {
		return query;
	}
	
	public Promise<Expression> resolveToExpression(final BranchContext context) {
		if (expressionPromise == null) {
			expressionPromise = SnomedRequests.prepareQueryEvaluation(query)
					.build()
					.execute(context);
		}
		return expressionPromise;
	}
	
	public static SnomedQueryExpression of(String ql) {
		return new SnomedQueryExpression(ql);
	}
	
}
