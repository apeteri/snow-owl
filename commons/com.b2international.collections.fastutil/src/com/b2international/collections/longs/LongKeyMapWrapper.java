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
package com.b2international.collections.longs;

import java.util.Collection;

import com.b2international.collections.StrategyWrapper;
import com.b2international.collections.longs.LongIterator;
import com.b2international.collections.longs.LongKeyMap;
import com.b2international.collections.longs.LongSet;
import com.google.common.hash.HashFunction;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/**
 * @since 4.7
 */
public final class LongKeyMapWrapper<V> implements LongKeyMap<V> {

	private final Long2ObjectMap<V> delegate;

	LongKeyMapWrapper(Long2ObjectMap<V> delegate) {
		this.delegate = delegate;
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public void trimToSize() {
		trim(delegate);
	}

	@Override
	public boolean containsKey(long key) {
		return delegate.containsKey(key);
	}

	@Override
	public LongKeyMap<V> dup() {
		return create(this);
	}

	@Override
	public V get(long key) {
		return delegate.get(key);
	}

	@Override
	public LongSet keySet() {
		return LongOpenHashSetWrapper.wrap(delegate.keySet());
	}

	@Override
	public V put(long key, V value) {
		return delegate.put(key, value);
	}

	@Override
	public V remove(long key) {
		return delegate.remove(key);
	}

	@Override
	public Collection<V> values() {
		return delegate.values();
	}
	
	public static <V> LongKeyMap<V> create(LongKeyMap<V> map) {
		if (map instanceof LongKeyMapWrapper) {
			final Long2ObjectMap<V> sourceDelegate = ((LongKeyMapWrapper<V>) map).delegate;
			return wrap(clone(sourceDelegate));
		} else {
			final LongKeyMap<V> result = create(map.size());
			final LongIterator iter = map.keySet().iterator();
			while (iter.hasNext()) {
				final long key = iter.next();
				result.put(key, map.get(key));
			}
			return result;
		}
	}
	
	public static <V> LongKeyMap<V> create(HashFunction hashFunction) {
		return wrap(new Long2ObjectOpenCustomHashMap<V>(new StrategyWrapper(hashFunction)));
	}
	
	public static <V> LongKeyMap<V> create(int expectedSize) {
		return wrap(new Long2ObjectOpenHashMap<V>(expectedSize));
	}
	
	public static <V> LongKeyMap<V> create() {
		return wrap(new Long2ObjectOpenHashMap<V>());
	}
	
	public static <V> LongKeyMap<V> wrap(Long2ObjectMap<V> delegate) {
		return new LongKeyMapWrapper<>(delegate);
	}
	
	// Move to FastUtil helper methods
	private static <V> Long2ObjectMap<V> clone(Long2ObjectMap<V> sourceDelegate) {
		if (sourceDelegate instanceof Long2ObjectOpenHashMap) {
			return ((Long2ObjectOpenHashMap<V>) sourceDelegate).clone();
		} else if (sourceDelegate instanceof Long2ObjectOpenCustomHashMap) {
			return ((Long2ObjectOpenCustomHashMap<V>) sourceDelegate).clone();
		} else {
			throw new UnsupportedOperationException("Unsupported map implementation: " + sourceDelegate.getClass().getSimpleName());
		}
	}
	
	private static <V> void trim(Long2ObjectMap<V> map) {
		if (map instanceof Long2ObjectOpenHashMap) {
			((Long2ObjectOpenHashMap<V>) map).trim();
		} else if (map instanceof Long2ObjectOpenCustomHashMap) {
			((Long2ObjectOpenCustomHashMap<V>) map).trim();
		} else {
			throw new UnsupportedOperationException("Unsupported map implementation: " + map.getClass().getSimpleName());
		}
	}

}
