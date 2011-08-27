/*******************************************************************************
 * Copyright (c) 2010 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sourceforge.docfetcher.base.annotations.Immutable;
import net.sourceforge.docfetcher.base.annotations.NotNull;
import net.sourceforge.docfetcher.base.annotations.Nullable;

/**
 * An event class that provides a much simpler alternative to the Observer
 * pattern: Instead of adding methods like addListener, removeListener, etc. to
 * each class one wants to listen to, this class can simply be added as a field.
 * <p>
 * Moreover, the entire event system can be put into a 'caching mode' using the
 * static {@link #hold()} and {@link #flush()} methods, which is useful for
 * avoiding mass notification when a lot of changes are made to an observed
 * object. In caching mode, the event system may discard duplicate events,
 * depending on the return value that is chosen for
 * {@link Listener#getEventDataPolicy()}.
 * <p>
 * The additional type parameter T of this class specifies the type of the event
 * data object that is transmitted on notifications. If none is needed, the
 * observed object may return itself.
 * 
 * @author Tran Nam Quang
 */
public final class Event<T> {
	
	public enum EventDataPolicy {
		/**
		 * This constant indicates that when the event system leaves the caching
		 * mode, the listener should only be notified of the last cached event.
		 */
		SINGLE,
		
		/**
		 * This constant indicates that when the event system leaves the caching
		 * mode, the listener should not receive multiple copies of the same
		 * cached event data object.
		 */
		UNIQUE,
		
		/**
		 * This constant indicates that when the event system leaves the caching
		 * mode, the listener should receive every cached event data object,
		 * even if there are duplicates.
		 */
		DUPLICATE,
	}
	
	/**
	 * @see Event#add(Listener)
	 */
	public static abstract class Listener<T> {
		@Nullable private List<T> cachedEventData;
		private final EventDataPolicy eventDataPolicy;
		
		public Listener() {
			this.eventDataPolicy = EventDataPolicy.UNIQUE;
		}
		public Listener(@NotNull EventDataPolicy eventDataPolicy) {
			this.eventDataPolicy = Util.checkNotNull(eventDataPolicy);
		}
		
		/**
		 * This method is called when an event occurred on the event object this
		 * listener was listening to. The <tt>eventData</tt> parameter is the
		 * transmitted event data.
		 */
		public abstract void update(T eventData);
		
		private void updateFromCache() {
			if (cachedEventData == null) return;
			for (T eventData : cachedEventData)
				update(eventData);
			cachedEventData = null;
		}
	}
	
	private boolean enabled = true;
	
	/*
	 * We'll use a concurrent array list here to allow listeners to unregister
	 * themselves immedidately upon receiving an event notification.
	 */
	private final List<Listener<T>> listeners = new CopyOnWriteArrayList<Listener<T>> ();
	
	/**
	 * Adds the given listener to the list of listeners who will be notified
	 * when this event is fired.
	 */
	public void add(Listener<T> listener) {
		listeners.add(listener);
	}
	
	/**
	 * Adds all the given listeners to the list of listeners who will be
	 * notified when this event is fired.
	 */
	public void addAll(Collection<Listener<T>> listeners) {
		this.listeners.addAll(listeners);
	}
	
	/**
	 * Removes the given listener from the list of listeners who will be
	 * notified when this event is fired.
	 */
	public void remove(Listener<T> listener) {
		listeners.remove(listener);
	}
	
	/**
	 * Detaches all listeners from this event.
	 */
	public void removeAllListeners() {
		listeners.clear();
	}
	
	/**
	 * Returns an unmodifiable list of all listeners of this event.
	 */
	@Immutable
	@NotNull
	public List<Listener<T>> getListeners() {
		return Collections.unmodifiableList(listeners);
	}
	
	/**
	 * Returns the number of listeners of this event.
	 */
	public int getListenerCount() {
		return listeners.size();
	}
	
	/**
	 * Sents an event with the given event data to the registered listeners,
	 * with the following behavior:
	 * <ul>
	 * <li>Does nothing if the event system has been disabled with
	 * {@link #setEnabled(boolean)}.</li>
	 * <li>The event will be cached if the event system currently operates in
	 * caching mode, via {@link #hold()}.</li>
	 * </ul>
	 * This method should only be called by the observed object.
	 */
	public void fire(T eventData) {
		if (! enabled) return;
		doFireUpdate(eventData);
	}
	
	private void doFireUpdate(T eventData) {
		if (listeners.isEmpty()) return;
		if (hold == 0) {
			for (Listener<T> listener : listeners)
				listener.update(eventData);
		}
		else {
			for (Listener<T> listener : listeners) {
				if (cachedListeners == null)
					cachedListeners = new LinkedHashSet<Listener<?>> ();
				cachedListeners.add(listener);
				
				if (listener.cachedEventData == null)
					listener.cachedEventData = new ArrayList<T> ();
				
				List<T> cache = listener.cachedEventData;
				switch (listener.eventDataPolicy) {
				case SINGLE:
					assert cache.size() <= 1;
					if (cache.isEmpty())
						cache.add(eventData);
					else
						cache.set(0, eventData);
					break;
				case UNIQUE:
					if (! cache.contains(eventData))
						cache.add(eventData);
					break;
				case DUPLICATE:
					cache.add(eventData);
					break;
				}
			}
		}
	}
	
	/**
	 * Returns whether the event system is enabled or not.
	 * 
	 * @see #setEnabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Enables or disables the entire event system. If the system is in caching
	 * mode, cached events will not be discarded.
	 * 
	 * @see #isEnabled
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	private static int hold = 0;
	@Nullable private static Set<Listener<?>> cachedListeners;
	
	/**
	 * Temporarily puts the entire event system into a 'caching mode', meaning
	 * that subsequent notification requests caused by changes on the observed
	 * objects will be delayed until {@link #flush()} is called. Each invocation
	 * of this method must be followed by an invocation of <tt>flush</tt> at a
	 * later point.
	 * <p>
	 * In caching mode, the event system may discard duplicate events, depending
	 * on the return value of {@link Listener#getEventDataPolicy()}.
	 * <p>
	 * Calls to <tt>hold</tt> and <tt>flush</tt> can be nested, so you could,
	 * for example, call <tt>hold</tt> three times, and then <tt>flush</tt>
	 * three times.
	 */
	public static void hold() {
		hold++;
	}
	
	/**
	 * @see #hold
	 */
	public static void flush() {
		hold = Math.max(0, hold - 1);
		if (hold > 0 || cachedListeners == null) return;
		for (Listener<?> listener : cachedListeners)
			listener.updateFromCache();
		cachedListeners = null;
	}
	
	/**
	 * Helper method for redirecting events from one event object to another,
	 * i.e. {@code destination} will be fired when {@code source} is fired.
	 */
	public static <S> void redirect(@NotNull Event<S> source,
									@NotNull final Event<S> destination) {
		source.add(new Listener<S> () {
			public void update(S eventData) {
				destination.fire(eventData);
			}
		});
	}
	
}
