/*
 * Copyright 2011-2016 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.index.mapping;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Maps.newHashMap;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * @since 4.7
 */
public final class Mappings {

	private final Map<Class<?>, DocumentMapping> mappingsByType = newHashMap(); 
	
	public Mappings(Class<?>...types) {
		this(ImmutableSet.copyOf(types));
	}
	
	public Mappings(Collection<Class<?>> types) {
		checkArgument(!types.isEmpty(), "At least one document type should be specified");
		for (Class<?> type : ImmutableSet.copyOf(types)) {
			// XXX register only root mappings, nested mappings should be looked up via the parent/ancestor mapping
			mappingsByType.put(type, new DocumentMapping(type));
		}
	}
	
	public Collection<DocumentMapping> getMappings() {
		return ImmutableList.copyOf(mappingsByType.values());
	}
	
	public DocumentMapping getMapping(Class<?> type) {
		checkArgument(mappingsByType.containsKey(type), "Dynamic mapping is not supported: %s", type);
		return mappingsByType.get(type);
	}
	
	public DocumentMapping getByType(String className) {
		final Collection<DocumentMapping> mappings = Lists.newArrayList();
		for (DocumentMapping mapping : mappingsByType.values()) {
			if (mapping.type().getName().equals(className)) {
				mappings.add(mapping);
			}
		}
		return Iterables.getOnlyElement(mappings);
	}

}