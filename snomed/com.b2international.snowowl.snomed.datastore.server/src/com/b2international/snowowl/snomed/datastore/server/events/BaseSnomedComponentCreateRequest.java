/*
 * Copyright 2011-2015 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.snomed.datastore.server.events;

import javax.validation.constraints.NotNull;

import com.b2international.snowowl.core.domain.TransactionContext;
import com.b2international.snowowl.core.events.BaseRequest;
import com.b2international.snowowl.snomed.core.domain.IdGenerationStrategy;

/**
 * @since 4.0
 */
public abstract class BaseSnomedComponentCreateRequest<B> extends BaseRequest<TransactionContext, B> implements SnomedComponentCreateRequest {

	@NotNull
	private IdGenerationStrategy idGenerationStrategy;
	private String moduleId;

	@Override
	public IdGenerationStrategy getIdGenerationStrategy() {
		return idGenerationStrategy;
	}

	@Override
	public String getModuleId() {
		return moduleId;
	}

	public void setIdGenerationStrategy(final IdGenerationStrategy idGenerationStrategy) {
		this.idGenerationStrategy = idGenerationStrategy;
	}

	public void setModuleId(final String moduleId) {
		this.moduleId = moduleId;
	}
	
	@Override
	protected String getAddress() {
		throw new UnsupportedOperationException();
	}
	
}